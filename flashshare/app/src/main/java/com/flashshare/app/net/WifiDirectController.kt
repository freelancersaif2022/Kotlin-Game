package com.flashshare.app.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import com.flashshare.app.data.BandPref
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import kotlin.coroutines.resume

class WifiP2pException(message: String) : Exception(message)

/** Result of hosting a Wi-Fi Direct group. */
data class HostedGroup(val ssid: String, val passphrase: String, val frequency: Int, val ownerIp: String) {
    val bandLabel: String get() = bandLabel(frequency)
}

fun bandLabel(freq: Int): String = when {
    freq >= 5925 -> "6 GHz"
    freq >= 4900 -> "5 GHz"
    freq in 2400..2500 -> "2.4 GHz"
    else -> "Wi-Fi Direct"
}

/**
 * Creates / joins Wi-Fi Direct groups.
 *
 * Speed strategy: the receiver hosts an *autonomous* group with a fixed SSID + passphrase pinned to the
 * 5 GHz (or 6 GHz) band. The sender joins with the exact frequency from the QR code, so there is no slow
 * peer discovery and no system "accept connection" dialog. Older or picky phones fall back to auto band,
 * then to a normal Wi-Fi join (WifiNetworkSpecifier / legacy WifiConfiguration).
 */
@SuppressLint("MissingPermission", "NewApi", "InlinedApi")
class WifiDirectController(private val context: Context) {

    private val manager: WifiP2pManager? = context.getSystemService(WifiP2pManager::class.java)
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
    private val cm: ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private var channel: WifiP2pManager.Channel? = null
    private var specifierCallback: ConnectivityManager.NetworkCallback? = null
    private var legacyNetId: Int = -1
    private val random = SecureRandom()

    val isSupported: Boolean get() = manager != null
    val isWifiEnabled: Boolean get() = wifiManager.isWifiEnabled
    val supports5Ghz: Boolean get() = wifiManager.is5GHzBandSupported
    val supports6Ghz: Boolean get() = Build.VERSION.SDK_INT >= 30 && wifiManager.is6GHzBandSupported

    private fun ch(): WifiP2pManager.Channel {
        val m = manager ?: throw WifiP2pException("This phone doesn't support Wi-Fi Direct")
        return channel ?: m.initialize(context, Looper.getMainLooper()) { channel = null }.also { channel = it }
    }

    // region suspend wrappers
    private suspend fun action(block: (WifiP2pManager.ActionListener) -> Unit): Int =
        suspendCancellableCoroutine { c ->
            block(object : WifiP2pManager.ActionListener {
                override fun onSuccess() { if (c.isActive) c.resume(OK) }
                override fun onFailure(reason: Int) { if (c.isActive) c.resume(reason) }
            })
        }

    private suspend fun groupInfo(): WifiP2pGroup? = suspendCancellableCoroutine { c ->
        manager!!.requestGroupInfo(ch()) { g -> if (c.isActive) c.resume(g) }
    }

    private suspend fun connectionInfo(): WifiP2pInfo? = suspendCancellableCoroutine { c ->
        manager!!.requestConnectionInfo(ch()) { i -> if (c.isActive) c.resume(i) }
    }
    // endregion

    /** Band constants in the order we try them for the given preference. */
    private fun bandOrder(pref: BandPref): List<Int> {
        val list = mutableListOf<Int>()
        if (pref == BandPref.GHZ6 && supports6Ghz) band6Ghz()?.let { list += it }
        if (pref != BandPref.AUTO && supports5Ghz) list += WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
        if (pref == BandPref.AUTO && supports5Ghz) list += WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
        list += WifiP2pConfig.GROUP_OWNER_BAND_AUTO
        return list.distinct()
    }

    /** GROUP_OWNER_BAND_6GHZ only exists on newer platforms – look it up reflectively so we compile against SDK 35. */
    private fun band6Ghz(): Int? = runCatching {
        WifiP2pConfig::class.java.getField("GROUP_OWNER_BAND_6GHZ").getInt(null)
    }.getOrNull()

