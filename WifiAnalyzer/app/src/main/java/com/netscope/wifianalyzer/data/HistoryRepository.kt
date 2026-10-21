package com.netscope.wifianalyzer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SpeedResult(
    val id: Long,
    val timestamp: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val network: String,
    val connection: String,
    val isp: String,
    val server: String,
    val rssi: Int?,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("ts", timestamp)
        .put("dl", downloadMbps)
        .put("ul", uploadMbps)
        .put("ping", pingMs)
        .put("jitter", jitterMs)
        .put("net", network)
        .put("conn", connection)
        .put("isp", isp)
        .put("server", server)
        .put("rssi", rssi ?: Int.MIN_VALUE)

    companion object {
        fun fromJson(o: JSONObject) = SpeedResult(
            id = o.optLong("id"),
            timestamp = o.optLong("ts"),
            downloadMbps = o.optDouble("dl", 0.0),
            uploadMbps = o.optDouble("ul", 0.0),
            pingMs = o.optDouble("ping", 0.0),
            jitterMs = o.optDouble("jitter", 0.0),
            network = o.optString("net"),
            connection = o.optString("conn", "Wi-Fi"),
            isp = o.optString("isp"),
            server = o.optString("server"),
            rssi = o.optInt("rssi", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
        )
    }
}

class HistoryRepository(private val context: Context) {
    private val file = File(context.filesDir, "speed_history.json")
    private val _results = MutableStateFlow(load())
    val results: StateFlow<List<SpeedResult>> = _results.asStateFlow()

    private fun load(): List<SpeedResult> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { SpeedResult.fromJson(arr.getJSONObject(it)) }
            .sortedByDescending { it.timestamp }
    }.getOrDefault(emptyList())

    @Synchronized
    private fun save(list: List<SpeedResult>) {
        _results.value = list
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            file.writeText(arr.toString())
        }
    }

    fun add(result: SpeedResult) = save((listOf(result) + _results.value).take(MAX_ITEMS))

    fun delete(id: Long) = save(_results.value.filterNot { it.id == id })

    fun clear() = save(emptyList())

    /** Writes the history as CSV into the cache and returns the file for sharing. */
    fun exportCsv(): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val out = File(dir, "wifi_speed_history.csv")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder("Date,Network,Connection,Download (Mbps),Upload (Mbps),Ping (ms),Jitter (ms),Signal (dBm),Provider,Server\n")
        _results.value.forEach { r ->
            sb.append(fmt.format(Date(r.timestamp))).append(',')
                .append(csv(r.network)).append(',')
                .append(csv(r.connection)).append(',')
                .append(String.format(Locale.US, "%.2f", r.downloadMbps)).append(',')
                .append(String.format(Locale.US, "%.2f", r.uploadMbps)).append(',')
                .append(String.format(Locale.US, "%.0f", r.pingMs)).append(',')
                .append(String.format(Locale.US, "%.0f", r.jitterMs)).append(',')
                .append(r.rssi?.toString() ?: "").append(',')
                .append(csv(r.isp)).append(',')
                .append(csv(r.server)).append('\n')
        }
        out.writeText(sb.toString())
        return out
    }

    private fun csv(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    private companion object {
        const val MAX_ITEMS = 500
    }
}
