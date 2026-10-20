package com.tvremote.universal.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object LocalNetwork {

    fun isOnLocalNetwork(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /** This phone's IPv4 address on the active (Wi-Fi) network. */
    fun ownIpv4(context: Context): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val lp = cm.getLinkProperties(cm.activeNetwork) ?: return null
        return lp.linkAddresses.map { it.address }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }

    fun <T> withMulticastLock(context: Context, block: () -> T): T {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val lock = wifi?.createMulticastLock("tvremote-discovery")?.apply {
            setReferenceCounted(false)
            acquire()
        }
        try {
            return block()
        } finally {
            runCatching { lock?.release() }
        }
    }

    fun acquireMulticastLock(context: Context): WifiManager.MulticastLock? {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        return wifi?.createMulticastLock("tvremote-discovery")?.apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    /** Directed broadcast addresses of the active network, plus the limited broadcast address. */
    fun broadcastAddresses(context: Context): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val lp = cm?.getLinkProperties(cm.activeNetwork)
        lp?.linkAddresses?.forEach { la ->
            val addr = la.address
            if (addr is Inet4Address) {
                val ip = addr.address.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }
                val prefix = la.prefixLength
                val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
                val bcast = ip or mask.inv()
                val bytes = byteArrayOf((bcast ushr 24).toByte(), (bcast ushr 16).toByte(), (bcast ushr 8).toByte(), bcast.toByte())
                runCatching { result += InetAddress.getByAddress(bytes) }
            }
        }
        result += InetAddress.getByName("255.255.255.255")
        return result.distinct()
    }

    suspend fun isPortOpen(host: String, port: Int, timeoutMs: Int = 1500): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
            true
        }.getOrDefault(false)
    }

    fun isValidIpv4(ip: String): Boolean {
        val parts = ip.trim().split(".")
        return parts.size == 4 && parts.all { p -> p.toIntOrNull()?.let { it in 0..255 } == true && p.isNotEmpty() }
    }
}

object WakeOnLan {

    fun normalizeMac(input: String): String? {
        val hex = input.filter { it.isLetterOrDigit() }.uppercase()
        if (hex.length != 12 || hex.any { it !in "0123456789ABCDEF" }) return null
        return hex.chunked(2).joinToString(":")
    }

    /** Sends the magic packet several times to every broadcast address, ports 9 and 7. */
    suspend fun send(context: Context, mac: String, host: String? = null): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizeMac(mac) ?: return@withContext false
        val macBytes = normalized.split(":").map { it.toInt(16).toByte() }.toByteArray()
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 0 until 16) System.arraycopy(macBytes, 0, packet, 6 + i * 6, 6)
        val targets = LocalNetwork.broadcastAddresses(context).toMutableList()
        host?.let { h -> runCatching { targets += InetAddress.getByName(h) } }
        var sent = false
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                repeat(5) {
                    for (address in targets) {
                        for (port in intArrayOf(9, 7, 40000)) {
                            runCatching {
                                socket.send(DatagramPacket(packet, packet.size, address, port))
                                sent = true
                            }
                        }
                    }
                    delay(90)
                }
            }
        }
        sent
    }
}
