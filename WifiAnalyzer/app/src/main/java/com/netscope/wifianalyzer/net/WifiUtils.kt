package com.netscope.wifianalyzer.net

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.netscope.wifianalyzer.data.SpeedUnit
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.Locale
import kotlin.math.roundToInt

data class WifiSnapshot(
    val wifiConnected: Boolean,
    val connectionType: String,
    val hasInternet: Boolean,
    val ssid: String?,
    val bssid: String?,
    val rssi: Int,
    val linkSpeed: Int,
    val txSpeed: Int,
    val rxSpeed: Int,
    val frequency: Int,
    val channel: Int,
    val band: String,
    val standard: String?,
    val security: String?,
    val ip: String?,
    val prefix: Int,
    val gateway: String?,
    val dns: List<String>,
    val ipv6: String?,
    val networkId: Int,
) {
    val hasSignal: Boolean get() = wifiConnected && rssi in -126..0
    val signalPercent: Int get() = if (hasSignal) WifiUtils.rssiToPercent(rssi) else 0
    val subnetMask: String get() = WifiUtils.prefixToMask(prefix)

    /** Stable id for "this network" used to group known devices and heatmaps. */
    val networkKey: String? get() = gateway?.let { "$it/$prefix" }

    val displayName: String
        get() = when {
            !wifiConnected -> "Not connected"
            ssid != null -> ssid
            else -> "Wi-Fi network"
        }

    companion object {
        val EMPTY = WifiSnapshot(
            wifiConnected = false, connectionType = "Offline", hasInternet = false, ssid = null, bssid = null,
            rssi = -127, linkSpeed = 0, txSpeed = 0, rxSpeed = 0, frequency = 0, channel = 0, band = "",
            standard = null, security = null, ip = null, prefix = 24, gateway = null, dns = emptyList(),
            ipv6 = null, networkId = -1,
        )
    }
}

object WifiUtils {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    fun isWifiEnabled(context: Context): Boolean =
        context.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled == true

