package com.novacleaner.app.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.novacleaner.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Owns the Mobile Ads SDK lifecycle and the interstitial. Interstitials are always preloaded so
 * that showing one never makes the user wait; if none is ready the action simply continues.
 */
class AdsManager(
    private val app: Application,
    private val isPremium: () -> Boolean,
) {
    private val main = Handler(Looper.getMainLooper())
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var initStarted = false
    private var interstitial: InterstitialAd? = null
    private var interstitialLoadedAt = 0L
    private var loading = false
    private var retryAttempt = 0
    private val counters = mutableMapOf<AdPlacement, Int>()

    /** True while any full-screen ad (interstitial or app open) is on screen. */
    var isShowingFullScreen = false
        internal set

    /** Timestamp of the last full-screen ad dismissal; shared with [AppOpenAdManager]. */
    var lastFullScreenAt = 0L
        internal set

    var onReady: (() -> Unit)? = null

    fun initialize() {
        if (initStarted || isPremium()) return
        initStarted = true
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(app) {
                main.post {
                    _ready.value = true
                    preloadInterstitial()
                    onReady?.invoke()
                }
            }
        }
    }

    private fun interstitialValid() =
        interstitial != null && System.currentTimeMillis() - interstitialLoadedAt < AdConfig.INTERSTITIAL_EXPIRY_MS

    fun preloadInterstitial() {
        if (!_ready.value || isPremium() || loading || interstitialValid()) return
        loading = true
        InterstitialAd.load(
            app,
            BuildConfig.AD_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoadedAt = System.currentTimeMillis()
                    loading = false
                    retryAttempt = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loading = false
                    retryAttempt++
                    val delay = min(60_000L, 4_000L * (1L shl min(retryAttempt, 4)))
                    main.postDelayed({ preloadInterstitial() }, delay)
                }
            },
        )
    }

    /**
     * Shows an interstitial for [placement] if pacing rules allow it, then runs [onFinished]
     * (immediately when no ad is shown, or after the ad is dismissed).
     */
    fun showInterstitial(activity: Activity?, placement: AdPlacement, onFinished: () -> Unit) {
        var done = false
        val finish = {
            if (!done) {
                done = true
                onFinished()
            }
        }
        if (activity == null || isPremium() || !_ready.value || isShowingFullScreen) {
            finish(); return
        }
        val count = (counters[placement] ?: 0) + 1
        counters[placement] = count
        if (count % placement.frequency != 0) {
            finish(); return
        }
        if (System.currentTimeMillis() - lastFullScreenAt < AdConfig.FULLSCREEN_COOLDOWN_MS) {
            // Too soon after the previous ad: make the very next eligible event show instead.
            counters[placement] = count - 1
            finish(); return
        }
        val ad = interstitial?.takeIf { interstitialValid() }
        if (ad == null) {
            counters[placement] = count - 1
            preloadInterstitial()
            finish(); return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingFullScreen = false
                lastFullScreenAt = System.currentTimeMillis()
                preloadInterstitial()
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                isShowingFullScreen = false
                preloadInterstitial()
                finish()
            }
        }
        isShowingFullScreen = true
        ad.show(activity)
    }

    /** Drops cached ads once the user buys "Remove Ads". */
    fun onPremiumActivated() {
        interstitial = null
    }
}
