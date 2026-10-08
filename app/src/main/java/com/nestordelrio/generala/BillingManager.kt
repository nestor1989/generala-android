package com.nestordelrio.generala

import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/** "Sin anuncios": producto de compra única `remove_ads` (Play Billing Library 8). */
class BillingManager(private val activity: MainActivity, private val prefs: Prefs) : PurchasesUpdatedListener {

    private val productId = BuildConfig.REMOVE_ADS_PRODUCT_ID

    private val client: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var productDetails: ProductDetails? = null

    @Volatile
    var formattedPrice: String? = null
        private set

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryProduct()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing no disponible: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() reconecta en la próxima llamada.
            }
        })
    }

    fun end() {
        client.endConnection()
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                productDetails = queryResult.productDetailsList.firstOrNull()
                @Suppress("DEPRECATION")
                formattedPrice = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                activity.js("GeneralaApp.onAdsStateChanged()")
            }
        }
    }

    /** Recupera la compra al reinstalar o en otro dispositivo, y la quita si hubo reembolso. */
    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.any {
                productId in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            purchases.forEach { handlePurchase(it, silent = true) }
            if (!owned && prefs.adsRemoved) {
                prefs.adsRemoved = false
                activity.js("GeneralaApp.onAdsStateChanged()")
            }
        }
    }

    fun launchPurchase() {
        val details = productDetails
        if (details == null) {
            toast("La tienda no está disponible ahora. Probá en un rato.")
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
            )
            .build()
        client.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it, silent = false) }
            BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            BillingResponseCode.USER_CANCELED -> Unit
            else -> toast("No se pudo completar la compra.")
        }
    }

    private fun handlePurchase(purchase: Purchase, silent: Boolean) {
        if (productId !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                val wasRemoved = prefs.adsRemoved
                prefs.adsRemoved = true
                activity.ads.onAdsRemoved()
                activity.js("GeneralaApp.onAdsStateChanged()")
                if (!silent && !wasRemoved) toast("¡Listo! Ya no vas a ver anuncios entre partidas.")
                if (!purchase.isAcknowledged) {
                    val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    client.acknowledgePurchase(ack) { r ->
                        if (r.responseCode != BillingResponseCode.OK) Log.w(TAG, "Ack falló: ${r.debugMessage}")
                    }
                }
            }
            Purchase.PurchaseState.PENDING ->
                if (!silent) toast("Tu pago está pendiente. Cuando se acredite, se quitan los anuncios.")
            else -> Unit
        }
    }

    private fun toast(msg: String) = activity.runOnUiThread {
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val TAG = "GeneralaBilling"
    }
}
