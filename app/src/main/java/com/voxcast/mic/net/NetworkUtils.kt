package com.voxcast.mic.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

enum class LinkKind { WIFI, HOTSPOT, WIFI_DIRECT }

data class LocalLink(val ip: String, val iface: String, val kind: LinkKind)

data class NetworkStatus(
    val wifiConnected: Boolean,
    val hotspotOn: Boolean,
    val wifiDirectOn: Boolean,
    val links: List<LocalLink>,
) {
    val ready: Boolean get() = links.isNotEmpty()
    val headline: String
        get() = when {
            wifiDirectOn -> "Wi-Fi Direct group active"
            hotspotOn && wifiConnected -> "Wi-Fi + Hotspot active"
            hotspotOn -> "Hotspot active"
            wifiConnected -> "Connected to Wi-Fi"
            else -> "No local network"
        }
}

object NetworkUtils {

    private val skipPrefixes = listOf("rmnet", "ccmni", "pdp", "v4-", "dummy", "tun", "ppp", "lo", "radio", "bt-pan")
    private val hotspotNames = Regex("^(ap|swlan|softap|wlan_ap)\\d*$")

    fun localLinks(): List<LocalLink> {
        val result = mutableListOf<LocalLink>()
        val interfaces = runCatching { NetworkInterface.getNetworkInterfaces()?.toList() }.getOrNull().orEmpty()
        for (nif in interfaces) {
            val up = runCatching { nif.isUp && !nif.isLoopback }.getOrDefault(false)
            if (!up) continue
            val name = nif.name.lowercase()
            if (skipPrefixes.any { name.startsWith(it) }) continue
            for (addr in nif.inetAddresses.toList()) {
                if (addr !is Inet4Address || addr.isLoopbackAddress) continue
                if (!addr.isSiteLocalAddress) continue
                val ip = addr.hostAddress ?: continue
                val kind = when {
                    name.startsWith("p2p") -> LinkKind.WIFI_DIRECT
                    hotspotNames.matches(name) -> LinkKind.HOTSPOT
                    // A secondary wlan interface acting as gateway (.1) is the tethering AP.
                    name.startsWith("wlan") && name != "wlan0" && ip.endsWith(".1") -> LinkKind.HOTSPOT
                    else -> LinkKind.WIFI
                }
                result += LocalLink(ip, nif.name, kind)
            }
        }
        // Wi-Fi Direct first (fixed group-owner address), then hotspot, then Wi-Fi.
        return result.sortedByDescending { it.kind.ordinal }
    }

    fun status(context: Context): NetworkStatus {
        val links = localLinks()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("DEPRECATION")
        val wifi = runCatching {
            cm.allNetworks.any { n ->
                cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        }.getOrDefault(false) || links.any { it.kind == LinkKind.WIFI }
        return NetworkStatus(
            wifiConnected = wifi,
            hotspotOn = links.any { it.kind == LinkKind.HOTSPOT },
            wifiDirectOn = links.any { it.kind == LinkKind.WIFI_DIRECT },
            links = links,
        )
    }

    const val WIFI_DIRECT_OWNER_IP = "192.168.49.1"
}
