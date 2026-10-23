package com.voxcast.mic.data

import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Mock subscription billing: "VoxCast Premium" at $1.99 / month.
 *
 * Replace the bodies of [subscribe], [restore] and [cancel] with Google Play Billing
 * (BillingClient + a "premium_monthly" subscription product) before publishing.
 * The rest of the app only reads [PremiumState.isEntitled], so nothing else changes.
 */
class PremiumManager(private val settings: SettingsRepository) {

    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    suspend fun subscribe(): Result {
        delay(1_800) // simulated store round-trip
        val now = System.currentTimeMillis()
        settings.setPremium(
            PremiumState(
                active = true,
                autoRenew = true,
                renewsAt = now + PERIOD_MS,
                purchasedAt = now,
            )
        )
        return Result.Success
    }

    suspend fun restore(): Result {
        delay(1_200)
        val current = settings.settings.value.premium
        return if (current.isEntitled) Result.Success
        else Result.Failure("No active subscription found for this account")
    }

    /** Turns off auto-renew; premium stays active until the end of the paid month. */
    suspend fun cancel() {
        delay(800)
        val current = settings.settings.value.premium
        settings.setPremium(current.copy(autoRenew = false))
    }

    companion object {
        const val PRICE = "$1.99"
        const val PERIOD_LABEL = "month"
        val PERIOD_MS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
