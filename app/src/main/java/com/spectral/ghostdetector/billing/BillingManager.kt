package com.spectral.ghostdetector.billing

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-time "Remove Ads / Premium" purchase via Google Play Billing.
 * Create an in-app product with id [PRODUCT_ID] in Play Console.
 */
class BillingManager(
    context: Context,
    private val onPremiumGranted: () -> Unit,
) : PurchasesUpdatedListener {

    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    restore(silent = true)
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { _, list ->
            details = list.firstOrNull()
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    fun purchase(activity: Activity) {
        val d = details
        if (!client.isReady || d == null) {
            _message.value = "Store is not available right now. Check your connection and try again."
            connect()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    fun restore(silent: Boolean = false) {
        if (!client.isReady) {
            if (!silent) _message.value = "Connecting to Google Play…"
            connect()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { _, purchases ->
            val owned = purchases.filter {
                PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            owned.forEach { handle(it, silent) }
            if (!silent) {
                _message.value = if (owned.isNotEmpty()) "Premium restored. Thank you!" else "No previous purchase found."
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handle(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> _message.value = "Purchase cancelled."
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            else -> _message.value = "Purchase failed. Please try again."
        }
    }

    private fun handle(purchase: Purchase, silent: Boolean = false) {
        if (PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                onPremiumGranted()
                if (!silent) _message.value = "Premium unlocked. Ads removed!"
                if (!purchase.isAcknowledged) {
                    val ack = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    client.acknowledgePurchase(ack) { }
                }
            }
            Purchase.PurchaseState.PENDING -> _message.value = "Payment pending. Premium unlocks once it completes."
            else -> Unit
        }
    }

    companion object {
        const val PRODUCT_ID = "premium_remove_ads"
    }
}
