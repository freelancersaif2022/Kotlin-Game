package com.flashshare.app.ads

import android.app.Activity
import android.content.Context
import com.flashshare.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Interstitial after every finished transfer and rewarded ads that unlock Group Share, Phone Clone and
 * the Cleaner for one use. Premium users never see ads (callers check premium first).
 */
class AdsManager(private val context: Context) {
    private val initialized = AtomicBoolean(false)
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var consent: ConsentInformation? = null

    /** Gathers GDPR consent when required (UMP), then initializes the SDK and preloads ads. */
    fun init(activity: Activity) {
        if (initialized.get()) return
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consent = info
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                if (info.canRequestAds()) startSdk()
            }
        }, { _ -> startSdk() })
        if (info.canRequestAds()) startSdk()
    }

    private fun startSdk() {
        if (!initialized.compareAndSet(false, true)) return
        MobileAds.initialize(context) {
            preloadInterstitial()
            preloadRewarded()
        }
    }

    fun preloadInterstitial() {
        if (!initialized.get() || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loadingInterstitial = false }
                override fun onAdFailedToLoad(error: LoadAdError) { loadingInterstitial = false }
            })
    }

    fun preloadRewarded() {
        if (!initialized.get() || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; loadingRewarded = false }
                override fun onAdFailedToLoad(error: LoadAdError) { loadingRewarded = false }
            })
    }

    /** Shows the post-transfer interstitial if one is ready; [onDone] always runs. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            preloadInterstitial()
            onDone()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preloadInterstitial(); onDone() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { preloadInterstitial(); onDone() }
        }
        ad.show(activity)
    }

    /**
     * Shows a rewarded ad. Waits briefly if it is still loading. Result:
     * REWARDED – user watched it, SKIPPED – user closed early, UNAVAILABLE – no ad could be loaded (offline, no fill).
     */
    suspend fun showRewarded(activity: Activity): RewardResult {
        if (!initialized.get()) startSdk()
        preloadRewarded()
        val ad = rewarded ?: withTimeoutOrNull(8000) {
            while (rewarded == null) {
                if (!loadingRewarded) preloadRewarded()
                delay(200)
            }
            rewarded
        } ?: return RewardResult.UNAVAILABLE
        rewarded = null
        return kotlinx.coroutines.suspendCancellableCoroutine { c ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    preloadRewarded()
                    if (c.isActive) c.resumeWith(Result.success(if (earned) RewardResult.REWARDED else RewardResult.SKIPPED))
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    preloadRewarded()
                    if (c.isActive) c.resumeWith(Result.success(RewardResult.UNAVAILABLE))
                }
            }
            ad.show(activity) { earned = true }
        }
    }

    fun privacyOptionsRequired(): Boolean =
        consent?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}

enum class RewardResult { REWARDED, SKIPPED, UNAVAILABLE }
