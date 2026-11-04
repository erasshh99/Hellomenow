package com.piyush.island.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.material.button.MaterialButton
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentSimpleBinding

/**
 * Shima Pro. The Pro features are unlocked with a one off purchase through Google Play.
 *
 * Nothing about the purchase is checked outside Google Play: the entitlement comes from the
 * Play billing result and is stored in the app's own settings.
 */
class ProFragment : Fragment() {

    private var _binding: FragmentSimpleBinding? = null
    private val binding get() = _binding!!

    private var billingClient: BillingClient? = null
    private var product: ProductDetails? = null
    private lateinit var buyButton: MaterialButton
    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSimpleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        heading(getString(R.string.pro_title))
        body(getString(R.string.pro_headline))

        listOf(
            R.string.pro_more_alerts,
            R.string.pro_more_rules,
            R.string.pro_swipe_gestures,
            R.string.pro_companion,
            R.string.pro_backup,
        ).forEach { bullet ->
            body("• " + getString(bullet))
        }

        statusText = TextView(requireContext()).apply {
            textSize = 15f
            setPadding(0, 16, 0, 8)
        }
        binding.container.addView(statusText)

        buyButton = MaterialButton(requireContext()).apply {
            text = getString(R.string.pro_buy)
            setOnClickListener { purchase() }
        }
        binding.container.addView(buyButton)

        val restore = MaterialButton(requireContext()).apply {
            text = getString(R.string.pro_restore)
            setOnClickListener { restorePurchases() }
        }
        binding.container.addView(restore)

        connectBilling()
        renderState()
    }

    private fun renderState() {
        val unlocked = AppGraph.settings.current.proUnlocked
        statusText.setText(if (unlocked) R.string.pro_owned else R.string.pro_headline)
        buyButton.isEnabled = !unlocked
    }

    private fun connectBilling() {
        val client = BillingClient.newBuilder(requireContext())
            .setListener { result, purchases ->
                handlePurchases(result, purchases)
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        billingClient = client
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
                queryProduct()
                restorePurchases()
            }

            override fun onBillingServiceDisconnected() {
                // The button stays, the purchase can be retried.
            }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billingClient?.queryProductDetailsAsync(params) { _, details ->
            product = details.firstOrNull()
            val price = product?.oneTimePurchaseOfferDetails?.formattedPrice
            if (price != null) buyButton.text = "${getString(R.string.pro_buy)} · $price"
        }
    }

    private fun purchase() {
        val details = product ?: return
        val activity = activity ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient?.launchBillingFlow(activity, params)
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient?.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK &&
                purchases.any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        it.products.contains(PRODUCT_ID)
                }
            ) {
                unlock()
            }
        }
    }

    private fun handlePurchases(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        purchases?.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(PRODUCT_ID)
            ) {
                if (purchase.isAcknowledged) {
                    unlock()
                } else {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(params) { _ -> unlock() }
                }
            }
        }
    }

    private fun unlock() {
        AppGraph.settings.setProUnlocked(true)
        renderState()
    }

    private fun heading(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 24, 0, 8)
        binding.container.addView(this)
    }

    private fun body(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 15f
        setPadding(0, 0, 0, 8)
        binding.container.addView(this)
    }

    override fun onDestroyView() {
        billingClient?.endConnection()
        billingClient = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val PRODUCT_ID = "shima_pro"
    }
}
