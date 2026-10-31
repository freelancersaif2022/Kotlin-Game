package com.spectral.ghostdetector.ui.scanner

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Reads the phone's real magnetometer so the EMF needle reacts to nearby
 * electronics/metal — makes the prank feel authentic. Values are in micro-Tesla.
 */
class MagnetometerReader {
    @Volatile var magnitude: Float = 0f; private set
    @Volatile var baseline: Float = 0f; private set
    @Volatile var available: Boolean = false; internal set

    /** Deviation from the slowly-adapting baseline, in milliGauss (1 µT = 10 mG). */
    val deviationMg: Float get() = if (!available || baseline == 0f) 0f else abs(magnitude - baseline) * 10f

    internal fun onValues(x: Float, y: Float, z: Float) {
        val m = sqrt(x * x + y * y + z * z)
        magnitude = m
        baseline = if (baseline == 0f) m else baseline * 0.985f + m * 0.015f
    }
}

@Composable
fun rememberMagnetometer(): MagnetometerReader {
    val context = LocalContext.current
    val reader = remember { MagnetometerReader() }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) = reader.onValues(e.values[0], e.values[1], e.values[2])
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        if (sensor != null) {
            reader.available = true
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sm.unregisterListener(listener) }
    }
    return reader
}
