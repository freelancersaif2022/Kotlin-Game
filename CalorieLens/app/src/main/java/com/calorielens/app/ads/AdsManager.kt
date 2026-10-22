package com.calorielens.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdMob integration. Uses Google's official *test* ad unit IDs — swap them in [AdUnits] for your own
 * before publishing. Ads are never requested for Premium users.
 */
@Singleton
class AdsManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var initialized = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var scansSinceInterstitial = 0

    fun initialize() {
        if (initialized) return
        initialized = true
        scope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(context) {} }
            preload()
        }
    }

    fun preload() {
        loadInterstitial()
        loadRewarded()
    }

    val isRewardedReady: Boolean get() = rewarded != null

    private fun loadInterstitial() {
        if (interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(context, AdUnits.INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                loadingInterstitial = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                loadingInterstitial = false
            }
        })
    }

    private fun loadRewarded() {
        if (rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(context, AdUnits.REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad
                loadingRewarded = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewarded = null
                loadingRewarded = false
            }
        })
    }

    /** Shows an interstitial after every [INTERSTITIAL_EVERY] scans. [onDone] always runs exactly once. */
    fun maybeShowInterstitialAfterScan(activity: Activity, onDone: () -> Unit) {
        scansSinceInterstitial++
        val ad = interstitial
        if (scansSinceInterstitial < INTERSTITIAL_EVERY || ad == null) {
            loadInterstitial()
            onDone()
            return
        }
        scansSinceInterstitial = 0
        interstitial = null
        var finished = false
        val finish = { if (!finished) { finished = true; onDone(); loadInterstitial() } }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        ad.show(activity)
    }

    /** Shows a rewarded ad. [onResult] receives true when the reward was earned. */
    fun showRewarded(activity: Activity, onResult: (Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded()
            onResult(false)
            return
        }
        rewarded = null
        var earned = false
        var finished = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (!finished) { finished = true; onResult(earned) }
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                if (!finished) { finished = true; onResult(false) }
                loadRewarded()
            }
        }
        ad.show(activity) { earned = true }
    }

    private companion object {
        const val INTERSTITIAL_EVERY = 2
    }
}

object AdUnits {
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
}
