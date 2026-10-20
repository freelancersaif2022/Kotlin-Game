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
import com.vaultly.app.BuildConfig
import com.vaultly.app.Notifier
import com.vaultly.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A purchasable option. [details] is null for test-store plans. */
data class Plan(
    val key: PlanKey,
    val price: String,
    val details: ProductDetails?,
    val offerToken: String? = null,
    /** Monthly equivalent for yearly plans, e.g. "$1.67/mo". */
    val perMonth: String? = null,
    /** e.g. "Save 44%" on the yearly plan. */
    val saving: String? = null,
) {
    val isMock: Boolean get() = details == null
}

enum class PlanKey { YEARLY, MONTHLY, LIFETIME, REMOVE_ADS }

/**
 * Google Play Billing: Pro subscription (monthly/yearly), Pro lifetime, and a one-time Remove Ads.
 *
 * Test builds (signed with the public test key, see build.gradle.kts) fall back to a mock store
 * when Play has no products for the app, so the full purchase flow can be tried on any phone.
 * Release builds signed with a real upload key never use the mock store.
 */
class BillingManager(context: Context, private val prefs: Prefs, private val notifier: Notifier) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val mockEnabled: Boolean = BuildConfig.MOCK_BILLING

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _plans = MutableStateFlow(if (mockEnabled) MOCK_PLANS else emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _purchasing = MutableStateFlow(false)
    val purchasing: StateFlow<Boolean> = _purchasing.asStateFlow()

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
        var monthlyMicros = 0L
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
                        monthlyMicros = phase.priceAmountMicros
                        out += Plan(PlanKey.MONTHLY, phase.formattedPrice, d, offer.offerToken)
                    }
                }
            }
        }
        if (monthlyMicros > 0) {
            val yearly = out.firstOrNull { it.key == PlanKey.YEARLY }
            val yearlyMicros = yearly?.details?.subscriptionOfferDetails
                ?.firstOrNull { it.basePlanId == BASE_YEARLY }?.pricingPhases?.pricingPhaseList?.lastOrNull()?.priceAmountMicros
            if (yearly != null && yearlyMicros != null) {
                val saving = (100 - yearlyMicros * 100 / (monthlyMicros * 12)).toInt()
                if (saving > 0) out[out.indexOf(yearly)] = yearly.copy(saving = "Save $saving%")
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
        if (out.isNotEmpty()) _plans.value = out.sortedBy { it.key.ordinal }
        else if (!mockEnabled) _plans.value = emptyList()
    }

    fun buy(activity: Activity, plan: Plan) {
        if (_purchasing.value) return
        if (plan.isMock) {
            scope.launch {
                _purchasing.value = true
                delay(1400) // feels like a real checkout
                if (plan.key == PlanKey.REMOVE_ADS) {
                    prefs.setRemoveAds(true)
                    notifier.success("Ads removed. Thank you!")
                } else {
                    prefs.setMockPro(true)
                }
                _purchasing.value = false
            }
            return
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.details!!)
            .apply { plan.offerToken?.let { setOfferToken(it) } }
            .build()
        val result = client.launchBillingFlow(
            activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            notifier.error("The purchase couldn't start. Please try again.")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { purchases.orEmpty().forEach { handle(it, announce = true) } }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restore(silent = false) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> notifier.error("Purchase failed. Please try again.")
        }
    }

    private suspend fun handle(purchase: Purchase, announce: Boolean) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (announce && purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                notifier.show("Payment pending. Pro unlocks as soon as it completes.")
            }
            return
        }
        grant(purchase.products)
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
            )
        }
        if (announce && !prefs.isPro) notifier.success("Ads removed. Thank you!")
    }

    private fun grant(products: List<String>) {
        if (products.any { it == SUB_PRO || it == INAPP_LIFETIME }) prefs.setPro(true)
        if (products.contains(INAPP_REMOVE_ADS)) prefs.setRemoveAds(true)
    }

    /** Re-reads what the user owns. Entitlements are only revoked when Play answers successfully. */
    suspend fun restore(silent: Boolean) {
        if (!client.isReady) {
            connect()
            if (!silent) {
                when {
                    prefs.mockPro.value -> notifier.success("Vaultly Pro is active on this device")
                    mockEnabled -> notifier.show("No previous purchases found")
                    else -> notifier.error("Google Play isn't reachable right now. Try again in a moment.")
                }
            }
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
            if (!silent) notifier.error("Couldn't reach Google Play. Try again in a moment.")
            return
        }
        val owned = (subs.purchasesList + inapp.purchasesList)
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val products = owned.flatMap { it.products }
        prefs.setPro(products.any { it == SUB_PRO || it == INAPP_LIFETIME })
        if (products.contains(INAPP_REMOVE_ADS)) prefs.setRemoveAds(true)
        owned.forEach { handle(it, announce = false) }
        if (!silent) {
            when {
                prefs.isPro -> notifier.success("Vaultly Pro restored. Welcome back!")
                products.contains(INAPP_REMOVE_ADS) -> notifier.success("Ad-free purchase restored")
                else -> notifier.show("No previous purchases found for this Google account")
            }
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

        private val MOCK_PLANS = listOf(
            Plan(PlanKey.YEARLY, "$19.99", null, perMonth = "$1.67/mo", saving = "Save 44%"),
            Plan(PlanKey.MONTHLY, "$2.99", null),
            Plan(PlanKey.REMOVE_ADS, "$3.99", null),
        )
    }
}
