package com.eyescroll.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ScrollStyle(val label: String, val description: String) {
    NATURAL("Natural", "Finger up = page moves up, like swiping the screen"),
    JOYSTICK("Joystick", "Finger up = go back up the page"),
}

enum class PadSize(val label: String, val widthDp: Int, val heightDp: Int) {
    SMALL("Small", 40, 104),
    MEDIUM("Medium", 48, 128),
    LARGE("Large", 58, 156),
}

enum class ScrollSpeed(val label: String, val durationMs: Long) {
    SMOOTH("Smooth", 420),
    BALANCED("Balanced", 300),
    FAST("Fast", 200),
}

data class AppSettings(
    val onboardingDone: Boolean = false,
    /** Floating pad visible (needs the accessibility service). */
    val padEnabled: Boolean = true,
    val style: ScrollStyle = ScrollStyle.NATURAL,
    /** How far (dp) the finger must move before it counts as a scroll. */
    val triggerDp: Int = 8,
    /** Fraction of the screen scrolled by a full-length swipe on the pad. */
    val maxDistance: Float = 0.7f,
    val speed: ScrollSpeed = ScrollSpeed.BALANCED,
    /** 0..1: how fast it keeps scrolling while you hold. */
    val holdSpeed: Float = 0.5f,
    val padSize: PadSize = PadSize.MEDIUM,
    val padOpacity: Float = 0.9f,
    val haptics: Boolean = true,
    val padX: Int = -1,
    val padY: Int = -1,
)

/** Small SharedPreferences-backed store, exposed as a StateFlow so UI and services stay in sync. */
class SettingsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("fingerscroll", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_settings.value)
        _settings.value = next
        write(next)
    }

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboardingDone = prefs.getBoolean("onboardingDone", d.onboardingDone),
            padEnabled = prefs.getBoolean("padEnabled", d.padEnabled),
            style = enumOr(prefs.getString("style", null), d.style),
            triggerDp = prefs.getInt("triggerDp", d.triggerDp),
            maxDistance = prefs.getFloat("maxDistance", d.maxDistance),
            speed = enumOr(prefs.getString("speed", null), d.speed),
            holdSpeed = prefs.getFloat("holdSpeed", d.holdSpeed),
            padSize = enumOr(prefs.getString("padSize", null), d.padSize),
            padOpacity = prefs.getFloat("padOpacity", d.padOpacity),
            haptics = prefs.getBoolean("haptics", d.haptics),
            padX = prefs.getInt("padX", d.padX),
            padY = prefs.getInt("padY", d.padY),
        )
    }

    private fun write(s: AppSettings) {
        prefs.edit()
            .putBoolean("onboardingDone", s.onboardingDone)
            .putBoolean("padEnabled", s.padEnabled)
            .putString("style", s.style.name)
            .putInt("triggerDp", s.triggerDp)
            .putFloat("maxDistance", s.maxDistance)
            .putString("speed", s.speed.name)
            .putFloat("holdSpeed", s.holdSpeed)
            .putString("padSize", s.padSize.name)
            .putFloat("padOpacity", s.padOpacity)
            .putBoolean("haptics", s.haptics)
            .putInt("padX", s.padX)
            .putInt("padY", s.padY)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
