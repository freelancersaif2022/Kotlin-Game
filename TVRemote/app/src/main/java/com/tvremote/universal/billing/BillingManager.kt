package com.tvremote.universal.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Plan(val productId: String, val type: String) {
    MONTHLY("premium_monthly", BillingClient.ProductType.SUBS),
    YEARLY("premium_yearly", BillingClient.ProductType.SUBS),
    LIFETIME("premium_lifetime", BillingClient.ProductType.INAPP),
}

data class PlanOffer(
    val plan: Plan,
    val price: String,
    val hasFreeTrial: Boolean,
    val trialText: String?,
)

/**
 * Google Play Billing. Products to create in Play Console:
 *  - Subscriptions: premium_monthly (base plan "monthly"), premium_yearly (base plan "yearly", optional free-trial offer)
 *  - One-time product: premium_lifetime
 */
class BillingManager(context: Context, private val scope: CoroutineScope) : PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val details = MutableStateFlow<Map<Plan, ProductDetails>>(emptyMap())
    private val _offers = MutableStateFlow<Map<Plan, PlanOffer>>(emptyMap())
    val offers: StateFlow<Map<Plan, PlanOffer>> = _offers.asStateFlow()

    /** null until Google Play has answered at least once. */
    private val _owned = MutableStateFlow<Boolean?>(null)
    val owned: StateFlow<Boolean?> = _owned.asStateFlow()

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    private var retries = 0

    fun start() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retries = 0
                    queryProducts()
                    refreshPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                if (retries++ < 5) scope.launch { delay(2000L * retries); start() }
            }
        })
    }

    fun consumeEvent() { _events.value = null }

    private fun queryProducts() {
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            val products = Plan.entries.filter { it.type == type }.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it.productId).setProductType(type).build()
            }
            val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
            client.queryProductDetailsAsync(params) { result, queryResult ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val list = queryResult.productDetailsList
                details.update { current ->
                    current + list.mapNotNull { pd -> Plan.entries.firstOrNull { it.productId == pd.productId }?.let { it to pd } }
                }
                _offers.value = details.value.mapValues { (plan, pd) -> toOffer(plan, pd) }.filterValues { it != null }
                    .mapValues { it.value!! }
            }
        }
    }

    private fun toOffer(plan: Plan, pd: ProductDetails): PlanOffer? {
        if (plan.type == BillingClient.ProductType.INAPP) {
            @Suppress("DEPRECATION")
            val price = pd.oneTimePurchaseOfferDetails?.formattedPrice ?: return null
            return PlanOffer(plan, price, false, null)
        }
        val offers = pd.subscriptionOfferDetails ?: return null
        val chosen = pickOffer(pd) ?: return null
        val phases = chosen.pricingPhases.pricingPhaseList
        val recurring = phases.lastOrNull()?.formattedPrice ?: return null
        val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
        val trialText = trial?.billingPeriod?.let { period ->
            val days = Regex("P(\\d+)D").find(period)?.groupValues?.get(1)
            val weeks = Regex("P(\\d+)W").find(period)?.groupValues?.get(1)
            when {
                days != null -> "$days-day free trial"
                weeks != null -> "${weeks.toInt() * 7}-day free trial"
                else -> "Free trial"
            }
        }
        if (offers.isEmpty()) return null
        return PlanOffer(plan, recurring, trial != null, trialText)
    }

    /** Prefer an offer with a free trial, otherwise the base plan. */
    private fun pickOffer(pd: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        val offers = pd.subscriptionOfferDetails ?: return null
        return offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
            ?: offers.firstOrNull { it.offerId == null }
            ?: offers.firstOrNull()
    }

    fun refreshPurchases() {
        if (!client.isReady) { start(); return }
        var subsOwned: Boolean? = null
        var inappOwned: Boolean? = null
        fun publish() {
            val s = subsOwned
            val i = inappOwned
            if (s != null && i != null) _owned.value = s || i
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.forEach { acknowledge(it) }
            subsOwned = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            publish()
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.forEach { acknowledge(it) }
            inappOwned = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            publish()
        }
    }

    /** Returns false if the product is not available (e.g. store not reachable). */
    fun launch(activity: Activity, plan: Plan): Boolean {
        val pd = details.value[plan] ?: run { start(); return false }
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd)
        if (plan.type == BillingClient.ProductType.SUBS) {
            val offer = pickOffer(pd) ?: return false
            builder.setOfferToken(offer.offerToken)
        }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(builder.build())).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { acknowledge(it) }
                if (purchases.orEmpty().any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                    _owned.value = true
                    _events.value = "Welcome to Premium! All features are unlocked."
                } else if (purchases.orEmpty().any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    _events.value = "Your payment is pending. Premium unlocks as soon as it completes."
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                refreshPurchases()
                _events.value = "You already own Premium. Restoring…"
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _events.value = "Purchase failed. Please try again."
        }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.acknowledgePurchase(params) { }
    }
}
