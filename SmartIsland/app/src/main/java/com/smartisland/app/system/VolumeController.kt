package com.smartisland.app.system

import android.content.Context
import android.media.AudioManager
import android.os.Build
import kotlin.math.roundToInt

enum class VolumeStream(val stream: Int, val label: String) {
    MEDIA(AudioManager.STREAM_MUSIC, "Media"),
    RING(AudioManager.STREAM_RING, "Ringtone"),
    NOTIFICATION(AudioManager.STREAM_NOTIFICATION, "Notifications"),
    ALARM(AudioManager.STREAM_ALARM, "Alarm"),
    CALL(AudioManager.STREAM_VOICE_CALL, "Call"),
}

/** Percent-based volume control (0..100) on top of the device's per-stream index range. */
class VolumeController(context: Context) {

    private val audio = context.systemService<AudioManager>()

    fun percent(stream: VolumeStream): Int {
        val min = minIndex(stream)
        val max = audio.getStreamMaxVolume(stream.stream)
        if (max <= min) return 0
        val cur = audio.getStreamVolume(stream.stream)
        return (((cur - min).toFloat() / (max - min)) * 100f).roundToInt().coerceIn(0, 100)
    }

    fun steps(stream: VolumeStream): Int = audio.getStreamMaxVolume(stream.stream) - minIndex(stream)

    /** Returns false when the platform refused the change (e.g. ringer to 0 without DND access). */
    fun setPercent(stream: VolumeStream, percent: Int, showSystemUi: Boolean = false): Boolean {
        val min = minIndex(stream)
        val max = audio.getStreamMaxVolume(stream.stream)
        val index = (min + (max - min) * (percent.coerceIn(0, 100) / 100f)).roundToInt()
        return runCatching {
            audio.setStreamVolume(stream.stream, index, if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0)
        }.isSuccess
    }

    fun step(stream: VolumeStream, up: Boolean, showSystemUi: Boolean = false): Boolean = runCatching {
        audio.adjustStreamVolume(
            stream.stream,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0,
        )
    }.isSuccess

    fun isMuted(stream: VolumeStream): Boolean = audio.isStreamMute(stream.stream)

    fun toggleMute(stream: VolumeStream): Boolean = runCatching {
        audio.adjustStreamVolume(stream.stream, AudioManager.ADJUST_TOGGLE_MUTE, 0)
    }.isSuccess

    val ringerMode: Int get() = audio.ringerMode

    fun setRingerMode(mode: Int): Boolean = runCatching { audio.ringerMode = mode }.isSuccess

    val isInCall: Boolean
        get() = audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION ||
            audio.mode == AudioManager.MODE_RINGTONE

    private fun minIndex(stream: VolumeStream): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream.stream) else 0
}
