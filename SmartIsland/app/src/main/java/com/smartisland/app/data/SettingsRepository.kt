package com.smartisland.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed settings exposed as a [StateFlow]. Reads are synchronous and cheap, so the
 * overlay service and the UI can both observe the same instance with zero startup latency.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("island_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<IslandSettings> = _settings.asStateFlow()

    private val main = Handler(Looper.getMainLooper())

    /** Slider drags update many times per second; disk writes are batched to one per pause. */
    private val persist = Runnable { write(_settings.value) }

    val current: IslandSettings get() = _settings.value

    fun update(transform: (IslandSettings) -> IslandSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        main.removeCallbacks(persist)
        main.postDelayed(persist, WRITE_DELAY_MS)
    }

    /** Writes any pending change now; called when the app goes to the background. */
    fun flush() {
        main.removeCallbacks(persist)
        write(_settings.value)
    }

    fun resetGeometry() = update {
        val d = IslandSettings()
        it.copy(
            widthDp = d.widthDp, heightDp = d.heightDp, topOffsetDp = d.topOffsetDp,
            xOffsetDp = d.xOffsetDp, expandedWidthDp = d.expandedWidthDp, roundness = d.roundness,
        )
    }

    private fun read(): IslandSettings {
        val d = IslandSettings()
        return IslandSettings(
            enabled = prefs.getBoolean(K_ENABLED, d.enabled),
            widthDp = prefs.getFloat(K_WIDTH, d.widthDp),
            heightDp = prefs.getFloat(K_HEIGHT, d.heightDp),
            topOffsetDp = prefs.getFloat(K_TOP, d.topOffsetDp),
            xOffsetDp = prefs.getFloat(K_X, d.xOffsetDp),
            expandedWidthDp = prefs.getFloat(K_EXPANDED, d.expandedWidthDp),
            roundness = prefs.getFloat(K_ROUND, d.roundness),
            showMedia = prefs.getBoolean(K_MEDIA, d.showMedia),
            showCalls = prefs.getBoolean(K_CALLS, d.showCalls),
            showCharging = prefs.getBoolean(K_CHARGING, d.showCharging),
            showNotifications = prefs.getBoolean(K_NOTIFS, d.showNotifications),
            showRinger = prefs.getBoolean(K_RINGER, d.showRinger),
            replaceVolumePanel = prefs.getBoolean(K_VOLUME, d.replaceVolumePanel),
            hideInLandscape = prefs.getBoolean(K_LANDSCAPE, d.hideInLandscape),
            haptics = prefs.getBoolean(K_HAPTICS, d.haptics),
            glow = prefs.getBoolean(K_GLOW, d.glow),
            accentColor = prefs.getInt(K_ACCENT, d.accentColor),
            hideWhenIdle = prefs.getBoolean(K_HIDE_IDLE, d.hideWhenIdle),
            outline = prefs.getInt(K_OUTLINE, d.outline),
            outlineColor = prefs.getInt(K_OUTLINE_COLOR, d.outlineColor),
            edgePanelEnabled = prefs.getBoolean(K_EDGE, d.edgePanelEnabled),
            edgePanelOffsetDp = prefs.getFloat(K_EDGE_OFFSET, d.edgePanelOffsetDp),
            edgePanelRight = prefs.getBoolean(K_EDGE_RIGHT, d.edgePanelRight),
            onboardingDone = prefs.getBoolean(K_ONBOARDED, d.onboardingDone),
            darkMode = prefs.getBoolean(K_DARK, d.darkMode),
        )
    }

    private fun write(s: IslandSettings) {
        prefs.edit()
            .putBoolean(K_ENABLED, s.enabled)
            .putFloat(K_WIDTH, s.widthDp)
            .putFloat(K_HEIGHT, s.heightDp)
            .putFloat(K_TOP, s.topOffsetDp)
            .putFloat(K_X, s.xOffsetDp)
            .putFloat(K_EXPANDED, s.expandedWidthDp)
            .putFloat(K_ROUND, s.roundness)
            .putBoolean(K_MEDIA, s.showMedia)
            .putBoolean(K_CALLS, s.showCalls)
            .putBoolean(K_CHARGING, s.showCharging)
            .putBoolean(K_NOTIFS, s.showNotifications)
            .putBoolean(K_RINGER, s.showRinger)
            .putBoolean(K_VOLUME, s.replaceVolumePanel)
            .putBoolean(K_LANDSCAPE, s.hideInLandscape)
            .putBoolean(K_HAPTICS, s.haptics)
            .putBoolean(K_GLOW, s.glow)
            .putInt(K_ACCENT, s.accentColor)
            .putBoolean(K_HIDE_IDLE, s.hideWhenIdle)
            .putInt(K_OUTLINE, s.outline)
            .putInt(K_OUTLINE_COLOR, s.outlineColor)
            .putBoolean(K_EDGE, s.edgePanelEnabled)
            .putFloat(K_EDGE_OFFSET, s.edgePanelOffsetDp)
            .putBoolean(K_EDGE_RIGHT, s.edgePanelRight)
            .putBoolean(K_ONBOARDED, s.onboardingDone)
            .putBoolean(K_DARK, s.darkMode)
            .apply()
    }

    private companion object {
        const val K_ENABLED = "enabled"
        const val K_WIDTH = "width"
        const val K_HEIGHT = "height"
        const val K_TOP = "top"
        const val K_X = "x"
        const val K_EXPANDED = "expanded_width"
        const val K_ROUND = "roundness"
        const val K_MEDIA = "show_media"
        const val K_CALLS = "show_calls"
        const val K_CHARGING = "show_charging"
        const val K_NOTIFS = "show_notifications"
        const val K_RINGER = "show_ringer"
        const val K_VOLUME = "replace_volume"
        const val K_LANDSCAPE = "hide_landscape"
        const val K_HAPTICS = "haptics"
        const val K_GLOW = "glow"
        const val K_ACCENT = "accent"
        const val K_HIDE_IDLE = "hide_when_idle"
        const val K_OUTLINE = "outline"
        const val K_OUTLINE_COLOR = "outline_color"
        const val K_EDGE = "edge_panel"
        const val K_EDGE_OFFSET = "edge_offset"
        const val K_EDGE_RIGHT = "edge_right"
        const val K_ONBOARDED = "onboarding_done"
        const val K_DARK = "dark_mode"
        const val WRITE_DELAY_MS = 250L
    }
}
