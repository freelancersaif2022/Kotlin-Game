package com.netscope.wifianalyzer.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

enum class DeviceType(val label: String) {
    ROUTER("Router"),
    PHONE("Phone"),
    TABLET("Tablet"),
    COMPUTER("Computer"),
    TV("TV & streaming"),
    SPEAKER("Speaker"),
    PRINTER("Printer"),
    CAMERA("Camera"),
    CONSOLE("Game console"),
    SMART_HOME("Smart home"),
    UNKNOWN("Device"),
}

data class NetDevice(
    val ip: String,
    val hostname: String?,
    val mdnsName: String?,
    val mac: String?,
    val vendor: String?,
    val type: DeviceType,
    val openPorts: List<Int>,
    val services: Set<String>,
    val latencyMs: Int?,
    val isGateway: Boolean,
    val isSelf: Boolean,
) {
    /** Stable identity across scans: MAC when Android exposes it, otherwise name, otherwise IP. */
    val key: String
        get() = mac?.lowercase()
            ?: (mdnsName ?: hostname)?.lowercase()?.let { "name:$it" }
            ?: "ip:$ip"

    val displayName: String
        get() = when {
            isSelf -> "This device"
            isGateway -> vendor?.let { "$it router" } ?: "Router"
            mdnsName != null -> mdnsName
            hostname != null -> hostname
            vendor != null -> "$vendor ${type.label.lowercase()}"
            else -> "Unknown device"
        }

    val ipSortKey: Long
        get() = ip.split('.').fold(0L) { acc, s -> acc * 256 + (s.toLongOrNull() ?: 0L) }
}

data class ScanProgress(
    val progress: Float,
    val devices: List<NetDevice>,
    val done: Boolean,
    val error: String? = null,
)

/**
 * Finds devices on the local network using TCP probes (an open or refused port
 * both prove a host exists), reverse DNS, mDNS/Bonjour and the ARP cache where
 * Android still allows reading it.
 */
class NetworkScanner(private val context: Context) {

    fun scan(withMdns: Boolean = true): Flow<ScanProgress> = channelFlow {
        val snap = WifiUtils.snapshot(context)
        val selfIp = snap.ip
        if (!snap.wifiConnected || selfIp == null) {
            send(ScanProgress(0f, emptyList(), true, "Connect to a Wi-Fi network to scan for devices."))
            return@channelFlow
        }
        val hosts = hostsToScan(selfIp, snap.prefix)
        val found = ConcurrentHashMap<String, NetDevice>()
        val mdns = ConcurrentHashMap<String, MdnsHit>()
        val done = AtomicInteger(0)

        found[selfIp] = selfDevice(selfIp, snap.gateway == selfIp)

        val mdnsJob = if (withMdns) launch { discoverMdns(mdns) } else null

        val sem = Semaphore(48)
        coroutineScope {
            hosts.filter { it != selfIp }.map { ip ->
                async(Dispatchers.IO) {
                    sem.withPermit {
                        probe(ip)?.let { (latency, ports) ->
                            found[ip] = buildDevice(ip, latency, ports, ip == snap.gateway, mdns[ip], null)
                        }
                    }
                    val d = done.incrementAndGet()
                    if (d % 8 == 0 || d == hosts.size - 1) {
                        send(ScanProgress(0.85f * d / hosts.size, sorted(found.values), false))
                    }
                }
            }.awaitAll()
        }

        // ARP cache (readable on older Android versions only) catches devices that block every probe.
        val arp = readArp()
        arp.forEach { (ip, mac) ->
            val existing = found[ip]
            if (existing != null) {
                found[ip] = existing.copy(mac = mac, vendor = existing.vendor ?: Vendors.fromMac(mac))
            } else if (hosts.contains(ip)) {
                found[ip] = buildDevice(ip, null, emptyList(), ip == snap.gateway, mdns[ip], mac)
            }
        }
        send(ScanProgress(0.9f, sorted(found.values), false))

        // Reverse DNS names, resolved in parallel with a short timeout.
        coroutineScope {
            found.values.filter { !it.isSelf && it.hostname == null }.map { dev ->
                async(Dispatchers.IO) {
                    val name = withTimeoutOrNull(1_500) { reverseDns(dev.ip) }
                    if (name != null) {
                        found[dev.ip] = buildDevice(dev.ip, dev.latencyMs, dev.openPorts, dev.isGateway, mdns[dev.ip], dev.mac, name)
                    }
                }
            }.awaitAll()
        }

        mdnsJob?.join()
        mdns.forEach { (ip, hit) ->
            val dev = found[ip]
            if (dev != null && !dev.isSelf) {
                found[ip] = buildDevice(ip, dev.latencyMs, dev.openPorts, dev.isGateway, hit, dev.mac, dev.hostname)
            } else if (dev == null && hosts.contains(ip)) {
                found[ip] = buildDevice(ip, null, emptyList(), ip == snap.gateway, hit, null)
            }
        }
        send(ScanProgress(1f, sorted(found.values), true))
    }

