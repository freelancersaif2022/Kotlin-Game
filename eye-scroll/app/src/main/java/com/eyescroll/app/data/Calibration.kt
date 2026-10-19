package com.eyescroll.app.data

import com.eyescroll.app.tracking.CalibrationSample
import com.eyescroll.app.tracking.GazeModel
import org.json.JSONArray
import org.json.JSONObject

/** Raw calibration samples. The model is re-fitted from these, so switching tracking mode needs no recalibration. */
class Calibration(val samples: List<CalibrationSample>, val createdAt: Long) {

    fun model(mode: TrackingMode): GazeModel? =
        GazeModel.fit(samples, useHead = mode == TrackingMode.EYES_AND_HEAD)

    fun toJson(): String {
        val arr = JSONArray()
        for (s in samples) {
            val o = JSONObject()
            o.put("t", s.targetY.toDouble())
            val f = JSONArray()
            for (v in s.features) f.put(v.toDouble())
            o.put("f", f)
            arr.put(o)
        }
        return JSONObject().put("createdAt", createdAt).put("samples", arr).toString()
    }

    companion object {
        fun fromJson(json: String?): Calibration? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                val root = JSONObject(json)
                val arr = root.getJSONArray("samples")
                val list = ArrayList<CalibrationSample>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val f = o.getJSONArray("f")
                    list += CalibrationSample(
                        o.getDouble("t").toFloat(),
                        FloatArray(f.length()) { f.getDouble(it).toFloat() },
                    )
                }
                Calibration(list, root.optLong("createdAt"))
            }.getOrNull()
        }
    }
}
