package com.bramble.dash.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.LongArray as GdxLongArray

/** Short effects: preloaded 16-bit WAV, played through LibGDX Sound. */
enum class Sfx(val file: String, val volume: Float = 1f, val minInterval: Float = 0.03f, val maxVoices: Int = 3) {
    UI_CLICK("ui_click", 0.7f), UI_BACK("ui_back", 0.7f),
    PURCHASE_OK("purchase_ok"), PURCHASE_FAIL("purchase_fail"),
    JUMP("jump", 0.55f), DOUBLE_JUMP("double_jump", 0.6f), SLIDE("slide", 0.5f, 0.15f, 1), LAND("land", 0.45f, 0.08f, 1),
    COIN("coin", 0.45f, 0.055f, 4), GEM("gem", 0.7f, 0.06f, 2),
    POWERUP_PICKUP("powerup_pickup", 0.7f), POWERUP_ACTIVATE("powerup_activate", 0.7f),
    SHIELD_BREAK("shield_break", 0.8f), DAMAGE("damage", 0.85f, 0.15f, 1), DEATH("death", 0.9f, 0.5f, 1), REVIVE("revive"),
    ENEMY_ATTACK("enemy_attack", 0.6f, 0.12f, 2), ENEMY_DEFEAT("enemy_defeat", 0.75f),
    ENERGY_SHOT("energy_shot", 0.6f, 0.08f, 3),
    BOSS_WARNING("boss_warning", 0.85f, 1f, 1), BOSS_ATTACK("boss_attack", 0.8f, 0.15f, 2), BOSS_DEFEAT("boss_defeat", 1f, 1f, 1),
    EXPLOSION("explosion", 0.8f, 0.1f, 2), CHEST_OPEN("chest_open"), UNLOCK("unlock"),
    MISSION_COMPLETE("mission_complete", 0.8f, 0.3f, 1), VICTORY("victory", 0.9f, 1f, 1), SPRING("spring", 0.7f),
    ;
    val path get() = "audio/sfx/$file.wav"
}

/** Streamed loops (OGG Vorbis). */
enum class MusicTrack(val file: String, val volume: Float) {
    MENU("menu", 0.55f), RUN("run", 0.5f), BOSS("boss", 0.6f),
    AMB_NATURE("amb_nature", 0.35f), AMB_WIND("amb_wind", 0.35f), AMB_RUMBLE("amb_rumble", 0.4f),
    AMB_HUM("amb_hum", 0.3f), AMB_EERIE("amb_eerie", 0.35f);
    val path get() = "audio/music/$file.ogg"

    companion object {
        fun ambience(key: String) = when (key) {
            "wind" -> AMB_WIND; "rumble" -> AMB_RUMBLE; "hum" -> AMB_HUM; "eerie" -> AMB_EERIE
            else -> AMB_NATURE
        }
    }
}

/**
 * Plays preloaded sounds with per-effect rate limiting and voice caps, and streams music with
 * short volume fades. While [suspended] (app paused / game paused) play requests are dropped,
 * so nothing queues up and replays as a burst after resuming.
 */
class AudioService(
    private val soundProvider: (Sfx) -> Sound,
    private val musicFactory: (MusicTrack) -> Music,
) : Disposable {
    var sfxVolume = 0.8f
    var musicVolume = 0.6f
    var muted = false
        set(value) { field = value; applyMusicVolume() }

    private var clock = 0f
    private val lastPlayed = FloatArray(Sfx.values().size) { -10f }
    private val voices = Array(Sfx.values().size) { GdxLongArray() }
    private val voiceStart = Array(Sfx.values().size) { com.badlogic.gdx.utils.FloatArray() }
    private var suspended = false

    private inner class Channel {
        var track: MusicTrack? = null
        var music: Music? = null
        var fade = 1f
        var fadeTarget = 1f
        var outgoing: Music? = null
        var outgoingTrack: MusicTrack? = null
        var outgoingFade = 0f

        fun play(t: MusicTrack?) {
            if (t == track) return
            outgoing?.let { it.stop(); it.dispose() }
            outgoing = music
            outgoingTrack = track
            outgoingFade = fade
            track = t
            music = t?.let { musicFactory(it).apply { isLooping = true } }
            fade = 0f
            fadeTarget = 1f
            music?.let { it.volume = 0f; if (!suspended) it.play() }
        }

        fun update(dt: Float) {
            if (fade < fadeTarget) fade = minOf(fadeTarget, fade + dt / 0.8f)
            outgoing?.let {
                outgoingFade -= dt / 0.6f
                if (outgoingFade <= 0f) { it.stop(); it.dispose(); outgoing = null; outgoingTrack = null }
                else it.volume = volumeFor(outgoingTrack) * outgoingFade
            }
            music?.volume = volumeFor(track) * fade
        }

        fun pause() { music?.pause(); outgoing?.pause() }
        fun resume() { music?.play(); outgoing?.play() }
        fun dispose() { music?.dispose(); outgoing?.dispose(); music = null; outgoing = null; track = null }
    }

    private val musicChannel = Channel()
    private val ambienceChannel = Channel()

    private fun volumeFor(t: MusicTrack?) = if (muted || t == null) 0f else t.volume * musicVolume

    fun play(sfx: Sfx, volume: Float = 1f, pitch: Float = 1f) {
        if (suspended || muted || sfxVolume <= 0f) return
        val i = sfx.ordinal
        if (clock - lastPlayed[i] < sfx.minInterval) return
        lastPlayed[i] = clock
        val sound = soundProvider(sfx)
        val ids = voices[i]
        if (ids.size >= sfx.maxVoices) {
            sound.stop(ids.removeIndex(0))
            voiceStart[i].removeIndex(0)
        }
        val id = sound.play((sfx.volume * volume * sfxVolume).coerceIn(0f, 1f), pitch, 0f)
        if (id != -1L) { ids.add(id); voiceStart[i].add(clock) }
    }

    fun playMusic(track: MusicTrack?) = musicChannel.play(track)
    fun playAmbience(track: MusicTrack?) = ambienceChannel.play(track)

    fun update(dt: Float) {
        clock += dt
        // Forget voices older than the longest effect (~1.5 s) so the cap only counts audible ones.
        for (i in voices.indices) {
            val starts = voiceStart[i]
            while (starts.size > 0 && clock - starts[0] > 1.5f) { starts.removeIndex(0); voices[i].removeIndex(0) }
        }
        musicChannel.update(dt)
        ambienceChannel.update(dt)
    }

    fun applyMusicVolume() {
        musicChannel.update(0f)
        ambienceChannel.update(0f)
    }

    /** App lifecycle / interruption. */
    fun suspend() {
        if (suspended) return
        suspended = true
        musicChannel.pause()
        ambienceChannel.pause()
        for (s in Sfx.values()) runCatching { soundProvider(s).stop() }
        voices.forEach { it.clear() }
        voiceStart.forEach { it.clear() }
    }

    fun resume() {
        if (!suspended) return
        suspended = false
        musicChannel.resume()
        ambienceChannel.resume()
    }

    /** Stop all effect voices (e.g. when the game is paused) without touching music. */
    fun stopEffects() {
        for (s in Sfx.values()) runCatching { soundProvider(s).stop() }
        voices.forEach { it.clear() }
        voiceStart.forEach { it.clear() }
    }

    override fun dispose() {
        musicChannel.dispose()
        ambienceChannel.dispose()
    }
}
