package com.vaultly.app.billing

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
import com.vaultly.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A purchasable option shown on the paywall. */
data class Plan(
    val key: PlanKey,
    val price: String,
    val details: ProductDetails,
    val offerToken: String? = null,
    /** Monthly equivalent for yearly plans, e.g. "$0.83/mo". */
    val perMonth: String? = null,
)

enum class PlanKey { YEARLY, MONTHLY, LIFETIME, REMOVE_ADS }

/** Google Play Billing: Pro subscription (monthly/yearly), Pro lifetime, and a one-time Remove Ads. */
class BillingManager(context: Context, private val prefs: Prefs) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var retryDelay = 1_000L

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryDelay = 1_000L
                    _connected.value = true
                    scope.launch { loadPlans(); restore(silent = true) }
                }
            }

            override fun onBillingServiceDisconnected() {
                _connected.value = false
                scope.launch {
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(60_000L)
                    connect()
                }
            }
        })
    }

    private suspend fun loadPlans() {
        val out = mutableListOf<Plan>()
        val subs = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                listOf(product(SUB_PRO, BillingClient.ProductType.SUBS)),
            ).build(),
        )
        subs.productDetailsList.orEmpty().forEach { d ->
            d.subscriptionOfferDetails.orEmpty().forEach { offer ->
                // Use the base plan price (last phase) so trials/intro phases don't confuse the label.
                val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@forEach
                when (offer.basePlanId) {
                    BASE_YEARLY -> if (out.none { it.key == PlanKey.YEARLY }) {
                        val monthly = phase.priceAmountMicros / 12 / 1_000_000.0
                        val perMonth = runCatching {
                            java.text.NumberFormat.getCurrencyInstance().apply {
                                currency = java.util.Currency.getInstance(phase.priceCurrencyCode)
                            }.format(monthly) + "/mo"
                        }.getOrNull()
                        out += Plan(PlanKey.YEARLY, phase.formattedPrice, d, offer.offerToken, perMonth)
                    }
                    BASE_MONTHLY -> if (out.none { it.key == PlanKey.MONTHLY }) {
                        out += Plan(PlanKey.MONTHLY, phase.formattedPrice, d, offer.offerToken)
                    }
                }
            }
        }
        val inapp = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                listOf(
                    product(INAPP_LIFETIME, BillingClient.ProductType.INAPP),
                    product(INAPP_REMOVE_ADS, BillingClient.ProductType.INAPP),
                ),
            ).build(),
        )
        inapp.productDetailsList.orEmpty().forEach { d ->
            val price = d.oneTimePurchaseOfferDetails?.formattedPrice ?: return@forEach
            when (d.productId) {
                INAPP_LIFETIME -> out += Plan(PlanKey.LIFETIME, price, d)
                INAPP_REMOVE_ADS -> out += Plan(PlanKey.REMOVE_ADS, price, d)
            }
        }
        _plans.value = out.sortedBy { it.key.ordinal }
    }

    fun buy(activity: Activity, plan: Plan) {
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.details)
            .apply { plan.offerToken?.let { setOfferToken(it) } }
            .build()
        val result = client.launchBillingFlow(
            activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit("Purchase couldn't start. Please try again.")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { purchases.orEmpty().forEach { handle(it, announce = true) } }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restore(silent = false) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _events.tryEmit("Purchase failed (${result.responseCode}). Please try again.")
        }
    }

    private suspend fun handle(purchase: Purchase, announce: Boolean) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (announce && purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                _events.tryEmit("Payment pending. Vaultly will unlock once it completes.")
            }
            return
        }
        grant(purchase.products)
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
            )
        }
        if (announce) {
            _events.tryEmit(if (prefs.isPro) "Welcome to Vaultly Pro ✦" else "Ads removed. Thank you!")
        }
    }

    private fun grant(products: List<String>) {
        if (products.any { it == SUB_PRO || it == INAPP_LIFETIME }) prefs.setPro(true)
        if (products.contains(INAPP_REMOVE_ADS)) prefs.setRemoveAds(true)
    }

    /** Re-reads what the user owns. Entitlements are only revoked when Play answers successfully. */
    suspend fun restore(silent: Boolean) {
        if (!client.isReady) {
            if (!silent) _events.tryEmit("Google Play isn't available right now.")
            connect()
            return
        }
        val subs = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        )
        val inapp = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        val ok = subs.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            inapp.billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (!ok) {
            if (!silent) _events.tryEmit("Couldn't reach Google Play. Try again later.")
            return
        }
        val owned = (subs.purchasesList + inapp.purchasesList)
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val products = owned.flatMap { it.products }
        prefs.setPro(products.any { it == SUB_PRO || it == INAPP_LIFETIME })
        prefs.setRemoveAds(products.contains(INAPP_REMOVE_ADS))
        owned.forEach { handle(it, announce = false) }
        if (!silent) {
            _events.tryEmit(
                when {
                    prefs.isPro -> "Vaultly Pro restored ✦"
                    products.contains(INAPP_REMOVE_ADS) -> "Ad-free restored"
                    else -> "No previous purchases found"
                },
            )
        }
    }

    private fun product(id: String, type: String) =
        QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()

    companion object {
        // Create these in Google Play Console → Monetize.
        const val SUB_PRO = "vaultly_pro"
        const val BASE_MONTHLY = "monthly"
        const val BASE_YEARLY = "yearly"
        const val INAPP_LIFETIME = "vaultly_pro_lifetime"
        const val INAPP_REMOVE_ADS = "vaultly_remove_ads"
    }
}
