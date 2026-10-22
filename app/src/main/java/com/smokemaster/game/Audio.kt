package com.smokemaster.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/** Sound effects through SoundPool (low latency) and looping music through MediaPlayer. */
class Audio(private val context: Context, private val settings: Settings) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val ids = HashMap<String, Int>()
    @Volatile private var ready = false

    private var player: MediaPlayer? = null
    private var track: String? = null
    private var prepared = false
    private var paused = false
    private var volume = 1f
    private var targetVolume = 1f

    fun loadAll() {
        val map = HashMap<String, Int>()
        for (name in EFFECTS) {
            try {
                context.assets.openFd("sfx/$name.ogg").use { map[name] = pool.load(it, 1) }
            } catch (_: Exception) {
            }
        }
        synchronized(ids) { ids.putAll(map) }
        ready = true
    }

    fun play(name: String, vol: Float = 1f, rate: Float = 1f): Int {
        if (!settings.sound || !ready) return 0
        val id = synchronized(ids) { ids[name] } ?: return 0
        return pool.play(id, vol, vol, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun stop(stream: Int) {
        if (stream != 0) pool.stop(stream)
    }

    /** Starts (or keeps) a looping music track. */
    fun music(name: String, vol: Float = 0.55f) {
        targetVolume = vol
        if (track == name && player != null) {
            if (!paused && prepared && player?.isPlaying == false && settings.music) player?.start()
            return
        }
        releasePlayer()
        track = name
        if (settings.music) startPlayer()
    }

    fun fadeMusicTo(vol: Float) {
        targetVolume = vol
    }

    fun update(dt: Float) {
        val p = player ?: return
        if (!prepared) return
        if (volume != targetVolume) {
            val step = dt * 0.8f
            volume = if (volume < targetVolume) minOf(targetVolume, volume + step) else maxOf(targetVolume, volume - step)
            p.setVolume(volume, volume)
        }
    }

    fun setMusicEnabled(on: Boolean) {
        settings.music = on
        if (on) {
            if (player == null && track != null) startPlayer()
        } else {
            releasePlayer(keepTrack = true)
        }
    }

    fun pause() {
        paused = true
        pool.autoPause()
        try {
            if (player?.isPlaying == true) player?.pause()
        } catch (_: IllegalStateException) {
        }
    }

    fun resume() {
        paused = false
        pool.autoResume()
        try {
            if (prepared && settings.music) player?.start()
        } catch (_: IllegalStateException) {
        }
    }

    fun release() {
        releasePlayer()
        pool.release()
    }

    private fun startPlayer() {
        val name = track ?: return
        try {
            val mp = MediaPlayer()
            context.assets.openFd("sfx/$name.ogg").use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.isLooping = true
            volume = 0f
            mp.setVolume(0f, 0f)
            mp.setOnPreparedListener {
                if (player === it) {
                    prepared = true
                    if (!paused) it.start()
                }
            }
            player = mp
            prepared = false
            mp.prepareAsync()
        } catch (_: Exception) {
            player = null
        }
    }

    private fun releasePlayer(keepTrack: Boolean = false) {
        player?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
        prepared = false
        if (!keepTrack) track = null
    }

    companion object {
        val EFFECTS = listOf(
            "click", "pickup", "whoosh", "lighter", "inhale", "exhale", "cough", "heartbeat",
            "explosion", "sizzle", "ding", "perfect", "miss", "gameover", "new_record", "pop",
        )
    }
}