    suspend fun createGroup(pref: BandPref): HostedGroup {
        ch()
        if (!isWifiEnabled) throw WifiP2pException("Turn on Wi-Fi to start a fast connection")
        reset()
        delay(250)

        var created = false
        var lastReason = OK
        if (Build.VERSION.SDK_INT >= 29) {
            val ssid = "DIRECT-" + randomString(2) + "-FlashShare"
            val pass = randomString(12)
            // If this phone is also on a 5/6 GHz router, host on the same channel: the radio then never has to
            // time-share between two channels (multi-channel concurrency roughly halves throughput).
            val sta = staFrequency()
            val attempts = buildList<Pair<Int, Int>> {
                if (sta >= 4900 && pref != BandPref.AUTO) add(sta to -1)
                bandOrder(pref).forEach { add(0 to it) }
            }
            for ((freq, band) in attempts) {
                val cfg = runCatching {
                    val b = WifiP2pConfig.Builder()
                        .setNetworkName(ssid)
                        .setPassphrase(pass)
                        .enablePersistentMode(false)
                    if (freq > 0) b.setGroupOperatingFrequency(freq) else b.setGroupOperatingBand(band)
                    b.build()
                }.getOrNull() ?: continue
                lastReason = action { manager!!.createGroup(ch(), cfg, it) }
                Log.d(TAG, "createGroup freq=$freq band=$band -> $lastReason")
                if (lastReason == OK) { created = true; break }
                action { manager!!.removeGroup(ch(), it) }
                delay(400)
            }
        }
        if (!created) {
            lastReason = action { manager!!.createGroup(ch(), it) }
            created = lastReason == OK
        }
        if (!created) throw WifiP2pException(reasonText(lastReason))

        // Wait until the group is actually up and has credentials.
        val g = withTimeoutOrNull(12_000) {
            while (true) {
                val g = groupInfo()
                if (g != null && g.isGroupOwner && !g.networkName.isNullOrEmpty() && !g.passphrase.isNullOrEmpty()) return@withTimeoutOrNull g
                delay(200)
            }
            @Suppress("UNREACHABLE_CODE") null
        } ?: throw WifiP2pException("Wi-Fi Direct didn't start. Turn off Hotspot and try again.")

        val freq = if (Build.VERSION.SDK_INT >= 29) g.frequency else 0
        val ip = p2pInterfaceIp(g.`interface`) ?: "192.168.49.1"
        return HostedGroup(g.networkName, g.passphrase, freq, ip)
    }

    /** Joins a group from a scanned QR code and returns the group owner's IP. */
    suspend fun joinGroup(qr: QrPayload): String {
        ch()
        if (!isWifiEnabled) throw WifiP2pException("Turn on Wi-Fi to connect")
        reset()
        delay(200)

        if (Build.VERSION.SDK_INT >= 29) {
            val attempts = buildList {
                if (qr.freq > 0) add(qr.freq to -1)
                add(0 to bandForFreq(qr.freq))
                add(0 to WifiP2pConfig.GROUP_OWNER_BAND_AUTO)
            }.distinct()
            for ((freq, band) in attempts) {
                val cfg = runCatching {
                    val b = WifiP2pConfig.Builder().setNetworkName(qr.ssid).setPassphrase(qr.pass)
                    if (freq > 0) b.setGroupOperatingFrequency(freq) else b.setGroupOperatingBand(band)
                    b.build()
                }.getOrNull() ?: continue
                val r = action { manager!!.connect(ch(), cfg, it) }
                Log.d(TAG, "connect freq=$freq band=$band -> $r")
                if (r != OK) { delay(500); continue }
                val ip = waitForClientConnection(20_000)
                if (ip != null) return ip
                action { manager!!.cancelConnect(ch(), it) }
                action { manager!!.removeGroup(ch(), it) }
                delay(400)
            }
            // Last resort: normal Wi-Fi join to the group's soft-AP.
            return joinWithSpecifier(qr)
        }
        return joinLegacy(qr)
    }

    private fun bandForFreq(freq: Int): Int = when {
        freq >= 5925 -> band6Ghz() ?: WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
        freq >= 4900 -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
        freq in 2400..2500 -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
        else -> WifiP2pConfig.GROUP_OWNER_BAND_AUTO
    }

    private suspend fun waitForClientConnection(timeoutMs: Long): String? = withTimeoutOrNull(timeoutMs) {
        while (true) {
            val info = connectionInfo()
            if (info != null && info.groupFormed && !info.isGroupOwner && info.groupOwnerAddress != null) {
                return@withTimeoutOrNull info.groupOwnerAddress.hostAddress
            }
            delay(250)
        }
        @Suppress("UNREACHABLE_CODE") null
    }

