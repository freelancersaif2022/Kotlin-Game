package com.novacleaner.app.billing

import android.app.Activity
import android.app.Application
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
import com.novacleaner.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One-time, non-consumable "Remove Ads" lifetime purchase. */
class BillingManager(
    app: Application,
    private val scope: CoroutineScope,
    private val onPremiumChanged: (Boolean) -> Unit,
) : PurchasesUpdatedListener {

    sealed interface Event {
        data object Purchased : Event
        data object Restored : Event
        data object NothingToRestore : Event
        data object Pending : Event
        data class Error(val message: String) : Event
    }

    private val productId = BuildConfig.REMOVE_ADS_PRODUCT_ID
    private val client = BillingClient.newBuilder(app)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _product = MutableStateFlow<ProductDetails?>(null)
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var retry = 0
    private var restoreRequestedByUser = false

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retry = 0
                    queryProduct()
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                scope.launch {
                    delay(minOf(30_000L, 2_000L * (1 shl minOf(retry++, 4))))
                    connect()
                }
            }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val product = details.firstOrNull { it.productId == productId }
                _product.value = product
                _price.value = product?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (restoreRequestedByUser) _events.tryEmit(Event.Error("Couldn't reach Google Play. Try again later."))
                restoreRequestedByUser = false
                return@queryPurchasesAsync
            }
            val owned = purchases.filter { it.products.contains(productId) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            owned.forEach { acknowledge(it) }
            onPremiumChanged(owned.isNotEmpty())
            if (restoreRequestedByUser) {
                _events.tryEmit(if (owned.isNotEmpty()) Event.Restored else Event.NothingToRestore)
            }
            restoreRequestedByUser = false
        }
    }

    fun restore() {
        restoreRequestedByUser = true
        if (client.isReady) queryPurchases() else connect()
    }

    fun purchase(activity: Activity) {
        val product = _product.value
        if (!client.isReady || product == null) {
            _events.tryEmit(Event.Error("Store is not available right now. Please try again in a moment."))
            connect()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { handle(it) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                onPremiumChanged(true)
                _events.tryEmit(Event.Restored)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _events.tryEmit(Event.Error("Purchase failed (${result.debugMessage.ifBlank { result.responseCode.toString() }})"))
        }
    }

    private fun handle(purchase: Purchase) {
        if (!purchase.products.contains(productId)) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                acknowledge(purchase)
                onPremiumChanged(true)
                _events.tryEmit(Event.Purchased)
            }
            Purchase.PurchaseState.PENDING -> _events.tryEmit(Event.Pending)
            else -> Unit
        }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.acknowledgePurchase(params) { }
    }
}
