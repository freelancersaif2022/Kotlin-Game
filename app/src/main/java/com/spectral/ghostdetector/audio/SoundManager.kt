package com.spectral.ghostdetector.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.spectral.ghostdetector.R

enum class Sfx(val res: Int) {
    BOOT(R.raw.boot),
    PING(R.raw.ping),
    ALARM(R.raw.alarm),
    TICK(R.raw.tick),
    STATIC(R.raw.static_burst),
    WHISPER(R.raw.whisper),
    CLICK(R.raw.click),
    SCAN_START(R.raw.scan_start),
    GLITCH(R.raw.glitch),
    VANISH(R.raw.vanish),
    LOCK(R.raw.lock_beep),
}

enum class Loop(val res: Int) {
    HUM(R.raw.hum_loop),
    HEARTBEAT(R.raw.heartbeat_loop),
    DRONE(R.raw.drone_loop),
}

/** Low-latency sound effects + seamless loops, all synthesized in-house (res/raw). */
class SoundManager(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(14)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = HashMap<Int, Int>()      // res -> soundId
    private val loaded = HashSet<Int>()             // soundIds ready to play
    private val loopStreams = HashMap<Loop, Int>()  // active loop -> streamId
    private val wantedLoops = HashMap<Loop, Float>()// loops the UI wants playing -> volume
    private var foreground = true

    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                loopStreams.values.forEach { pool.stop(it) }
                loopStreams.clear()
            } else {
                syncLoops()
            }
        }

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loaded += sampleId
                syncLoops()
            }
        }
        (Sfx.entries.map { it.res } + Loop.entries.map { it.res }).forEach { res ->
            soundIds[res] = pool.load(context, res, 1)
        }
    }

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled || !foreground) return
        val id = soundIds[sfx.res] ?: return
        if (id !in loaded) return
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun startLoop(loop: Loop, volume: Float = 0.5f) {
        wantedLoops[loop] = volume
        syncLoops()
    }

    fun setLoopVolume(loop: Loop, volume: Float) {
        if (loop !in wantedLoops) return
        wantedLoops[loop] = volume
        loopStreams[loop]?.let { pool.setVolume(it, volume, volume) }
    }

    fun stopLoop(loop: Loop) {
        wantedLoops.remove(loop)
        loopStreams.remove(loop)?.let { pool.stop(it) }
    }

    fun onForeground() {
        foreground = true
        pool.autoResume()
        syncLoops()
    }

    fun onBackground() {
        foreground = false
        pool.autoPause()
    }

    private fun syncLoops() {
        if (!enabled || !foreground) return
        for ((loop, vol) in wantedLoops) {
            if (loop in loopStreams) continue
            val id = soundIds[loop.res] ?: continue
            if (id !in loaded) continue
            val stream = pool.play(id, vol, vol, 2, -1, 1f)
            if (stream != 0) loopStreams[loop] = stream
        }
    }
}