    private fun sorted(devices: Collection<NetDevice>): List<NetDevice> =
        devices.sortedWith(compareByDescending<NetDevice> { it.isGateway }.thenByDescending { it.isSelf }.thenBy { it.ipSortKey })

    private fun hostsToScan(ip: String, prefix: Int): List<String> {
        val parts = ip.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return emptyList()
        val ipLong = parts.fold(0L) { acc, p -> acc * 256 + p }
        val p = prefix.coerceIn(22, 30)
        val mask = (0xFFFFFFFFL shl (32 - p)) and 0xFFFFFFFFL
        // Keep big subnets fast: scan at most a /23 (510 hosts) around our address.
        val effective = if (p < 23) 23 else p
        val effMask = (0xFFFFFFFFL shl (32 - effective)) and 0xFFFFFFFFL
        val network = ipLong and effMask and mask
        val count = (1L shl (32 - effective)) - 2
        return (1..count).map { i ->
            val v = network + i
            "${v shr 24 and 0xFF}.${v shr 16 and 0xFF}.${v shr 8 and 0xFF}.${v and 0xFF}"
        }
    }

    /** Returns latency and open ports when the host answered, or null when nothing is there. */
    private fun probe(ip: String): Pair<Int?, List<Int>>? {
        val addr = runCatching { InetAddress.getByName(ip) }.getOrNull() ?: return null
        var alive = false
        var latency: Int? = null
        val t0 = System.nanoTime()
        if (runCatching { addr.isReachable(350) }.getOrDefault(false)) {
            alive = true
            latency = ((System.nanoTime() - t0) / 1_000_000).toInt()
        }
        val open = mutableListOf<Int>()
        for ((index, port) in PORTS.withIndex()) {
            if (!alive && index >= 4) break
            val start = System.nanoTime()
            when (connect(addr, port)) {
                PortState.OPEN -> {
                    open += port
                    alive = true
                }
                PortState.REFUSED -> alive = true
                PortState.TIMEOUT -> Unit
            }
            if (alive && latency == null) latency = ((System.nanoTime() - start) / 1_000_000).toInt()
        }
        return if (alive) latency to open else null
    }

    private enum class PortState { OPEN, REFUSED, TIMEOUT }

    private fun connect(addr: InetAddress, port: Int): PortState = try {
        Socket().use { it.connect(InetSocketAddress(addr, port), 220) }
        PortState.OPEN
    } catch (e: ConnectException) {
        if (e.message?.contains("ECONNREFUSED") == true || e.message?.contains("refused", ignoreCase = true) == true) {
            PortState.REFUSED
        } else {
            PortState.TIMEOUT
        }
    } catch (e: Exception) {
        PortState.TIMEOUT
    }

    private fun reverseDns(ip: String): String? = runCatching {
        val name = InetAddress.getByName(ip).canonicalHostName
        if (name == null || name == ip) null else cleanName(name)
    }.getOrNull()

    private fun cleanName(raw: String): String? {
        val n = raw.trim().trimEnd('.')
            .removeSuffix(".local").removeSuffix(".lan").removeSuffix(".home").removeSuffix(".localdomain")
            .removeSuffix(".domain").removeSuffix(".router").removeSuffix(".gateway")
        if (n.isBlank() || n.all { it.isDigit() || it == '.' }) return null
        return n.substringBefore('.').ifBlank { n }
    }

