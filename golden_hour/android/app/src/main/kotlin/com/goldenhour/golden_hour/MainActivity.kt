package com.goldenhour.golden_hour

import android.hardware.GeomagneticField
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // Magnetic declination (offline World Magnetic Model built into Android),
        // so the compass and AR view point to TRUE north.
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "golden_hour/geomag")
            .setMethodCallHandler { call, result ->
                if (call.method == "declination") {
                    val lat = call.argument<Double>("lat") ?: 0.0
                    val lng = call.argument<Double>("lng") ?: 0.0
                    val time = call.argument<Long>("time") ?: System.currentTimeMillis()
                    val field = GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, time)
                    result.success(field.declination.toDouble())
                } else {
                    result.notImplemented()
                }
            }
    }
}
