package com.calorielens.app.premium

import com.calorielens.app.data.prefs.PremiumPlan
import com.calorielens.app.data.prefs.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class PlanOffer(
    val plan: PremiumPlan,
    val price: String,
    val period: String,
    val perMonth: String?,
    val badge: String?,
)

/**
 * Mock billing: simulates the Google Play purchase flow and unlocks Premium locally.
 * Replace [purchase] / [restore] with Google Play Billing calls before publishing.
 */
@Singleton
class PremiumManager @Inject constructor(private val prefs: UserPreferences) {

    val offers = listOf(
        PlanOffer(PremiumPlan.YEARLY, "$29.99", "/ year", "$2.49 / month", "SAVE 58%"),
        PlanOffer(PremiumPlan.MONTHLY, "$5.99", "/ month", null, null),
        PlanOffer(PremiumPlan.LIFETIME, "$59.99", "one-time", null, "BEST DEAL"),
    )

    val isPremium: Flow<Boolean> = prefs.settings.map { it.isPremium }

    suspend fun purchase(plan: PremiumPlan): Result<Unit> {
        delay(1_800)
        prefs.setPremium(plan)
        return Result.success(Unit)
    }

    /** Returns true when an active purchase exists on this device. */
    suspend fun restore(): Boolean {
        delay(1_200)
        return prefs.settings.first().isPremium
    }

    suspend fun cancel() = prefs.setPremium(null)
}