    private fun readArp(): Map<String, String> = runCatching {
        File("/proc/net/arp").readLines().drop(1).mapNotNull { line ->
            val cols = line.split(Regex("\\s+"))
            if (cols.size >= 4 && cols[2] != "0x0" && cols[3] != "00:00:00:00:00:00") cols[0] to cols[3] else null
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun selfDevice(ip: String, isGateway: Boolean): NetDevice = NetDevice(
        ip = ip,
        hostname = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        mdnsName = null,
        mac = null,
        vendor = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        type = DeviceType.PHONE,
        openPorts = emptyList(),
        services = emptySet(),
        latencyMs = 0,
        isGateway = isGateway,
        isSelf = true,
    )

    private fun buildDevice(
        ip: String,
        latency: Int?,
        ports: List<Int>,
        isGateway: Boolean,
        mdns: MdnsHit?,
        mac: String?,
        hostname: String? = null,
    ): NetDevice {
        val services = mdns?.services ?: emptySet()
        val nameHint = listOfNotNull(mdns?.name, hostname).joinToString(" ")
        val vendor = Vendors.fromName(nameHint) ?: mac?.let { Vendors.fromMac(it) } ?: Vendors.fromServices(services)
        return NetDevice(
            ip = ip,
            hostname = hostname,
            mdnsName = mdns?.name,
            mac = mac,
            vendor = vendor,
            type = Vendors.inferType(nameHint, services, ports, vendor, isGateway),
            openPorts = ports.sorted(),
            services = services,
            latencyMs = latency,
            isGateway = isGateway,
            isSelf = false,
        )
    }

    // ---------------------------------------------------------------- mDNS

    data class MdnsHit(val name: String?, val services: Set<String>)

    private suspend fun discoverMdns(out: ConcurrentHashMap<String, MdnsHit>) {
        val nsd = context.getSystemService(NsdManager::class.java) ?: return
        val queue = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        val listeners = mutableListOf<NsdManager.DiscoveryListener>()
        MDNS_TYPES.forEach { type ->
            val l = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) = Unit
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) = Unit
                override fun onDiscoveryStarted(serviceType: String?) = Unit
                override fun onDiscoveryStopped(serviceType: String?) = Unit
                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    serviceInfo?.let { queue.trySend(it) }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo?) = Unit
            }
            runCatching {
                nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l)
                listeners += l
            }
        }
        val deadline = System.currentTimeMillis() + 7_000
        try {
            while (System.currentTimeMillis() < deadline) {
                val info = withTimeoutOrNull(deadline - System.currentTimeMillis()) { queue.receive() } ?: break
                val resolved = withTimeoutOrNull(2_500) { resolve(nsd, info) } ?: continue
                @Suppress("DEPRECATION")
                val host = resolved.host as? Inet4Address ?: continue
                val ip = host.hostAddress ?: continue
                val type = resolved.serviceType?.trim('.')?.substringBefore("._tcp")?.trimStart('.') ?: ""
                val friendly = resolved.attributes?.get("fn")?.let { String(it) }
                    ?: resolved.serviceName?.takeUnless { looksLikeId(it) }
                val prev = out[ip]
                out[ip] = MdnsHit(
                    name = prev?.name ?: friendly?.let { cleanMdnsName(it) },
                    services = (prev?.services ?: emptySet()) + type,
                )
            }
        } finally {
            listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
            queue.close()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolve(nsd: NsdManager, info: NsdServiceInfo): NsdServiceInfo? =
        suspendCancellableCoroutine { cont ->
            try {
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        if (cont.isActive) cont.resume(serviceInfo)
                    }
                })
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private fun looksLikeId(s: String): Boolean =
        s.length > 24 && s.count { it.isLetterOrDigit() } > s.length * 0.9 && s.none { it == ' ' }

    private fun cleanMdnsName(s: String): String =
        s.replace(Regex("\\[[0-9a-fA-F:]+]"), "").replace(Regex("-[0-9a-f]{12,}$"), "").trim()

    companion object {
        private val PORTS = listOf(80, 443, 62078, 445, 22, 8008, 8009, 554, 9100, 7000, 53, 8080, 5000, 139)

        private val MDNS_TYPES = listOf(
            "_googlecast._tcp", "_airplay._tcp", "_raop._tcp", "_companion-link._tcp",
            "_ipp._tcp", "_printer._tcp", "_spotify-connect._tcp", "_hap._tcp",
            "_smb._tcp", "_androidtvremote2._tcp", "_amzn-wplay._tcp", "_sonos._tcp",
        )

        fun portName(port: Int): String = when (port) {
            21 -> "FTP"
            22 -> "SSH"
            53 -> "DNS"
            80 -> "Web (HTTP)"
            139 -> "NetBIOS"
            443 -> "Web (HTTPS)"
            445 -> "File sharing (SMB)"
            554 -> "Video stream (RTSP)"
            5000 -> "UPnP / NAS"
            7000 -> "AirPlay"
            8008, 8009 -> "Chromecast"
            8080 -> "Web (alt)"
            9100 -> "Printer"
            62078 -> "Apple device sync"
            else -> "Port $port"
        }
    }
}

