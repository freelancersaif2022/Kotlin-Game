package com.pdfmaster.app.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Immutable
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
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.pdfmaster.app.di.AppScope
import com.pdfmaster.app.premium.PremiumManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class PlanId(val productId: String, val isSubscription: Boolean) {
    MONTHLY("pdfmaster_premium_monthly", true),
    YEARLY("pdfmaster_premium_yearly", true),
    LIFETIME("pdfmaster_premium_lifetime", false),
}

@Immutable
data class Plan(
    val id: PlanId,
    val title: String,
    val price: String,
    val period: String,
    val note: String,
    val badge: String? = null,
    val hasTrial: Boolean = false,
    val available: Boolean = false,
)

sealed interface BillingEvent {
    data object Success : BillingEvent
    data object Restored : BillingEvent
    data object NothingToRestore : BillingEvent
    data object Pending : BillingEvent
    data object Cancelled : BillingEvent
    data class Error(val message: String) : BillingEvent
}

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val premium: PremiumManager,
    @AppScope private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val defaults = listOf(
        Plan(PlanId.MONTHLY, "Monthly", "$2.99", "/month", "Cancel anytime"),
        Plan(PlanId.YEARLY, "Yearly", "$14.99", "/year", "3-day free trial, then $14.99/year", badge = "SAVE 58%", hasTrial = true),
        Plan(PlanId.LIFETIME, "Lifetime", "$29.99", "once", "Pay once, own it forever"),
    )

    private val _plans = MutableStateFlow(defaults)
    val plans: StateFlow<List<Plan>> = _plans

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BillingEvent> = _events

    private val details = HashMap<PlanId, ProductDetails>()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var retryDelay = 1000L

    fun connect() {
        if (client.isReady || client.connectionState == BillingClient.ConnectionState.CONNECTING) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryDelay = 1000L
                    scope.launch {
                        loadProducts()
                        syncPurchases(userInitiated = false)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                scope.launch {
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(60_000L)
                    connect()
                }
            }
        })
    }

    private suspend fun loadProducts() {
        val subs = QueryProductDetailsParams.newBuilder().setProductList(
            PlanId.entries.filter { it.isSubscription }.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it.productId).setProductType(BillingClient.ProductType.SUBS).build()
            },
        ).build()
        val inapp = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(PlanId.LIFETIME.productId)
                    .setProductType(BillingClient.ProductType.INAPP).build(),
            ),
        ).build()
        val found = (client.queryProductDetails(subs).productDetailsList.orEmpty() +
            client.queryProductDetails(inapp).productDetailsList.orEmpty())
        found.forEach { pd -> PlanId.entries.firstOrNull { it.productId == pd.productId }?.let { details[it] = pd } }
        _plans.value = defaults.map { plan ->
            val pd = details[plan.id] ?: return@map plan
            if (plan.id.isSubscription) {
                val offer = pd.subscriptionOfferDetails?.let { offers -> offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } } ?: offers.firstOrNull() }
                val phases = offer?.pricingPhases?.pricingPhaseList.orEmpty()
                val paid = phases.lastOrNull { it.priceAmountMicros > 0 }
                val trial = phases.any { it.priceAmountMicros == 0L }
                plan.copy(
                    price = paid?.formattedPrice ?: plan.price,
                    hasTrial = trial,
                    note = if (trial) "Free trial, then ${paid?.formattedPrice ?: plan.price}${plan.period}" else plan.note,
                    available = offer != null,
                )
            } else {
                plan.copy(price = pd.oneTimePurchaseOfferDetails?.formattedPrice ?: plan.price, available = true)
            }
        }
    }

    fun purchase(activity: Activity, planId: PlanId) {
        val pd = details[planId]
        if (!client.isReady || pd == null) {
            connect()
            _events.tryEmit(BillingEvent.Error("Google Play Store isn't available right now. Please try again in a moment."))
            return
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).apply {
            if (planId.isSubscription) {
                val offers = pd.subscriptionOfferDetails.orEmpty()
                val offer = offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } } ?: offers.firstOrNull()
                offer?.let { setOfferToken(it.offerToken) }
            }
        }.build()
        val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(BillingEvent.Error(friendly(result)))
        }
    }

    fun restore() {
        if (!client.isReady) {
            connect()
            _events.tryEmit(BillingEvent.Error("Couldn't reach Google Play. Check your connection and try again."))
            return
        }
        scope.launch { syncPurchases(userInitiated = true) }
    }

    private suspend fun syncPurchases(userInitiated: Boolean) {
        val subs = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        val inapp = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        val ok = subs.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            inapp.billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (!ok) {
            if (userInitiated) _events.emit(BillingEvent.Error("Couldn't check your purchases. Please try again."))
            return
        }
        val owned = (subs.purchasesList + inapp.purchasesList).filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        owned.forEach { acknowledge(it) }
        premium.setPurchased(owned.isNotEmpty())
        if (userInitiated) _events.emit(if (owned.isNotEmpty()) BillingEvent.Restored else BillingEvent.NothingToRestore)
    }

    private suspend fun acknowledge(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                val list = purchases.orEmpty()
                val done = list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                done.forEach { acknowledge(it) }
                if (done.isNotEmpty()) {
                    premium.setPurchased(true)
                    _events.emit(BillingEvent.Success)
                } else if (list.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    _events.emit(BillingEvent.Pending)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(BillingEvent.Cancelled)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { syncPurchases(userInitiated = true) }
            else -> _events.tryEmit(BillingEvent.Error(friendly(result)))
        }
    }

    private fun friendly(result: BillingResult): String = when (result.responseCode) {
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.NETWORK_ERROR -> "Network problem. Check your connection and try again."
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Purchases aren't available on this device or account."
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "This plan isn't available in your country yet."
        else -> "The purchase couldn't be completed. Please try again."
    }
}
