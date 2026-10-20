package com.vaultly.app.ads

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
import com.vaultly.app.BuildConfig
import com.vaultly.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AdMob wrapper with a user-friendly policy: interstitials only after a save, never during the
 * first two saves, and at most once every 3 minutes. Rewarded ads are always opt-in.
 */
class AdsManager(private val context: Context, private val prefs: Prefs) {
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context) {}
            launch(Dispatchers.Main) {
                if (prefs.adsEnabled) loadInterstitial()
                loadRewarded()
            }
        }
    }

    private fun loadInterstitial() {
        if (interstitial != null || loadingInterstitial || !prefs.adsEnabled) return
        loadingInterstitial = true
        InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loadingInterstitial = false }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null; loadingInterstitial = false }
            })
    }

    private fun loadRewarded() {
        if (rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; loadingRewarded = false }
                override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null; loadingRewarded = false }
            })
    }

    /** Call after the user saves an item. [onDone] always runs, ad or not. */
    fun onItemSaved(activity: Activity, onDone: () -> Unit) {
        prefs.savesCount = prefs.savesCount + 1
        val ad = interstitial
        val now = System.currentTimeMillis()
        val due = prefs.savesCount > 2 && now - prefs.lastInterstitialAt > INTERSTITIAL_GAP_MS
        if (!prefs.adsEnabled || ad == null || !due) {
            loadInterstitial()
            onDone()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadInterstitial(); onDone() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { loadInterstitial(); onDone() }
        }
        prefs.lastInterstitialAt = now
        ad.show(activity)
    }

    val rewardedReady: Boolean get() = rewarded != null

    /**
     * Shows a rewarded ad. [onReward] runs only if the user earned the reward;
     * [onUnavailable] runs when no ad could be shown (e.g. offline).
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded()
            onUnavailable()
            return
        }
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded()
                if (earned) onReward()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { loadRewarded(); onUnavailable() }
        }
        ad.show(activity) { earned = true }
    }

    fun preloadRewarded() = loadRewarded()

    companion object {
        private const val INTERSTITIAL_GAP_MS = 3 * 60 * 1000L
    }
}
