package com.netscope.wifianalyzer.billing

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
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.data.PremiumManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlanInfo(
    val productId: String,
    val formattedPrice: String,
    val priceMicros: Long,
    val currency: String,
    val offerToken: String,
    val freeTrialDays: Int,
    val details: ProductDetails,
)

sealed interface BillingEvent {
    data class Purchased(val productId: String) : BillingEvent
    data object Cancelled : BillingEvent
    data object Pending : BillingEvent
    data class Failed(val message: String) : BillingEvent
    data class Restored(val active: Boolean) : BillingEvent
}

class BillingManager(
    context: Context,
    private val premium: PremiumManager,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _plans = MutableStateFlow<Map<String, PlanInfo>>(emptyMap())
    val plans: StateFlow<Map<String, PlanInfo>> = _plans.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var connecting = false
    private var retryDelay = 1_000L

    fun connect() {
        if (client.isReady || connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryDelay = 1_000L
                    _ready.value = true
                    queryProducts()
                    refreshPurchases()
                } else {
                    scheduleReconnect()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                _ready.value = false
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(60_000L)
            connect()
        }
    }

    private fun queryProducts() {
        val products = listOf(AppConfig.PRODUCT_MONTHLY, AppConfig.PRODUCT_YEARLY).map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val map = detailsResult.productDetailsList.mapNotNull { toPlan(it) }.associateBy { it.productId }
            _plans.value = map
        }
    }

    private fun toPlan(pd: ProductDetails): PlanInfo? {
        val offers = pd.subscriptionOfferDetails ?: return null
        if (offers.isEmpty()) return null
        // Prefer an offer with a free trial (if you created one in Play Console), else the base plan.
        val trialOffer = offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
        val offer = trialOffer ?: offers.first()
        val phases = offer.pricingPhases.pricingPhaseList
        val paid = phases.lastOrNull { it.priceAmountMicros > 0L } ?: phases.last()
        val trialDays = phases.firstOrNull { it.priceAmountMicros == 0L }?.billingPeriod?.let { parseDays(it) } ?: 0
        return PlanInfo(
            productId = pd.productId,
            formattedPrice = paid.formattedPrice,
            priceMicros = paid.priceAmountMicros,
            currency = paid.priceCurrencyCode,
            offerToken = offer.offerToken,
            freeTrialDays = trialDays,
            details = pd,
        )
    }

    /** Parses ISO-8601 periods like P3D, P1W or P1M into days. */
    private fun parseDays(period: String): Int {
        val m = Regex("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(period) ?: return 0
        val (y, mo, w, d) = m.destructured
        return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
    }

    fun refreshPurchases(reportRestore: Boolean = false) {
        if (!client.isReady) {
            connect()
            if (reportRestore) _events.tryEmit(BillingEvent.Failed("Google Play is not available right now. Please try again."))
            return
        }
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (reportRestore) _events.tryEmit(BillingEvent.Failed("Could not reach Google Play. Please try again."))
                return@queryPurchasesAsync
            }
            val active = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            active.forEach { acknowledge(it) }
            premium.setSubscription(active.isNotEmpty(), active.firstOrNull()?.products?.firstOrNull())
            if (reportRestore) _events.tryEmit(BillingEvent.Restored(active.isNotEmpty()))
        }
    }

    fun launch(activity: Activity, productId: String): Boolean {
        val plan = _plans.value[productId]
        if (!client.isReady || plan == null) {
            connect()
            _events.tryEmit(BillingEvent.Failed("Plans are still loading from Google Play. Please try again in a moment."))
            return false
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(plan.details)
                        .setOfferToken(plan.offerToken)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { p ->
                    when (p.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            acknowledge(p)
                            premium.setSubscription(true, p.products.firstOrNull())
                            _events.tryEmit(BillingEvent.Purchased(p.products.firstOrNull().orEmpty()))
                        }
                        Purchase.PurchaseState.PENDING -> _events.tryEmit(BillingEvent.Pending)
                        else -> Unit
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(BillingEvent.Cancelled)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refreshPurchases(reportRestore = true)
            else -> _events.tryEmit(BillingEvent.Failed("Purchase failed (${result.debugMessage.ifBlank { "code ${result.responseCode}" }})."))
        }
    }

    private fun acknowledge(p: Purchase) {
        if (p.isAcknowledged || p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
        client.acknowledgePurchase(params) { }
    }
}
