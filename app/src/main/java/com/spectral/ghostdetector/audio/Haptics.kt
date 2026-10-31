package com.spectral.ghostdetector.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    var enabled = true

    fun tap() = wave(longArrayOf(0, 12), intArrayOf(0, 90))
    fun detect() = wave(longArrayOf(0, 70, 90, 140), intArrayOf(0, 200, 0, 255))
    fun alarm() = wave(longArrayOf(0, 120, 80, 120, 80, 220), intArrayOf(0, 255, 0, 255, 0, 255))
    fun vanish() = wave(longArrayOf(0, 40, 60, 30), intArrayOf(0, 120, 0, 60))

    private fun wave(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        runCatching {
            val effect = if (v.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            v.vibrate(effect)
        }
    }
}
