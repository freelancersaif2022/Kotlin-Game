package com.smartisland.app.island

import com.smartisland.app.data.IslandSettings
import com.smartisland.app.data.PremiumState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Media transport controls exposed by whoever owns the active media session. */
interface MediaCommands {
    fun playPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun open()
}

/**
 * Single source of truth for island content. Producers (notification listener, accessibility service,
 * the app's demo buttons) push raw state here; the overlay renders [current].
 *
 * Priority: call > transient event (volume, charging, notice...) > media > idle.
 */
object IslandEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val media = MutableStateFlow<IslandActivity.Media?>(null)
    private val call = MutableStateFlow<IslandActivity.Call?>(null)
    private val transient = MutableStateFlow<IslandActivity?>(null)
    private var transientJob: Job? = null

    private val settings = MutableStateFlow(IslandSettings())
    private val premium = MutableStateFlow(PremiumState())

    @Volatile
    var mediaCommands: MediaCommands? = null

    /** Latest media state regardless of what the island shows (used by the media widget). */
    val mediaState: StateFlow<IslandActivity.Media?> = media.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    val current: StateFlow<IslandActivity> =
        combine(call, transient, media, settings, premium) { c, t, m, s, p ->
            when {
                c != null && s.showCalls -> c
                t != null && allowed(t, s, p) -> t
                m != null && s.showMedia -> m
                else -> IslandActivity.Idle
            }
        }.stateIn(scope, SharingStarted.Eagerly, IslandActivity.Idle)

    fun bindConfig(settings: StateFlow<IslandSettings>, premium: StateFlow<PremiumState>) {
        scope.launch { settings.collect { this@IslandEngine.settings.value = it } }
        scope.launch { premium.collect { this@IslandEngine.premium.value = it } }
    }

    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    fun setMedia(value: IslandActivity.Media?) {
        media.value = value
    }

    fun setCall(value: IslandActivity.Call?) {
        call.value = value
    }

    fun clearCall(key: String) {
        if (call.value?.key == key) call.value = null
    }

    /** Shows [activity] for [durationMs], replacing any transient event already on screen. */
    fun showTransient(activity: IslandActivity, durationMs: Long = 2600L) {
        transientJob?.cancel()
        transient.value = activity
        transientJob = scope.launch {
            delay(durationMs)
            if (transient.value === activity) transient.value = null
        }
    }

    fun dismissTransient() {
        transientJob?.cancel()
        transient.value = null
    }

    /** Every [com.smartisland.app.data.PremiumFeature] is currently subscription-only. */
    val isPremium: Boolean get() = premium.value.isPremium

    private fun allowed(a: IslandActivity, s: IslandSettings, p: PremiumState): Boolean = when (a) {
        is IslandActivity.Charging, is IslandActivity.LowBattery -> s.showCharging
        is IslandActivity.Ringer -> s.showRinger
        is IslandActivity.Notice -> s.showNotifications && p.isPremium
        else -> true
    }
}
