package com.voxcast.mic.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.voxcast.mic.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google AdMob: banner, interstitial and native ads, all disabled for Premium users.
 * Uses Google's official test unit ids; swap in your own ids from the AdMob console.
 * Interstitials are only shown between sessions (never while audio is live) and are
 * frequency-capped, which keeps the app within AdMob policy.
 */
class AdsManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: SettingsRepository,
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastInterstitialAt = 0L

    val adsEnabled: Boolean get() = !settings.settings.value.premium.isEntitled

    fun initialize() {
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {
                _ready.value = true
                scope.launch(Dispatchers.Main) { preloadInterstitial() }
            }
        }
    }

    fun preloadInterstitial() {
        if (!adsEnabled || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            context,
            AdUnits.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loadingInterstitial = false
                }
            },
        )
    }

    /** Shows an interstitial if one is ready and the cap allows; always calls [onDone]. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit = {}) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (!adsEnabled || ad == null || now - lastInterstitialAt < MIN_INTERVAL_MS) {
            onDone()
            preloadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitial = null
                preloadInterstitial()
                onDone()
            }
        }
        lastInterstitialAt = now
        scope.launch { withContext(Dispatchers.Main) { ad.show(activity) } }
    }

    private companion object {
        const val MIN_INTERVAL_MS = 90_000L
    }
}

object AdUnits {
    // Google sample ad units. Replace with production ids before release.
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
}