/** Best effort maker / type guesses from names, mDNS services, ports and MAC prefixes. */
object Vendors {
    private val nameRules = listOf(
        listOf("iphone", "ipad", "macbook", "imac", "apple", "airport", "homepod") to "Apple",
        listOf("galaxy", "samsung", "sm-") to "Samsung",
        listOf("pixel", "google", "chromecast", "nest") to "Google",
        listOf("xiaomi", "redmi", "poco", "mi-", "mibox") to "Xiaomi",
        listOf("huawei", "honor") to "Huawei",
        listOf("oneplus") to "OnePlus",
        listOf("oppo") to "OPPO",
        listOf("vivo") to "vivo",
        listOf("realme") to "realme",
        listOf("motorola", "moto") to "Motorola",
        listOf("bravia", "sony", "playstation", "ps4", "ps5") to "Sony",
        listOf("lgwebos", "lg-", "webos") to "LG",
        listOf("roku") to "Roku",
        listOf("echo", "amazon", "fire", "kindle", "alexa") to "Amazon",
        listOf("xbox", "surface", "microsoft") to "Microsoft",
        listOf("nintendo", "switch") to "Nintendo",
        listOf("hp", "officejet", "laserjet", "deskjet", "envy") to "HP",
        listOf("epson") to "Epson",
        listOf("canon") to "Canon",
        listOf("brother") to "Brother",
        listOf("tplink", "tp-link", "archer", "deco", "tapo", "kasa") to "TP-Link",
        listOf("netgear", "orbi") to "NETGEAR",
        listOf("asus", "rt-ax", "rt-ac") to "ASUS",
        listOf("dlink", "d-link") to "D-Link",
        listOf("linksys") to "Linksys",
        listOf("sonos") to "Sonos",
        listOf("tuya", "smartlife") to "Tuya",
        listOf("shelly") to "Shelly",
        listOf("esp", "espressif") to "Espressif",
        listOf("ring") to "Ring",
        listOf("wyze") to "Wyze",
        listOf("hikvision") to "Hikvision",
        listOf("dahua") to "Dahua",
        listOf("tcl") to "TCL",
        listOf("hisense") to "Hisense",
        listOf("lenovo", "thinkpad") to "Lenovo",
        listOf("dell") to "Dell",
        listOf("desktop-", "laptop-") to "Windows PC",
        listOf("android") to "Android",
    )

