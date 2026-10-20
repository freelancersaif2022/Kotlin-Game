package com.tvremote.universal.tv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.tvremote.universal.core.LocalNetwork
import com.tvremote.universal.data.DiscoveredTv
import com.tvremote.universal.data.TvPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Finds TVs on the local network:
 *  - mDNS: Android TV / Google TV (_androidtvremote2._tcp) and Fire TV (_amzn-wplay._tcp)
 *  - SSDP: Samsung, LG, Roku, Vizio (and Fire TV via DIAL)
 */
class TvDiscovery(private val context: Context, private val http: OkHttpClient) {

    private val resolveLock = Mutex()

    fun discover(durationMs: Long = 9000): Flow<DiscoveredTv> = channelFlow {
        val lock = LocalNetwork.acquireMulticastLock(context)
        val seen = HashMap<String, String>()
        val seenLock = Mutex()
        // Emits a TV once, or again when a better (non-generic) name arrives later.
        val emit: suspend (DiscoveredTv) -> Unit = { tv ->
            val key = "${tv.platform}|${tv.host}"
            val fresh = seenLock.withLock {
                val prev = seen[key]
                if (prev == null || (prev in GENERIC_NAMES && tv.name !in GENERIC_NAMES)) {
                    seen[key] = tv.name
                    true
                } else {
                    false
                }
            }
            if (fresh) send(tv)
        }
        try {
            withTimeoutOrNull(durationMs) {
                coroutineScope {
                    launch { nsd("_androidtvremote2._tcp", TvPlatform.ANDROID_TV, emit) }
                    launch { nsd("_amzn-wplay._tcp", TvPlatform.FIRE_TV, emit) }
                    launch { ssdp(emit) }
                    launch {
                        delay(700)
                        sweep(emit)
                    }
                }
            }
        } finally {
            runCatching { lock?.release() }
        }
    }

    // ---------------------------------------------------------------- mDNS

