package com.bornomala.keyboard.ime

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bornomala.keyboard.layout.Key
import com.bornomala.keyboard.layout.KeyCode

/** Key press vibration and click sound. */
class Feedback(context: Context) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var vibrate = true
    var vibrateMs = 15
    var sound = false
    var soundVolume = 0.3f

    fun onKey(key: Key?) {
        if (vibrate && vibrateMs > 0) vibrate(vibrateMs.toLong())
        if (sound) {
            val effect = when (key?.code) {
                KeyCode.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                KeyCode.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                KeyCode.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audio.playSoundEffect(effect, soundVolume)
        }
    }

    private fun vibrate(ms: Long) {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (e: Exception) {
            // Some devices restrict vibration; typing must never crash because of it.
        }
    }
}
