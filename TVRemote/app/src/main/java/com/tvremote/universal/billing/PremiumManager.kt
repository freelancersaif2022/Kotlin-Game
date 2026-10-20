package com.tvremote.universal.billing

import com.tvremote.universal.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Premium = an active purchase OR the 24 hour pass earned by watching 3 rewarded ads. */
class PremiumManager(
    private val settings: SettingsStore,
    private val billing: BillingManager,
    scope: CoroutineScope,
) {
    private val now = MutableStateFlow(System.currentTimeMillis())

    private val _isPremium = MutableStateFlow(computePremium(settings.purchasedCache.value, settings.rewardUntil.value))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    val purchased: StateFlow<Boolean> get() = settings.purchasedCache
    val rewardProgress: StateFlow<Int> get() = settings.rewardProgress
    val rewardUntil: StateFlow<Long> get() = settings.rewardUntil
    val plan: StateFlow<Plan?> get() = billing.ownedPlan

    /** True while the free 24 h pass is running (and no purchase). */
    private val _passActive = MutableStateFlow(false)
    val passActive: StateFlow<Boolean> = _passActive.asStateFlow()

    init {
        scope.launch {
            billing.owned.collect { owned ->
                if (owned != null) {
                    settings.setPurchased(owned)
                    // A real purchase supersedes the ad pass: start the ad counter fresh.
                    if (owned) settings.setReward(0, 0L)
                }
            }
        }
        scope.launch {
            combine(settings.purchasedCache, settings.rewardUntil, now) { p, until, t -> Triple(p, until, t) }
                .collect { (p, until, t) ->
                    _isPremium.value = p || until > t
                    _passActive.value = !p && until > t
                    // When the 24 h pass ends, the ad counter resets for the next pass.
                    if (until in 1..t) settings.setReward(0, 0L)
                }
        }
        scope.launch {
            while (true) {
                delay(30_000)
                now.value = System.currentTimeMillis()
            }
        }
    }

    private fun computePremium(purchased: Boolean, until: Long) = purchased || until > System.currentTimeMillis()

    /** Called after each completed rewarded ad. Returns true when the 24 h pass was granted. */
    fun onRewardEarned(): Boolean {
        val progress = settings.rewardProgress.value + 1
        return if (progress >= REQUIRED_ADS) {
            settings.setReward(0, System.currentTimeMillis() + PASS_DURATION_MS)
            now.value = System.currentTimeMillis()
            true
        } else {
            settings.setReward(progress, settings.rewardUntil.value)
            false
        }
    }

    companion object {
        const val REQUIRED_ADS = 3
        const val PASS_DURATION_MS = 24L * 60 * 60 * 1000
    }
}
