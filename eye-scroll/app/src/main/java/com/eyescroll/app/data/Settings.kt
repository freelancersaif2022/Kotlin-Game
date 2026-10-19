package com.eyescroll.app.data

import android.content.Context
import android.content.SharedPreferences
import com.eyescroll.app.gesture.GestureMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ScrollDistance(val label: String, val fraction: Float) {
    SHORT("Short", 0.25f),
    MEDIUM("Medium", 0.45f),
    LONG("Long", 0.65f),
}

data class AppSettings(
    val onboardingDone: Boolean = false,
    val mode: GestureMode = GestureMode.SWIPE,
    /** Swipe: hand up = page moves up (touchscreen feel). Hold: finger up = scroll up. */
    val natural: Boolean = true,
    /** 0 = needs a big movement, 1 = reacts to a small movement. */
    val sensitivity: Float = 0.5f,
    val scrollDistance: ScrollDistance = ScrollDistance.MEDIUM,
    /** Hold mode: delay between repeated scrolls. */
    val repeatMs: Int = 700,
    /** Camera turns off automatically after this many idle seconds; 0 = never. */
    val autoOffSeconds: Int = 60,
    val targetFps: Int = 30,
    val showBubble: Boolean = true,
    val vibrate: Boolean = true,
    val bubbleX: Int = -1,
    val bubbleY: Int = -1,
)

/** Small SharedPreferences-backed store, exposed as a StateFlow so UI and services stay in sync. */
class SettingsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("airscroll", Context.MODE_PRIVATE)

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
            mode = enumOr(prefs.getString("mode", null), d.mode),
            natural = prefs.getBoolean("natural", d.natural),
            sensitivity = prefs.getFloat("sensitivity", d.sensitivity),
            scrollDistance = enumOr(prefs.getString("scrollDistance", null), d.scrollDistance),
            repeatMs = prefs.getInt("repeatMs", d.repeatMs),
            autoOffSeconds = prefs.getInt("autoOffSeconds", d.autoOffSeconds),
            targetFps = prefs.getInt("targetFps", d.targetFps),
            showBubble = prefs.getBoolean("showBubble", d.showBubble),
            vibrate = prefs.getBoolean("vibrate", d.vibrate),
            bubbleX = prefs.getInt("bubbleX", d.bubbleX),
            bubbleY = prefs.getInt("bubbleY", d.bubbleY),
        )
    }

    private fun write(s: AppSettings) {
        prefs.edit()
            .putBoolean("onboardingDone", s.onboardingDone)
            .putString("mode", s.mode.name)
            .putBoolean("natural", s.natural)
            .putFloat("sensitivity", s.sensitivity)
            .putString("scrollDistance", s.scrollDistance.name)
            .putInt("repeatMs", s.repeatMs)
            .putInt("autoOffSeconds", s.autoOffSeconds)
            .putInt("targetFps", s.targetFps)
            .putBoolean("showBubble", s.showBubble)
            .putBoolean("vibrate", s.vibrate)
            .putInt("bubbleX", s.bubbleX)
            .putInt("bubbleY", s.bubbleY)
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
