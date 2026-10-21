package com.netscope.wifianalyzer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HeatPoint(val row: Int, val col: Int, val rssi: Int)

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
}
