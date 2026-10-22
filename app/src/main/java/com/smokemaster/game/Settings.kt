package com.smokemaster.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("smoke_master", Context.MODE_PRIVATE)

    var music: Boolean = prefs.getBoolean("music", true)
        set(v) { field = v; prefs.edit().putBoolean("music", v).apply() }
    var sound: Boolean = prefs.getBoolean("sound", true)
        set(v) { field = v; prefs.edit().putBoolean("sound", v).apply() }
    var vibration: Boolean = prefs.getBoolean("vibration", true)
        set(v) { field = v; prefs.edit().putBoolean("vibration", v).apply() }

    /** Top five scores, best first. */
    val highScores: MutableList<Int> = prefs.getString("scores", "")!!
        .split(',').mapNotNull { it.toIntOrNull() }.sortedDescending().take(5).toMutableList()

    val best: Int get() = highScores.firstOrNull() ?: 0

    /** Records a score; returns true when it beats the previous best. */
    fun submitScore(score: Int): Boolean {
        if (score <= 0) return false
        val newBest = score > best
        highScores.add(score)
        highScores.sortDescending()
        while (highScores.size > 5) highScores.removeAt(highScores.size - 1)
        prefs.edit().putString("scores", highScores.joinToString(",")).apply()
        return newBest
    }
}

class Haptics(context: Context, private val settings: Settings) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun buzz(ms: Long, amplitude: Int = 120) {
        if (!settings.vibration) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }
}
