package com.smartisland.app.ads

import android.app.Activity
import android.app.Application
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.smartisland.app.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RewardOutcome { REWARDED, SKIPPED, NOT_READY, FAILED }

/**
 * Google UMP consent + AdMob rewarded ads. Ads are only requested after consent allows it, and a
 * fresh rewarded ad is preloaded after each one is shown so "Watch ad" is instant.
 */
class AdsManager(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sdkStarted = AtomicBoolean(false)
    private var consent: ConsentInformation? = null
    private var rewarded: RewardedAd? = null
    private var loading = false
    private var retryAttempt = 0

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    /** Call from the launcher activity's onCreate. Safe to call repeatedly. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consent = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { startSdk() }
            },
            { startSdk() },
        )
        if (info.canRequestAds()) startSdk()
    }

    val privacyOptionsRequired: Boolean
        get() = consent?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }

    private fun startSdk() {
        if (consent?.canRequestAds() != true) return
        if (sdkStarted.getAndSet(true)) {
            preload()
            return
        }
        scope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(app) {} }
            preload()
        }
    }

    fun preload() {
        if (!sdkStarted.get() || rewarded != null || loading) return
        loading = true
        RewardedAd.load(
            app,
            BuildConfig.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    retryAttempt = 0
                    rewarded = ad
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    rewarded = null
                    _rewardedReady.value = false
                    // Exponential backoff: 2s, 4s, 8s ... capped at 64s.
                    val wait = 1000L shl (++retryAttempt).coerceAtMost(6)
                    scope.launch {
                        delay(wait)
                        preload()
                    }
                }
            },
        )
    }

    fun showRewarded(activity: Activity, onResult: (RewardOutcome) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            preload()
            onResult(RewardOutcome.NOT_READY)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                clearAndReload()
                onResult(if (earned) RewardOutcome.REWARDED else RewardOutcome.SKIPPED)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                clearAndReload()
                onResult(RewardOutcome.FAILED)
            }
        }
        ad.show(activity) { earned = true }
    }

    private fun clearAndReload() {
        rewarded = null
        _rewardedReady.value = false
        preload()
    }
}
