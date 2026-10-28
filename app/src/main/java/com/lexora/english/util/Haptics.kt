package com.lexora.english.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lexora.english.data.AppRepository

class Haptics(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val enabled: Boolean get() = AppRepository.get(context).current.settings.vibration

    private fun play(predefined: Int, fallbackMs: Long, amplitude: Int) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(predefined))
            } else {
                v.vibrate(VibrationEffect.createOneShot(fallbackMs, amplitude))
            }
        } catch (_: Exception) {
        }
    }

    fun tick() = play(if (Build.VERSION.SDK_INT >= 29) VibrationEffect.EFFECT_TICK else 0, 12, 60)
    fun success() = play(if (Build.VERSION.SDK_INT >= 29) VibrationEffect.EFFECT_CLICK else 0, 25, 120)
    fun error() = play(if (Build.VERSION.SDK_INT >= 29) VibrationEffect.EFFECT_DOUBLE_CLICK else 0, 60, 200)
    fun heavy() = play(if (Build.VERSION.SDK_INT >= 29) VibrationEffect.EFFECT_HEAVY_CLICK else 0, 40, 255)
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { Haptics(context.applicationContext) }
}