    /** Frequency of the normal Wi-Fi (router) connection, or 0 when not connected. */
    fun staFrequency(): Int = runCatching {
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo
        if (info != null && info.networkId != -1 && info.frequency > 0) info.frequency else 0
    }.getOrDefault(0)

    /** Frequency of the group we are currently a member of (for the band badge on the client side). */
    suspend fun currentFrequency(): Int {
        if (Build.VERSION.SDK_INT < 29 || manager == null) return 0
        return runCatching { groupInfo()?.frequency ?: 0 }.getOrDefault(0)
    }

    @SuppressLint("NewApi")
    private suspend fun joinWithSpecifier(qr: QrPayload): String {
        val spec = WifiNetworkSpecifier.Builder().setSsid(qr.ssid).setWpa2Passphrase(qr.pass).build()
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(spec)
            .build()
        val network = withTimeoutOrNull(45_000) {
            suspendCancellableCoroutine<Network?> { c ->
                val cb = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) { if (c.isActive) c.resume(network) }
                    override fun onUnavailable() { if (c.isActive) c.resume(null) }
                }
                specifierCallback = cb
                cm.requestNetwork(req, cb)
            }
        } ?: throw WifiP2pException("Couldn't connect to the receiver. Move the phones closer and try again.")
        cm.bindProcessToNetwork(network)
        return qr.host
    }

    @Suppress("DEPRECATION")
    private suspend fun joinLegacy(qr: QrPayload): String {
        val conf = WifiConfiguration().apply {
            SSID = "\"${qr.ssid}\""
            preSharedKey = "\"${qr.pass}\""
        }
        val id = wifiManager.addNetwork(conf)
        if (id < 0) throw WifiP2pException("Couldn't join the receiver's network")
        legacyNetId = id
        wifiManager.disconnect()
        wifiManager.enableNetwork(id, true)
        wifiManager.reconnect()
        val ok = withTimeoutOrNull(30_000) {
            while (true) {
                val info = wifiManager.connectionInfo
                if (info != null && info.ssid == "\"${qr.ssid}\"" && info.ipAddress != 0) return@withTimeoutOrNull true
                delay(300)
            }
            @Suppress("UNREACHABLE_CODE") false
        } ?: false
        if (!ok) throw WifiP2pException("Couldn't connect to the receiver. Move the phones closer and try again.")
        cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
            ?.let { cm.bindProcessToNetwork(it) }
        val gw = wifiManager.dhcpInfo?.gateway ?: 0
        return if (gw != 0) intToIp(gw) else qr.host
    }

    /** Tears down any group / binding we created. Safe to call repeatedly. */
    suspend fun reset() {
        runCatching { cm.bindProcessToNetwork(null) }
        specifierCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        specifierCallback = null
        if (legacyNetId >= 0) {
            @Suppress("DEPRECATION")
            runCatching { wifiManager.removeNetwork(legacyNetId); wifiManager.reconnect() }
            legacyNetId = -1
        }
        if (manager != null && channel != null) {
            action { manager.cancelConnect(ch(), it) }
            action { manager.removeGroup(ch(), it) }
        }
    }

    private fun p2pInterfaceIp(iface: String?): String? = runCatching {
        val ifs = NetworkInterface.getNetworkInterfaces().toList()
        val target = ifs.firstOrNull { it.name == iface } ?: ifs.firstOrNull { it.name.startsWith("p2p") }
        target?.inetAddresses?.toList()?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }.getOrNull()

    private fun randomString(n: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        return (1..n).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    private fun intToIp(i: Int) = "${i and 0xff}.${i shr 8 and 0xff}.${i shr 16 and 0xff}.${i shr 24 and 0xff}"

    private fun reasonText(r: Int) = when (r) {
        WifiP2pManager.P2P_UNSUPPORTED -> "This phone doesn't support Wi-Fi Direct"
        WifiP2pManager.BUSY -> "Wi-Fi is busy. Turn off Mobile Hotspot / other sharing apps and retry."
        else -> "Couldn't start Wi-Fi Direct (code $r). Toggle Wi-Fi off and on, then retry."
    }

    companion object {
        private const val TAG = "WifiDirect"
        private const val OK = -1
    }
}
