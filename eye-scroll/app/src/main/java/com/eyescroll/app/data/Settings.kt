package com.eyescroll.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TrackingMode(val label: String, val description: String) {
    EYES_AND_HEAD("Smart", "Eyes + tiny head movement · most accurate"),
    EYES_ONLY("Eyes only", "Only eye movement, keep your head still"),
}

enum class ScrollDistance(val label: String, val fraction: Float) {
    SHORT("Short", 0.22f),
    MEDIUM("Medium", 0.38f),
    LONG("Long", 0.55f),
}

data class AppSettings(
    val onboardingDone: Boolean = false,
    val mode: TrackingMode = TrackingMode.EYES_AND_HEAD,
    /** 0 = needs a strong look to the edge, 1 = reacts to a small glance. */
    val sensitivity: Float = 0.5f,
    /** How long you must keep looking at an edge before a scroll fires. */
    val dwellMs: Int = 450,
    val scrollDistance: ScrollDistance = ScrollDistance.MEDIUM,
    /** Delay between repeated scrolls while you keep looking at the same edge. */
    val repeatMs: Int = 900,
    /** Camera turns off automatically after this many idle seconds; 0 = never. */
    val autoOffSeconds: Int = 60,
    val targetFps: Int = 24,
    val showBubble: Boolean = true,
    val vibrate: Boolean = false,
    val bubbleX: Int = -1,
    val bubbleY: Int = -1,
)

/** Small SharedPreferences-backed store, exposed as a StateFlow so UI and services stay in sync. */
class SettingsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("eyescroll", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _calibration = MutableStateFlow(Calibration.fromJson(prefs.getString(KEY_CALIBRATION, null)))
    val calibration: StateFlow<Calibration?> = _calibration.asStateFlow()

    val current: AppSettings get() = _settings.value

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_settings.value)
        _settings.value = next
        write(next)
    }

    fun saveCalibration(calibration: Calibration?) {
        _calibration.value = calibration
        prefs.edit().putString(KEY_CALIBRATION, calibration?.toJson()).apply()
    }

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboardingDone = prefs.getBoolean("onboardingDone", d.onboardingDone),
            mode = enumOr(prefs.getString("mode", null), d.mode),
            sensitivity = prefs.getFloat("sensitivity", d.sensitivity),
            dwellMs = prefs.getInt("dwellMs", d.dwellMs),
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
            .putFloat("sensitivity", s.sensitivity)
            .putInt("dwellMs", s.dwellMs)
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
        private const val KEY_CALIBRATION = "calibration"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
