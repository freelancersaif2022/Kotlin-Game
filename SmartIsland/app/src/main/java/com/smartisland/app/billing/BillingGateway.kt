package com.smartisland.app.billing

import android.app.Activity
import com.smartisland.app.data.Plan
import com.smartisland.app.data.PremiumRepository
import kotlinx.coroutines.delay

data class SubscriptionProduct(
    val id: String,
    val plan: Plan,
    val title: String,
    val price: String,
    val period: String,
    val pricePerMonth: String,
    val badge: String? = null,
    val trialDays: Int = 0,
)

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}

/**
 * The app only talks to this interface. [MockBillingGateway] is used today; a Play Billing
 * implementation (BillingClient + queryProductDetails + launchBillingFlow) can be dropped in later
 * without touching any screen.
 */
interface BillingGateway {
    val products: List<SubscriptionProduct>
    val isMock: Boolean
    suspend fun purchase(activity: Activity, product: SubscriptionProduct): PurchaseResult
    suspend fun restore(): Boolean
}

class MockBillingGateway(private val premium: PremiumRepository) : BillingGateway {

    override val isMock = true

    override val products = listOf(
        SubscriptionProduct(
            id = "smart_island_yearly",
            plan = Plan.YEARLY,
            title = "Yearly",
            price = "$19.99",
            period = "year",
            pricePerMonth = "$1.67/mo",
            badge = "SAVE 44%",
            trialDays = 3,
        ),
        SubscriptionProduct(
            id = "smart_island_monthly",
            plan = Plan.MONTHLY,
            title = "Monthly",
            price = "$2.99",
            period = "month",
            pricePerMonth = "$2.99/mo",
        ),
    )

    override suspend fun purchase(activity: Activity, product: SubscriptionProduct): PurchaseResult {
        delay(1400) // Simulates the Play purchase sheet round trip.
        val duration = if (product.plan == Plan.YEARLY) PremiumRepository.YEAR_MS else PremiumRepository.MONTH_MS
        premium.grant(product.plan, duration)
        return PurchaseResult.Success
    }

    override suspend fun restore(): Boolean {
        delay(900)
        premium.renewIfNeeded()
        return premium.current.isPremium
    }
}