    private val ouiTable = mapOf(
        "f0:18:98" to "Apple", "3c:22:fb" to "Apple", "a4:83:e7" to "Apple", "ac:bc:32" to "Apple",
        "dc:a6:32" to "Raspberry Pi", "b8:27:eb" to "Raspberry Pi", "e4:5f:01" to "Raspberry Pi",
        "f4:f5:d8" to "Google", "54:60:09" to "Google", "3c:5a:b4" to "Google",
        "8c:79:f5" to "Samsung", "5c:49:7d" to "Samsung", "fc:a1:3e" to "Samsung",
        "50:c7:bf" to "TP-Link", "ec:08:6b" to "TP-Link", "98:da:c4" to "TP-Link",
        "a0:40:a0" to "NETGEAR", "9c:3d:cf" to "NETGEAR",
        "24:4b:fe" to "ASUS", "04:d9:f5" to "ASUS",
        "68:54:fd" to "Amazon", "fc:65:de" to "Amazon", "74:c2:46" to "Amazon",
        "24:0a:c4" to "Espressif", "84:f3:eb" to "Espressif", "ec:fa:bc" to "Espressif",
        "64:09:80" to "Xiaomi", "28:6c:07" to "Xiaomi", "78:11:dc" to "Xiaomi",
    )

    fun fromName(name: String): String? {
        if (name.isBlank()) return null
        val n = name.lowercase()
        return nameRules.firstOrNull { (keys, _) -> keys.any { k -> containsWord(n, k) } }?.second
    }

    private fun containsWord(n: String, k: String): Boolean {
        if (k.length > 3) return n.contains(k)
        // Short keys like "hp" or "lg-" must start a word to avoid false matches.
        return Regex("(^|[^a-z])" + Regex.escape(k)).containsMatchIn(n)
    }

    fun fromMac(mac: String): String? = ouiTable[mac.lowercase().take(8)]

    fun fromServices(services: Set<String>): String? = when {
        services.any { it.contains("companion-link") || it.contains("airplay") || it.contains("raop") } -> "Apple"
        services.any { it.contains("googlecast") } -> "Google"
        services.any { it.contains("amzn") } -> "Amazon"
        services.any { it.contains("sonos") } -> "Sonos"
        else -> null
    }

    fun inferType(name: String, services: Set<String>, ports: List<Int>, vendor: String?, isGateway: Boolean): DeviceType {
        if (isGateway) return DeviceType.ROUTER
        val n = name.lowercase()
        return when {
            listOf("ipad", "tab", "kindle").any { n.contains(it) } -> DeviceType.TABLET
            listOf("iphone", "galaxy", "pixel", "phone", "redmi", "oneplus").any { n.contains(it) } || 62078 in ports -> DeviceType.PHONE
            listOf("tv", "bravia", "roku", "chromecast", "firetv", "fire-tv", "webos", "shield").any { n.contains(it) } ||
                services.any { it.contains("googlecast") || it.contains("androidtvremote") || it.contains("amzn-wplay") } ||
                8008 in ports || 8009 in ports -> DeviceType.TV
            listOf("printer", "officejet", "laserjet", "deskjet", "epson", "canon", "brother").any { n.contains(it) } ||
                services.any { it.contains("ipp") || it.contains("printer") } || 9100 in ports -> DeviceType.PRINTER
            listOf("cam", "doorbell", "ring", "wyze", "hikvision", "dahua").any { n.contains(it) } || 554 in ports -> DeviceType.CAMERA
            listOf("xbox", "playstation", "ps4", "ps5", "nintendo", "switch").any { n.contains(it) } -> DeviceType.CONSOLE
            listOf("echo", "homepod", "sonos", "speaker", "nest-mini", "home-mini").any { n.contains(it) } ||
                services.any { it.contains("sonos") || it.contains("spotify") || it.contains("raop") } -> DeviceType.SPEAKER
            listOf("plug", "bulb", "light", "tuya", "shelly", "esp", "smart").any { n.contains(it) } ||
                services.any { it.contains("hap") } || vendor == "Espressif" || vendor == "Tuya" -> DeviceType.SMART_HOME
            listOf("macbook", "imac", "desktop", "laptop", "pc", "thinkpad", "mac").any { n.contains(it) } ||
                445 in ports || 139 in ports || 22 in ports || services.any { it.contains("smb") } -> DeviceType.COMPUTER
            services.any { it.contains("companion-link") } -> DeviceType.PHONE
            vendor in listOf("Samsung", "Xiaomi", "OnePlus", "OPPO", "vivo", "realme", "Motorola", "Huawei", "Android") -> DeviceType.PHONE
            else -> DeviceType.UNKNOWN
        }
    }
}
