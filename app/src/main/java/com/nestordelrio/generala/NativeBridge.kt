package com.nestordelrio.generala

import android.webkit.JavascriptInterface

/**
 * Métodos que el juego llama como `window.GeneralaNative.xxx()`.
 * Se ejecutan en un hilo del WebView: todo lo que toca UI pasa por runOnUiThread.
 */
class NativeBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun loadState(key: String): String? = activity.prefs.getWeb(key)

    @JavascriptInterface
    fun saveState(key: String, value: String) = activity.prefs.setWeb(key, value)

    @JavascriptInterface
    fun haptic(kind: String) = activity.runOnUiThread { Haptics.perform(activity.webView, kind) }

    @JavascriptInterface
    fun onGameFinished(won: Boolean) = activity.runOnUiThread { activity.onGameFinished(won) }

    @JavascriptInterface
    fun onPlayAgain() = activity.runOnUiThread { activity.ads.maybeShowInterstitial() }

    @JavascriptInterface
    fun isRewardedReady(): Boolean = activity.ads.isRewardedReady

    @JavascriptInterface
    fun showRewarded(reason: String) = activity.runOnUiThread { activity.ads.showRewarded(reason) }

    @JavascriptInterface
    fun hasRemovedAds(): Boolean = activity.prefs.adsRemoved

    @JavascriptInterface
    fun getRemoveAdsPrice(): String = activity.billing.formattedPrice ?: ""

    @JavascriptInterface
    fun purchaseRemoveAds() = activity.runOnUiThread { activity.billing.launchPurchase() }

    @JavascriptInterface
    fun share(text: String) = activity.runOnUiThread { activity.share(text) }

    @JavascriptInterface
    fun isReminderEnabled(): Boolean = activity.prefs.reminderEnabled

    @JavascriptInterface
    fun setReminderEnabled(enabled: Boolean) = activity.runOnUiThread { activity.setReminder(enabled) }

    @JavascriptInterface
    fun isPrivacyOptionsRequired(): Boolean = activity.ads.privacyOptionsRequired

    @JavascriptInterface
    fun showPrivacyOptions() = activity.runOnUiThread { activity.ads.showPrivacyOptions() }

    @JavascriptInterface
    fun openPrivacyPolicy() = activity.runOnUiThread { activity.openUrl(BuildConfig.PRIVACY_URL) }
}
