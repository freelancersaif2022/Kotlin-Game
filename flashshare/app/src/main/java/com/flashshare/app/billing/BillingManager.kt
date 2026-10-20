package com.flashshare.app.billing

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
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.flashshare.app.BuildConfig
import com.flashshare.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Plan(val productId: String, val title: String, val fallbackPrice: String, val period: String) {
    MONTHLY("flashshare_premium_monthly", "Monthly", "$2.99", "month"),
    YEARLY("flashshare_premium_yearly", "Yearly", "$17.99", "year"),
}

/**
 * Google Play Billing for the monthly / yearly subscriptions.
 * Test-key builds (BuildConfig.MOCK_BILLING) use a local mock store so Premium can be tried before
 * the products exist in Play Console.
 */
class BillingManager(
    private val context: Context,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val isMock: Boolean get() = BuildConfig.MOCK_BILLING

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var retryDelay = 1000L

    fun start() {
        if (isMock) return
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryDelay = 1000L
                    scope.launch {
                        queryProducts()
                        refreshPurchases(userInitiated = false)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                scope.launch {
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(60_000)
                    start()
                }
            }
        })
    }

    private suspend fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            Plan.entries.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it.productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
        ).build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _products.value = result.productDetailsList.orEmpty().associateBy { it.productId }
        }
    }

    /** Recurring price shown on the plan card, e.g. "$2.99". */
    fun priceOf(plan: Plan): String {
        val pd = _products.value[plan.productId] ?: return plan.fallbackPrice
        return pd.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
            ?: plan.fallbackPrice
    }

    private fun recurringMicros(plan: Plan): Long? = _products.value[plan.productId]?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.priceAmountMicros

    /** How much cheaper the yearly plan is than 12 monthly payments, in percent. */
    fun yearlySavingsPercent(): Int {
        val m = recurringMicros(Plan.MONTHLY)
        val y = recurringMicros(Plan.YEARLY)
        if (m == null || y == null || m <= 0) return 50
        return (100 - y * 100 / (m * 12)).toInt().coerceIn(0, 90)
    }

    /** Free-trial text for the plan, if the Play Console offer has one. */
    fun trialOf(plan: Plan): String? {
        val pd = _products.value[plan.productId] ?: return null
        val phase = pd.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull() ?: return null
        if (phase.priceAmountMicros != 0L) return null
        val p = phase.billingPeriod // e.g. P7D, P1W, P1M
        val n = p.filter { it.isDigit() }.toIntOrNull() ?: return null
        val unit = when (p.last()) { 'D' -> "day"; 'W' -> "week"; 'M' -> "month"; else -> return null }
        return "$n-$unit free trial"
    }

    fun purchase(activity: Activity, plan: Plan) {
        if (isMock) {
            scope.launch {
                _busy.value = true
                delay(900)
                prefs.setPremium(true, plan.productId)
                _busy.value = false
                _messages.emit("Premium activated (test store) – welcome aboard!")
            }
            return
        }
        val pd = _products.value[plan.productId]
        val offer = pd?.subscriptionOfferDetails?.firstOrNull()
        if (pd == null || offer == null) {
            start()
            _messages.tryEmit("Google Play isn't ready yet. Check your connection and try again.")
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(pd)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            ).build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { purchases.orEmpty().forEach { handle(it) } }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { refreshPurchases(userInitiated = true) }
            else -> _messages.tryEmit("Purchase failed: ${result.debugMessage.ifEmpty { "code ${result.responseCode}" }}")
        }
    }

    private suspend fun handle(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (p.purchaseState == Purchase.PurchaseState.PENDING) _messages.emit("Payment pending – Premium unlocks once it completes.")
            return
        }
        if (!p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        }
        prefs.setPremium(true, p.products.firstOrNull())
        _messages.emit("Premium activated – enjoy unlimited speed!")
    }

    /** "Restore purchases": re-reads active subscriptions from Google Play. */
    suspend fun refreshPurchases(userInitiated: Boolean) {
        if (isMock) {
            if (userInitiated) _messages.emit(if (prefs.current.premium) "Premium is active" else "No purchases to restore")
            return
        }
        if (!client.isReady) {
            start()
            if (userInitiated) _messages.emit("Connecting to Google Play… try again in a moment")
            return
        }
        _busy.value = userInitiated
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        _busy.value = false
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (userInitiated) _messages.emit("Couldn't reach Google Play")
            return
        }
        val active = result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active.isEmpty()) {
            prefs.setPremium(false, null)
            if (userInitiated) _messages.emit("No active subscription found for this Google account")
        } else {
            active.forEach { handle(it) }
        }
    }

    /** Mock store only – lets testers return to the Free plan. */
    fun mockCancel() {
        if (isMock) prefs.setPremium(false, null)
    }
}
