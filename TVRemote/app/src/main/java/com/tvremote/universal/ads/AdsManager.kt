package com.tvremote.universal.ads

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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.tvremote.universal.BuildConfig
import com.tvremote.universal.billing.PremiumManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Interstitial + rewarded ads only (no banners).
 * Interstitials are frequency capped and never shown while the user is pressing remote keys.
 */
class AdsManager(
    private val context: Context,
    private val premium: PremiumManager,
    private val scope: CoroutineScope,
) {
    private val initialized = AtomicBoolean(false)
    private var consent: ConsentInformation? = null

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastInterstitialAt = 0L
    private val appStartedAt = System.currentTimeMillis()

    private var rewarded: RewardedAd? = null
    private var loadingRewarded = false
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Shows the GDPR/consent form when required, then initializes the Mobile Ads SDK. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consent = info
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacyFlag(info)
                    if (info.canRequestAds()) initialize()
                }
            },
            { if (info.canRequestAds()) initialize() },
        )
        if (info.canRequestAds()) initialize()
    }

    private fun updatePrivacyFlag(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    private fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {}
            withContext(Dispatchers.Main) {
                loadInterstitial()
                loadRewarded()
            }
        }
    }

    private fun canRequest() = initialized.get() && consent?.canRequestAds() != false

    private fun loadInterstitial() {
        if (!canRequest() || premium.isPremium.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(context, BuildConfig.INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                loadingInterstitial = false
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadingInterstitial = false
                interstitial = null
            }
        })
    }

    fun loadRewarded() {
        if (!canRequest() || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(context, BuildConfig.REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                loadingRewarded = false
                rewarded = ad
                _rewardedReady.value = true
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadingRewarded = false
                rewarded = null
                _rewardedReady.value = false
            }
        })
    }

    /**
     * Shows an interstitial at a natural break (after pairing, switching TV, leaving settings)
     * if one is ready and the frequency cap allows it. [then] always runs exactly once.
     */
    fun showInterstitial(activity: Activity?, then: () -> Unit) {
        val now = System.currentTimeMillis()
        val ad = interstitial
        val allowed = activity != null && ad != null && !premium.isPremium.value &&
            now - lastInterstitialAt >= MIN_INTERVAL_MS && now - appStartedAt >= GRACE_PERIOD_MS
        if (!allowed) {
            loadInterstitial()
            then()
            return
        }
        val done = AtomicBoolean(false)
        fun finish() { if (done.compareAndSet(false, true)) then() }
        ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial()
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial()
                finish()
            }
        }
        lastInterstitialAt = now
        interstitial = null
        ad.show(activity!!)
    }

    /** Shows a rewarded ad. [onReward] fires only when the user earned the reward. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit, onClosed: () -> Unit = {}) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded()
            onUnavailable()
            return
        }
        rewarded = null
        _rewardedReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadRewarded()
                onUnavailable()
            }
        }
        ad.show(activity) { onReward() }
    }

    companion object {
        private const val MIN_INTERVAL_MS = 3 * 60 * 1000L
        private const val GRACE_PERIOD_MS = 45 * 1000L
    }
}
