package com.netscope.wifianalyzer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class SpeedUnit(val label: String) { MBPS("Mbps"), MBYTES("MB/s") }

class AppPrefs(context: Context) {
    val sp: SharedPreferences = context.getSharedPreferences("wifi_analyzer_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(ThemeMode.entries[sp.getInt(K_THEME, 0).coerceIn(0, 2)])
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _unit = MutableStateFlow(SpeedUnit.entries[sp.getInt(K_UNIT, 0).coerceIn(0, 1)])
    val unit: StateFlow<SpeedUnit> = _unit.asStateFlow()

    private val _soundGuide = MutableStateFlow(sp.getBoolean(K_SOUND, false))
    val soundGuide: StateFlow<Boolean> = _soundGuide.asStateFlow()

    private val _intruderEnabled = MutableStateFlow(sp.getBoolean(K_INTRUDER, false))
    val intruderEnabled: StateFlow<Boolean> = _intruderEnabled.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        sp.edit { putInt(K_THEME, mode.ordinal) }
    }

    fun setUnit(unit: SpeedUnit) {
        _unit.value = unit
        sp.edit { putInt(K_UNIT, unit.ordinal) }
    }

    fun setSoundGuide(enabled: Boolean) {
        _soundGuide.value = enabled
        sp.edit { putBoolean(K_SOUND, enabled) }
    }

    fun setIntruderEnabled(enabled: Boolean) {
        _intruderEnabled.value = enabled
        sp.edit { putBoolean(K_INTRUDER, enabled) }
    }

    var onboardingDone: Boolean
        get() = sp.getBoolean(K_ONBOARDING, false)
        set(v) = sp.edit { putBoolean(K_ONBOARDING, v) }

    var permissionsDone: Boolean
        get() = sp.getBoolean(K_PERMISSIONS, false)
        set(v) = sp.edit { putBoolean(K_PERMISSIONS, v) }

    var homeTourDone: Boolean
        get() = sp.getBoolean(K_TOUR, false)
        set(v) = sp.edit { putBoolean(K_TOUR, v) }

    var completedTests: Int
        get() = sp.getInt(K_TESTS, 0)
        set(v) = sp.edit { putInt(K_TESTS, v) }

    var completedScans: Int
        get() = sp.getInt(K_SCANS, 0)
        set(v) = sp.edit { putInt(K_SCANS, v) }

    var reviewRequested: Boolean
        get() = sp.getBoolean(K_REVIEW, false)
        set(v) = sp.edit { putBoolean(K_REVIEW, v) }

    var subscribed: Boolean
        get() = sp.getBoolean(K_SUBSCRIBED, false)
        set(v) = sp.edit { putBoolean(K_SUBSCRIBED, v) }

    var subscribedPlan: String?
        get() = sp.getString(K_PLAN, null)
        set(v) = sp.edit { putString(K_PLAN, v) }

    var rewardUntil: Long
        get() = sp.getLong(K_REWARD_UNTIL, 0L)
        set(v) = sp.edit { putLong(K_REWARD_UNTIL, v) }

    var rewardAdsWatched: Int
        get() = sp.getInt(K_REWARD_COUNT, 0)
        set(v) = sp.edit { putInt(K_REWARD_COUNT, v) }

    var lastIntruderCheck: Long
        get() = sp.getLong(K_INTRUDER_CHECK, 0L)
        set(v) = sp.edit { putLong(K_INTRUDER_CHECK, v) }

    private companion object {
        const val K_THEME = "theme"
        const val K_UNIT = "unit"
        const val K_SOUND = "sound_guide"
        const val K_INTRUDER = "intruder_enabled"
        const val K_ONBOARDING = "onboarding_done"
        const val K_PERMISSIONS = "permissions_done"
        const val K_TOUR = "home_tour_done"
        const val K_TESTS = "completed_tests"
        const val K_SCANS = "completed_scans"
        const val K_REVIEW = "review_requested"
        const val K_SUBSCRIBED = "subscribed"
        const val K_PLAN = "subscribed_plan"
        const val K_REWARD_UNTIL = "reward_until"
        const val K_REWARD_COUNT = "reward_ads_watched"
        const val K_INTRUDER_CHECK = "last_intruder_check"
    }
}
