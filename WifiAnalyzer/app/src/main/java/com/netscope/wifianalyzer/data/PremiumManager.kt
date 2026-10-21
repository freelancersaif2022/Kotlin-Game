package com.netscope.wifianalyzer.data

import com.netscope.wifianalyzer.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PremiumState(
    val subscribed: Boolean,
    val plan: String?,
    val rewardUntil: Long,
    val rewardAdsWatched: Int,
    val now: Long,
) {
    val rewardActive: Boolean get() = rewardUntil > now
    val isPremium: Boolean get() = subscribed || rewardActive
    val rewardRemainingMs: Long get() = (rewardUntil - now).coerceAtLeast(0L)
}

/**
 * Single source of truth for premium access: an active subscription, or a
 * temporary 12 hour unlock earned by watching rewarded ads.
 */
class PremiumManager(private val prefs: AppPrefs, scope: CoroutineScope) {

    private val _state = MutableStateFlow(read())
    val state: StateFlow<PremiumState> = _state.asStateFlow()

    val isPremium: Boolean get() = _state.value.copy(now = System.currentTimeMillis()).isPremium

    /**
     * Only changes when access is gained or lost. Screens gate features on this
     * instead of [state], which ticks every second while a reward unlock is running.
     */
    private val _active = MutableStateFlow(_state.value.isPremium)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val listeners = mutableListOf<(Boolean) -> Unit>()

    init {
        scope.launch {
            while (true) {
                delay(1_000)
                val before = _state.value.isPremium
                val next = _state.value.copy(now = System.currentTimeMillis())
                if (next.rewardActive || before != next.isPremium) _state.value = next
                if (before != next.isPremium) notifyChanged(next.isPremium)
            }
        }
    }

    fun addChangeListener(listener: (Boolean) -> Unit) {
        listeners += listener
    }

    private fun notifyChanged(premium: Boolean) {
        _active.value = premium
        listeners.forEach { it(premium) }
    }

    private fun read() = PremiumState(
        subscribed = prefs.subscribed,
        plan = prefs.subscribedPlan,
        rewardUntil = prefs.rewardUntil,
        rewardAdsWatched = prefs.rewardAdsWatched,
        now = System.currentTimeMillis(),
    )

    private fun publish() {
        val before = _state.value.isPremium
        _state.value = read()
        if (before != _state.value.isPremium) notifyChanged(_state.value.isPremium)
    }

    fun setSubscription(active: Boolean, plan: String?) {
        if (prefs.subscribed == active && prefs.subscribedPlan == plan) return
        prefs.subscribed = active
        prefs.subscribedPlan = if (active) plan else null
        publish()
    }

    /** Records one completed rewarded ad. Returns true when this ad unlocked premium. */
    fun onRewardEarned(): Boolean {
        val count = prefs.rewardAdsWatched + 1
        return if (count >= AppConfig.REWARD_ADS_REQUIRED) {
            prefs.rewardAdsWatched = 0
            val base = maxOf(System.currentTimeMillis(), prefs.rewardUntil)
            prefs.rewardUntil = base + AppConfig.REWARD_DURATION_MS
            publish()
            true
        } else {
            prefs.rewardAdsWatched = count
            publish()
            false
        }
    }
}