    private fun wifiNetwork(cm: ConnectivityManager): Network? {
        val active = cm.activeNetwork
        if (active != null && cm.getNetworkCapabilities(active)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            return active
        }
        @Suppress("DEPRECATION")
        return cm.allNetworks.firstOrNull {
            cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    @Suppress("DEPRECATION")
    fun snapshot(context: Context): WifiSnapshot {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return WifiSnapshot.EMPTY
        val wm = context.applicationContext.getSystemService(WifiManager::class.java)
        val activeCaps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val connectionType = when {
            activeCaps == null -> "Offline"
            activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
        val hasInternet = activeCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val wifiNet = wifiNetwork(cm)
        if (wifiNet == null || wm == null) {
            return WifiSnapshot.EMPTY.copy(connectionType = connectionType, hasInternet = hasInternet)
        }
        val info: WifiInfo? = runCatching { wm.connectionInfo }.getOrNull()
        val lp: LinkProperties? = cm.getLinkProperties(wifiNet)

        val v4 = lp?.linkAddresses?.firstOrNull { it.address is Inet4Address }
        val v6 = lp?.linkAddresses?.firstOrNull { it.address is Inet6Address && !it.address.isLinkLocalAddress }
        val gateway = lp?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress
            ?: wm.dhcpInfo?.gateway?.takeIf { it != 0 }?.let { intToIp(it) }

        val freq = info?.frequency ?: 0
        val ssid = info?.ssid?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        val bssid = info?.bssid?.takeIf { it != "02:00:00:00:00:00" }

        val security = if (Build.VERSION.SDK_INT >= 31 && info != null) {
            securityName(info.currentSecurityType)
        } else {
            null
        } ?: bssid?.let { b -> runCatching { wm.scanResults.firstOrNull { it.BSSID == b }?.capabilities }.getOrNull() }
            ?.let { securityFromCapabilities(it) }

        val standard = if (Build.VERSION.SDK_INT >= 30 && info != null) standardName(info.wifiStandard) else null

        return WifiSnapshot(
            wifiConnected = true,
            connectionType = connectionType,
            hasInternet = hasInternet,
            ssid = ssid,
            bssid = bssid,
            rssi = info?.rssi ?: -127,
            linkSpeed = info?.linkSpeed ?: 0,
            txSpeed = if (Build.VERSION.SDK_INT >= 29) info?.txLinkSpeedMbps ?: 0 else 0,
            rxSpeed = if (Build.VERSION.SDK_INT >= 29) info?.rxLinkSpeedMbps ?: 0 else 0,
            frequency = freq,
            channel = frequencyToChannel(freq),
            band = bandName(freq),
            standard = standard,
            security = security,
            ip = (v4?.address as? Inet4Address)?.hostAddress ?: info?.ipAddress?.takeIf { it != 0 }?.let { intToIp(it) },
            prefix = v4?.prefixLength ?: 24,
            gateway = gateway,
            dns = lp?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList(),
            ipv6 = v6?.address?.hostAddress?.substringBefore('%'),
            networkId = info?.networkId ?: -1,
        )
    }

    fun intToIp(i: Int): String =
        "${i and 0xFF}.${i shr 8 and 0xFF}.${i shr 16 and 0xFF}.${i shr 24 and 0xFF}"

    fun prefixToMask(prefix: Int): String {
        val p = prefix.coerceIn(0, 32)
        val mask = if (p == 0) 0L else (0xFFFFFFFFL shl (32 - p)) and 0xFFFFFFFFL
        return "${mask shr 24 and 0xFF}.${mask shr 16 and 0xFF}.${mask shr 8 and 0xFF}.${mask and 0xFF}"
    }

    fun frequencyToChannel(freq: Int): Int = when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2407) / 5
        freq in 5160..5885 -> (freq - 5000) / 5
        freq in 5925..7125 -> (freq - 5950) / 5
        else -> 0
    }

    fun bandName(freq: Int): String = when {
        freq in 2400..2500 -> "2.4 GHz"
        freq in 4900..5900 -> "5 GHz"
        freq in 5925..7125 -> "6 GHz"
        else -> ""
    }

    fun rssiToPercent(rssi: Int): Int = ((rssi + 100) * 100 / 65).coerceIn(0, 100)

    fun signalLevel(rssi: Int): Int = when {
        rssi >= -55 -> 4
        rssi >= -67 -> 3
        rssi >= -75 -> 2
        rssi >= -85 -> 1
        else -> 0
    }

    fun qualityLabel(rssi: Int): String = when (signalLevel(rssi)) {
        4 -> "Excellent"
        3 -> "Good"
        2 -> "Fair"
        1 -> "Weak"
        else -> "Very weak"
    }

    fun standardName(std: Int): String? = when (std) {
        1 -> "Legacy (802.11a/b/g)"
        4 -> "Wi-Fi 4 (802.11n)"
        5 -> "Wi-Fi 5 (802.11ac)"
        6 -> "Wi-Fi 6 (802.11ax)"
        7 -> "WiGig (802.11ad)"
        8 -> "Wi-Fi 7 (802.11be)"
        else -> null
    }

    private fun securityName(type: Int): String? = when (type) {
        0 -> "Open (no password)"
        1 -> "WEP (outdated)"
        2 -> "WPA2-Personal"
        3 -> "WPA2-Enterprise"
        4 -> "WPA3-Personal"
        5, 9 -> "WPA3-Enterprise"
        6 -> "Enhanced Open (OWE)"
        7, 8 -> "WAPI"
        11, 12 -> "Passpoint"
        13 -> "Wi-Fi Easy Connect"
        else -> null
    }

    fun securityFromCapabilities(caps: String): String = when {
        caps.contains("SAE") || caps.contains("WPA3") -> "WPA3"
        caps.contains("EAP") -> "WPA2-Enterprise"
        caps.contains("WPA2") || caps.contains("RSN") -> "WPA2"
        caps.contains("WPA") -> "WPA"
        caps.contains("WEP") -> "WEP (outdated)"
        caps.contains("OWE") -> "Enhanced Open"
        else -> "Open"
    }

    /** Returns the value text and unit label for a speed in Mbps. */
    fun formatSpeed(mbps: Double, unit: SpeedUnit): Pair<String, String> {
        val v = if (unit == SpeedUnit.MBYTES) mbps / 8.0 else mbps
        val text = when {
            v <= 0.0 -> "0"
            v < 10 -> String.format(Locale.US, "%.2f", v)
            v < 100 -> String.format(Locale.US, "%.1f", v)
            else -> v.roundToInt().toString()
        }
        return text to unit.label
    }

    fun formatBytes(bytes: Long): String {
        val b = bytes.toDouble()
        return when {
            b >= 1e12 -> String.format(Locale.US, "%.2f TB", b / 1e12)
            b >= 1e9 -> String.format(Locale.US, "%.2f GB", b / 1e9)
            b >= 1e6 -> String.format(Locale.US, "%.1f MB", b / 1e6)
            b >= 1e3 -> String.format(Locale.US, "%.0f KB", b / 1e3)
            else -> "${bytes} B"
        }
    }
}
