package com.netscope.wifianalyzer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class KnownDevice(
    val key: String,
    val name: String,
    val ip: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val trusted: Boolean,
    val customName: String?,
)

/**
 * Remembers every device seen per network so we can flag new ones
 * ("New" badge, intruder alerts) and keep the names people give them.
 */
class KnownDevicesRepository(context: Context) {
    private val file = File(context.filesDir, "known_devices.json")
    private var root: JSONObject = runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    @Synchronized
    fun isBaselined(networkKey: String): Boolean = root.has(networkKey)

    @Synchronized
    fun devices(networkKey: String): List<KnownDevice> {
        val net = root.optJSONObject(networkKey) ?: return emptyList()
        return net.keys().asSequence().mapNotNull { k ->
            val o = net.optJSONObject(k) ?: return@mapNotNull null
            KnownDevice(
                key = k,
                name = o.optString("name"),
                ip = o.optString("ip"),
                firstSeen = o.optLong("first"),
                lastSeen = o.optLong("last"),
                trusted = o.optBoolean("trusted", false),
                customName = o.optString("custom").takeIf { it.isNotBlank() },
            )
        }.sortedByDescending { it.lastSeen }.toList()
    }

    @Synchronized
    fun device(networkKey: String, key: String): KnownDevice? = devices(networkKey).firstOrNull { it.key == key }

    /**
     * Stores the scan and returns the keys that were never seen on this network before.
     * The very first scan of a network only creates the baseline, so it returns nothing.
     */
    @Synchronized
    fun merge(networkKey: String, seen: List<Triple<String, String, String>>): Set<String> {
        val firstTime = !root.has(networkKey)
        val net = root.optJSONObject(networkKey) ?: JSONObject().also { root.put(networkKey, it) }
        val now = System.currentTimeMillis()
        val fresh = mutableSetOf<String>()
        seen.forEach { (key, name, ip) ->
            val existing = net.optJSONObject(key)
            if (existing == null) {
                if (!firstTime) fresh += key
                net.put(key, JSONObject().put("name", name).put("ip", ip).put("first", now).put("last", now).put("trusted", firstTime))
            } else {
                existing.put("last", now).put("ip", ip)
                if (name.isNotBlank()) existing.put("name", name)
            }
        }
        persist()
        return fresh
    }

    @Synchronized
    fun setTrusted(networkKey: String, key: String, trusted: Boolean) {
        root.optJSONObject(networkKey)?.optJSONObject(key)?.put("trusted", trusted)
        persist()
    }

    @Synchronized
    fun rename(networkKey: String, key: String, name: String?) {
        root.optJSONObject(networkKey)?.optJSONObject(key)?.put("custom", name.orEmpty())
        persist()
    }

    @Synchronized
    fun forget(networkKey: String) {
        root.remove(networkKey)
        persist()
    }

    private fun persist() {
        runCatching { file.writeText(root.toString()) }
        _version.value++
    }
}
