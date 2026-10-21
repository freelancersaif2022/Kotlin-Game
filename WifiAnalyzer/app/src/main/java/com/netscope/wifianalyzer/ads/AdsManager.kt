package com.netscope.wifianalyzer.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.BuildConfig
import com.netscope.wifianalyzer.data.AppPrefs
import com.netscope.wifianalyzer.data.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/** Where an interstitial is requested from; lets us tune placements later. */
enum class AdPlacement { SPEED_TEST_DONE, DEVICE_SCAN_DONE, LEAVE_HISTORY, LEAVE_CHANNELS }

/**
 * Handles consent (UMP), interstitials with a frequency cap, rewarded ads and
 * app-open ads. Nothing is shown to premium users.
 */
class AdsManager(
    private val context: Context,
    private val premium: PremiumManager,
    private val prefs: AppPrefs,
) {
    private val initialized = AtomicBoolean(false)
    private var consentInfo: ConsentInformation? = null

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var lastInterstitialAt = 0L

    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()
    private val rewardedWaiters = mutableListOf<(Boolean) -> Unit>()

    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var appOpenLoading = false
    private var showingFullScreen = false

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    // ------------------------------------------------------------------ consent

    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updateConsentState(info)
                }
            },
            { updateConsentState(info) },
        )
        // Consent from a previous session lets ads start loading right away.
        updateConsentState(info)
    }

    private fun updateConsentState(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds()) {
            _canRequestAds.value = true
            initialize()
        }
    }

    fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error -> onDone(error?.message) }
    }

    private fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        Thread {
            MobileAds.initialize(context) {}
        }.start()
        preload()
    }

    fun preload() {
        if (!_canRequestAds.value) return
        loadRewarded()
        if (!premium.isPremium) {
            loadInterstitial()
            loadAppOpen()
        }
    }

    private fun request(): AdRequest = AdRequest.Builder().build()

    // ------------------------------------------------------------------ interstitial

    private fun loadInterstitial() {
        if (interstitial != null || interstitialLoading || !_canRequestAds.value) return
        interstitialLoading = true
        InterstitialAd.load(context, BuildConfig.AD_INTERSTITIAL_ID, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                interstitialLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                interstitialLoading = false
            }
        })
    }

    /**
     * Shows an interstitial if one is ready, the user is not premium and the
     * frequency cap allows it. [onDone] always runs exactly once.
     */
    fun showInterstitial(activity: Activity?, placement: AdPlacement, onDone: () -> Unit) {
        val now = System.currentTimeMillis()
        val skipFirstUse = when (placement) {
            AdPlacement.SPEED_TEST_DONE -> prefs.completedTests <= 1
            AdPlacement.DEVICE_SCAN_DONE -> prefs.completedScans <= 1
            else -> false
        }
        val ad = interstitial
        if (activity == null || premium.isPremium || skipFirstUse || showingFullScreen ||
            now - lastInterstitialAt < AppConfig.INTERSTITIAL_MIN_INTERVAL_MS || ad == null
        ) {
            if (ad == null) loadInterstitial()
            onDone()
            return
        }
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            showingFullScreen = false
            onDone()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastInterstitialAt = System.currentTimeMillis()
                loadInterstitial()
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial()
                finish()
            }
        }
        showingFullScreen = true
        lastInterstitialAt = now
        ad.show(activity)
    }

    // ------------------------------------------------------------------ rewarded

    private fun loadRewarded() {
        if (rewarded != null || rewardedLoading || !_canRequestAds.value) return
        rewardedLoading = true
        RewardedAd.load(context, BuildConfig.AD_REWARDED_ID, request(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad
                rewardedLoading = false
                _rewardedReady.value = true
                notifyRewardedWaiters(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewarded = null
                rewardedLoading = false
                _rewardedReady.value = false
                notifyRewardedWaiters(false)
            }
        })
    }

    private fun notifyRewardedWaiters(ok: Boolean) {
        val copy = rewardedWaiters.toList()
        rewardedWaiters.clear()
        copy.forEach { it(ok) }
    }

    /** Makes sure a rewarded ad is loading; [onResult] reports whether one is ready. */
    fun ensureRewarded(onResult: (Boolean) -> Unit) {
        if (rewarded != null) {
            onResult(true)
            return
        }
        if (!_canRequestAds.value) {
            onResult(false)
            return
        }
        rewardedWaiters += onResult
        loadRewarded()
    }

    /**
     * Shows a rewarded ad. [onReward] fires when the user earned the reward,
     * [onClosed] fires after the ad is dismissed or failed (earned = true/false).
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onClosed: (earned: Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded()
            onClosed(false)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                _rewardedReady.value = false
                showingFullScreen = false
                loadRewarded()
                onClosed(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                _rewardedReady.value = false
                showingFullScreen = false
                loadRewarded()
                onClosed(false)
            }
        }
        showingFullScreen = true
        ad.show(activity) {
            earned = true
            onReward()
        }
    }

    // ------------------------------------------------------------------ app open

    private fun loadAppOpen() {
        if (appOpenLoading || !_canRequestAds.value || premium.isPremium) return
        if (appOpen != null && System.currentTimeMillis() - appOpenLoadedAt < 4 * 60 * 60 * 1000L) return
        appOpenLoading = true
        AppOpenAd.load(context, BuildConfig.AD_APP_OPEN_ID, request(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpen = ad
                appOpenLoadedAt = System.currentTimeMillis()
                appOpenLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                appOpen = null
                appOpenLoading = false
            }
        })
    }

    /** Called when the app returns from the background after a long time. */
    fun showAppOpenIfAvailable(activity: Activity) {
        val ad = appOpen
        if (ad == null || premium.isPremium || showingFullScreen) {
            loadAppOpen()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpen = null
                showingFullScreen = false
                loadAppOpen()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpen = null
                showingFullScreen = false
                loadAppOpen()
            }
        }
        showingFullScreen = true
        ad.show(activity)
    }
}
