package com.dailychallenge.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import timber.log.Timber

class BillingManager(
    context: Context,
    private val onPremiumGranted: suspend () -> Unit
) {
    companion object {
        const val PRODUCT_PREMIUM = "premium_lifetime"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private var productDetails: com.android.billingclient.api.ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                scope.launch { handlePurchase(purchase) }
            }
        }
        _isPurchasing.value = false
    }

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var isConnected = false

    suspend fun connect(): Boolean {
        if (isConnected) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    isConnected = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (cont.isActive) cont.resume(isConnected)
                }
                override fun onBillingServiceDisconnected() {
                    isConnected = false
                }
            })
        }
    }

    suspend fun queryProduct(): com.android.billingclient.api.ProductDetails? {
        if (!connect()) return null
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_PREMIUM)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val result: ProductDetailsResult = billingClient.queryProductDetails(params)
        productDetails = result.productDetailsList?.firstOrNull()
        return productDetails
    }

    fun launchPurchase(activity: Activity): Boolean {
        val details = productDetails ?: return false
        _isPurchasing.value = true
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _isPurchasing.value = false
            return false
        }
        return true
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            var acknowledged = false
            for (attempt in 1..3) {
                val result = billingClient.acknowledgePurchase(ackParams)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    acknowledged = true
                    break
                }
                Timber.w("acknowledgePurchase attempt $attempt failed: ${result.debugMessage}")
                kotlinx.coroutines.delay(1000L * attempt)
            }
            if (!acknowledged) {
                Timber.e("Failed to acknowledge purchase after 3 attempts")
                return
            }
        }
        if (purchase.products.contains(PRODUCT_PREMIUM)) {
            onPremiumGranted()
        }
    }

    suspend fun restorePurchases(): Boolean {
        if (!connect()) return false
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in result.purchasesList) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.contains(PRODUCT_PREMIUM)
                ) {
                    onPremiumGranted()
                    return true
                }
            }
        }
        return false
    }

    fun destroy() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}
