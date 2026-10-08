package com.nestordelrio.generala

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Anuncios de la app:
 *  - Recompensado (opcional): "tirada extra" y "deshacer", solo contra la máquina.
 *  - Intersticial: al tocar "Jugar de nuevo", cada 3 partidas completas, nunca en la
 *    primera sesión y como máximo 1 cada 5 minutos. Desaparece con "Sin anuncios".
 * Antes de pedir anuncios se resuelve el consentimiento (UMP), obligatorio en EEE y Reino Unido.
 */
class AdsManager(private val activity: MainActivity, private val prefs: Prefs) {

    private val main = Handler(Looper.getMainLooper())
    private val initialized = AtomicBoolean(false)
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var rewarded: RewardedAd? = null
    private var loadingRewarded = false

    @Volatile
    var isRewardedReady = false
        private set

    @Volatile
    var privacyOptionsRequired = false
        private set

    fun start() {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consentimiento: ${formError.message}")
                    updatePrivacyOptions()
                    if (consentInformation.canRequestAds()) initAds()
                }
            },
            { requestError ->
                Log.w(TAG, "No se pudo actualizar el consentimiento: ${requestError.message}")
            }
        )
        // Si ya hay consentimiento de una sesión anterior, no esperamos al formulario.
        if (consentInformation.canRequestAds()) initAds()
    }

    private fun updatePrivacyOptions() {
        privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        activity.js("GeneralaApp.onAdsStateChanged()")
    }

    fun showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Opciones de privacidad: ${formError.message}")
            updatePrivacyOptions()
        }
    }

    private fun initAds() {
        if (!initialized.compareAndSet(false, true)) return
        // Tu celular como dispositivo de prueba: ve anuncios de prueba aunque el build tenga IDs reales.
        val testDevices = BuildConfig.ADMOB_TEST_DEVICE_IDS.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (testDevices.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
            )
        }
        Thread {
            MobileAds.initialize(activity) {
                main.post {
                    loadRewarded()
                    loadInterstitial()
                }
            }
        }.start()
    }

    // ---------------- Intersticial ----------------

    private fun loadInterstitial() {
        if (!initialized.get() || prefs.adsRemoved || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            activity,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                    interstitial = null
                    main.postDelayed({ loadInterstitial() }, RETRY_MS)
                }
            }
        )
    }

    fun maybeShowInterstitial() {
        if (prefs.adsRemoved) return
        val games = prefs.gamesFinished
        val now = System.currentTimeMillis()
        val due = prefs.sessions > 1 &&
            games >= GAMES_BETWEEN_INTERSTITIALS &&
            games % GAMES_BETWEEN_INTERSTITIALS == 0 &&
            games != prefs.lastInterstitialGame &&
            now - prefs.lastInterstitialAt > MIN_INTERVAL_MS
        if (!due) return
        val ad = interstitial ?: run { loadInterstitial(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                prefs.interstitialsShown = prefs.interstitialsShown + 1
                loadInterstitial()
                if (prefs.interstitialsShown == 2) activity.js("GeneralaApp.onOfferRemoveAds()")
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial()
            }
        }
        prefs.lastInterstitialAt = now
        prefs.lastInterstitialGame = games
        ad.show(activity)
    }

    fun onAdsRemoved() {
        interstitial = null
    }

    // ---------------- Recompensado ----------------

    private fun loadRewarded() {
        if (!initialized.get() || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingRewarded = false
                    rewarded = ad
                    isRewardedReady = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingRewarded = false
                    rewarded = null
                    isRewardedReady = false
                    main.postDelayed({ loadRewarded() }, RETRY_MS)
                }
            }
        )
    }

    fun showRewarded(reason: String) {
        val quoted = JSONObject.quote(reason)
        val ad = rewarded
        if (ad == null) {
            activity.js("GeneralaApp.onRewardResult($quoted, false)")
            loadRewarded()
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                isRewardedReady = false
                activity.js("GeneralaApp.onRewardResult($quoted, $earned)")
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                isRewardedReady = false
                activity.js("GeneralaApp.onRewardResult($quoted, false)")
                loadRewarded()
            }
        }
        ad.show(activity) { earned = true }
    }

    private companion object {
        const val TAG = "GeneralaAds"
        const val GAMES_BETWEEN_INTERSTITIALS = 3
        const val MIN_INTERVAL_MS = 5 * 60 * 1000L
        const val RETRY_MS = 60 * 1000L
    }
}