    private suspend fun ProducerScope<DiscoveredTv>.nsd(
        type: String,
        platform: TvPlatform,
        emit: suspend (DiscoveredTv) -> Unit,
    ) {
        val nsd = context.getSystemService(NsdManager::class.java) ?: return
        val found = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) { found.trySend(serviceInfo) }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { found.close() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener) }.onFailure { return }
        try {
            for (info in found) {
                val resolved = resolve(nsd, info) ?: continue
                @Suppress("DEPRECATION")
                val address = resolved.host
                if (address !is Inet4Address) continue
                val host = address.hostAddress ?: continue
                val name = when (platform) {
                    TvPlatform.FIRE_TV -> resolved.attributes["n"]?.toString(Charsets.UTF_8)
                        ?.takeIf { it.isNotBlank() } ?: "Fire TV"
                    else -> resolved.serviceName
                }
                emit(DiscoveredTv(platform, host, name))
            }
        } finally {
            runCatching { nsd.stopServiceDiscovery(listener) }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolve(nsd: NsdManager, info: NsdServiceInfo): NsdServiceInfo? = resolveLock.withLock {
        withTimeoutOrNull(4000) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    nsd.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            if (cont.isActive) cont.resume(null)
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            if (cont.isActive) cont.resume(serviceInfo)
                        }
                    })
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
    }

    // ---------------------------------------------------------------- SSDP

    private suspend fun ProducerScope<DiscoveredTv>.ssdp(emit: suspend (DiscoveredTv) -> Unit) = withContext(Dispatchers.IO) {
        val targets = listOf(
            "roku:ecp",
            "urn:samsung.com:device:RemoteControlReceiver:1",
            "urn:lge-com:service:webos-second-screen:1",
            "urn:dial-multiscreen-org:service:dial:1",
            "urn:schemas-upnp-org:device:MediaRenderer:1",
        )
        val group = InetAddress.getByName("239.255.255.250")
        val handled = ConcurrentHashMap<String, Boolean>()
        DatagramSocket().use { socket ->
            socket.soTimeout = 400
            fun search() {
                for (st in targets) {
                    val msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $st\r\n\r\n"
                    val bytes = msg.toByteArray()
                    runCatching { socket.send(DatagramPacket(bytes, bytes.size, group, 1900)) }
                }
            }
            search()
            var lastSearch = System.currentTimeMillis()
            var searches = 1
            val buf = ByteArray(4096)
            while (isActive) {
                if (searches < 3 && System.currentTimeMillis() - lastSearch > 2500) {
                    search()
                    searches++
                    lastSearch = System.currentTimeMillis()
                }
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    continue
                } catch (e: Exception) {
                    break
                }
                val host = packet.address?.hostAddress ?: continue
                if (packet.address !is Inet4Address) continue
                val headers = parseHeaders(String(packet.data, 0, packet.length))
                if (handled.putIfAbsent(host, true) != null) continue
                launch {
                    runCatching { classify(host, headers) }.getOrNull()?.let { emit(it) } ?: handled.remove(host)
                }
            }
        }
    }

    private fun parseHeaders(response: String): Map<String, String> =
        response.split("\r\n").mapNotNull { line ->
            val idx = line.indexOf(':')
            if (idx <= 0) null else line.substring(0, idx).trim().uppercase() to line.substring(idx + 1).trim()
        }.toMap()

    private suspend fun classify(host: String, h: Map<String, String>): DiscoveredTv? {
        val st = h["ST"].orEmpty()
        val server = h["SERVER"].orEmpty()
        val location = h["LOCATION"].orEmpty()
        return when {
            st == "roku:ecp" || server.contains("roku", true) -> roku(host)
            st.contains("samsung", true) || server.contains("samsung", true) || server.contains("tizen", true) -> samsung(host)
            st.contains("lge-com", true) || server.contains("webos", true) -> lg(host, location)
            else -> fromDescription(host, location)
        }
    }

    private suspend fun roku(host: String): DiscoveredTv? {
        val info = runCatching { RokuApi.info(http, host) }.getOrNull()
        return DiscoveredTv(TvPlatform.ROKU, host, info?.name ?: "Roku", info?.model.orEmpty(), info?.mac.orEmpty())
    }

    private suspend fun samsung(host: String): DiscoveredTv? {
        val info = SamsungApi.info(http, host) ?: return null
        return DiscoveredTv(TvPlatform.SAMSUNG, host, info.name, info.model, info.mac)
    }

    private suspend fun lg(host: String, location: String): DiscoveredTv {
        val desc = description(location)
        val name = desc?.let { Xml.tag(it, "friendlyName") }.orEmpty().ifBlank { "LG TV" }
        val model = desc?.let { Xml.tag(it, "modelName") }.orEmpty()
        return DiscoveredTv(TvPlatform.LG, host, name, model)
    }

    private suspend fun fromDescription(host: String, location: String): DiscoveredTv? {
        val desc = description(location) ?: return null
        val manufacturer = Xml.tag(desc, "manufacturer")
        val name = Xml.tag(desc, "friendlyName")
        val model = Xml.tag(desc, "modelName")
        return when {
            manufacturer.contains("vizio", true) -> DiscoveredTv(TvPlatform.VIZIO, host, name.ifBlank { "Vizio TV" }, model)
            manufacturer.contains("samsung", true) -> samsung(host)
            manufacturer.contains("lg electronics", true) -> DiscoveredTv(TvPlatform.LG, host, name.ifBlank { "LG TV" }, model)
            manufacturer.contains("amazon", true) && !model.contains("echo", true) ->
                DiscoveredTv(TvPlatform.FIRE_TV, host, name.ifBlank { "Fire TV" }, model)
            else -> null
        }
    }

    private suspend fun description(location: String): String? = withContext(Dispatchers.IO) {
        if (location.isBlank()) return@withContext null
        runCatching {
            http.newCall(Request.Builder().url(location).build()).execute().use { it.body?.string() }
        }.getOrNull()
    }

    // ---------------------------------------------------------------- subnet sweep

    /**
     * Probes every address of the local /24 on the TV control ports. Finds TVs even when the
     * router blocks multicast (mDNS / SSDP).
     */
    private suspend fun sweep(emit: suspend (DiscoveredTv) -> Unit) = withContext(Dispatchers.IO) {
        val own = LocalNetwork.ownIpv4(context) ?: return@withContext
        val base = own.substringBeforeLast('.')
        val gate = Semaphore(40)
        coroutineScope {
            for (i in 1..254) {
                val host = "$base.$i"
                if (host == own) continue
                launch { gate.withPermit { probeHost(host)?.let { emit(it) } } }
            }
        }
    }

    private suspend fun probeHost(host: String): DiscoveredTv? {
        if (LocalNetwork.isPortOpen(host, 8060, 280)) return roku(host)
        if (LocalNetwork.isPortOpen(host, AndroidTvProtocol.PAIRING_PORT, 280)) return DiscoveredTv(TvPlatform.ANDROID_TV, host, GENERIC_ANDROID)
        if (LocalNetwork.isPortOpen(host, 8001, 280)) return samsung(host)
        if (LocalNetwork.isPortOpen(host, 7345, 280)) return DiscoveredTv(TvPlatform.VIZIO, host, "Vizio TV")
        return null
    }

    companion object {
        const val GENERIC_ANDROID = "Android TV"
        private val GENERIC_NAMES = setOf(GENERIC_ANDROID, "Fire TV", "Roku", "LG TV", "Vizio TV", "Samsung TV")

        /** Quick check used after manual entry: is anything listening where this platform should be? */
        suspend fun probe(platform: TvPlatform, host: String): Boolean = when (platform) {
            TvPlatform.ANDROID_TV -> LocalNetwork.isPortOpen(host, AndroidTvProtocol.PAIRING_PORT)
            TvPlatform.SAMSUNG -> LocalNetwork.isPortOpen(host, 8001)
            TvPlatform.LG -> LocalNetwork.isPortOpen(host, 3000) || LocalNetwork.isPortOpen(host, 3001)
            TvPlatform.ROKU -> LocalNetwork.isPortOpen(host, 8060)
            TvPlatform.VIZIO -> LocalNetwork.isPortOpen(host, 7345) || LocalNetwork.isPortOpen(host, 9000)
            TvPlatform.FIRE_TV -> LocalNetwork.isPortOpen(host, 5555)
        }
    }
}
