package com.netscope.wifianalyzer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HeatPoint(val row: Int, val col: Int, val rssi: Int)

/** A spot marked in live camera mode, e.g. "Kitchen: -61 dBm". */
data class SignalSpot(val name: String, val rssi: Int, val time: Long)

class HeatmapRepository(context: Context) {
    private val file = File(context.filesDir, "heatmaps.json")
    private val root: JSONObject = runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())

    @Synchronized
    fun load(networkKey: String): List<HeatPoint> {
        val arr = root.optJSONArray(networkKey) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { HeatPoint(it.optInt("r"), it.optInt("c"), it.optInt("s")) }
        }
    }

    @Synchronized
    fun save(networkKey: String, points: List<HeatPoint>) {
        val arr = JSONArray()
        points.forEach { arr.put(JSONObject().put("r", it.row).put("c", it.col).put("s", it.rssi)) }
        root.put(networkKey, arr)
        runCatching { file.writeText(root.toString()) }
    }

    @Synchronized
    fun loadSpots(networkKey: String): List<SignalSpot> {
        val arr = root.optJSONArray("$networkKey#spots") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { SignalSpot(it.optString("n"), it.optInt("s"), it.optLong("t")) }
        }
    }

    @Synchronized
    fun saveSpots(networkKey: String, spots: List<SignalSpot>) {
        val arr = JSONArray()
        spots.forEach { arr.put(JSONObject().put("n", it.name).put("s", it.rssi).put("t", it.time)) }
        root.put("$networkKey#spots", arr)
        runCatching { file.writeText(root.toString()) }
    }
}
