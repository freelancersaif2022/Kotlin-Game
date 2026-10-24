package com.pdfmaster.app.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.pdfmaster.app.BuildConfig
import com.pdfmaster.app.di.AppScope
import com.pdfmaster.app.premium.PremiumManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

enum class NativeState { IDLE, LOADING, LOADED, FAILED }

/**
 * Owns consent, SDK start-up and every ad format. Banner, native and interstitial ads are
 * suppressed for Premium users; rewarded ads stay available so a pass can be extended.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premium: PremiumManager,
    @AppScope private val scope: CoroutineScope,
) {
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val sdkStarted = AtomicBoolean(false)
    private val _sdkReady = MutableStateFlow(false)
    private val _consentDone = MutableStateFlow(false)

    /** True once the consent flow has finished (or failed) and the app can move past the splash. */
    val consentDone: StateFlow<Boolean> = _consentDone

    val rewardedAvailable: StateFlow<Boolean> = _sdkReady

    /** Whether banner / native / interstitial ads should be shown right now. */
    val adsEnabled: StateFlow<Boolean> = combine(_sdkReady, premium.state) { ready, p -> ready && !p.isPremium }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val privacyOptionsRequired: Boolean
        get() = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    // ---------- Consent & SDK ----------

    fun gatherConsent(activity: Activity) {
        if (_consentDone.value) return
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                _consentDone.value = true
                if (consent.canRequestAds()) startSdk()
            }
        }, {
            _consentDone.value = true
            if (consent.canRequestAds()) startSdk()
        })
        // Previously granted consent lets us start immediately while the update runs.
        if (consent.canRequestAds()) startSdk()
    }

    fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error -> onDone(error?.message) }
    }

    private fun startSdk() {
        if (!sdkStarted.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {
                _sdkReady.value = true
                scope.launch(Dispatchers.Main) {
                    loadRewarded()
                    if (!premium.isPremium) {
                        loadInterstitial()
                        loadNative()
                    }
                }
            }
        }
    }

    // ---------- Interstitial ----------

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var lastFullScreen = 0L
    private val interstitialGapMs = 60_000L

    private fun loadInterstitial() {
        if (interstitial != null || interstitialLoading || !_sdkReady.value || premium.isPremium) return
        interstitialLoading = true
        InterstitialAd.load(context, BuildConfig.AD_INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                interstitialLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialLoading = false
                scope.launch(Dispatchers.Main) {
                    delay(30_000)
                    loadInterstitial()
                }
            }
        })
    }

    /** Shows an interstitial if one is ready and the frequency cap allows; [onDone] always runs once. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        if (!adsEnabled.value || ad == null || now - lastFullScreen < interstitialGapMs) {
            loadInterstitial()
            onDone()
            return
        }
        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                onDone()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastFullScreen = SystemClock.elapsedRealtime()
                loadInterstitial()
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial()
                finish()
            }
        }
        lastFullScreen = now
        ad.show(activity)
    }

    // ---------- Rewarded ----------

    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady

    private fun loadRewarded() {
        if (rewarded != null || rewardedLoading || !_sdkReady.value) return
        rewardedLoading = true
        RewardedAd.load(context, BuildConfig.AD_REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad
                rewardedLoading = false
                _rewardedReady.value = true
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedLoading = false
                _rewardedReady.value = false
            }
        })
    }

    /**
     * Loads (if needed) and shows a rewarded ad. Returns false when no ad could be shown,
     * otherwise [onClosed] reports whether the reward was earned.
     */
    suspend fun showRewarded(activity: Activity, onClosed: (earned: Boolean) -> Unit): Boolean = withContext(Dispatchers.Main) {
        if (!_sdkReady.value) {
            withTimeoutOrNull(6_000) { _sdkReady.first { it } } ?: return@withContext false
        }
        if (rewarded == null) {
            loadRewarded()
            withTimeoutOrNull(10_000) { _rewardedReady.first { it } } ?: return@withContext false
        }
        val ad = rewarded ?: return@withContext false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                _rewardedReady.value = false
                lastFullScreen = SystemClock.elapsedRealtime()
                loadRewarded()
                onClosed(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                _rewardedReady.value = false
                loadRewarded()
                onClosed(false)
            }
        }
        ad.show(activity) { earned = true }
        true
    }

    // ---------- Native ----------

    private val _native = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _native
    private val _nativeState = MutableStateFlow(NativeState.IDLE)
    val nativeState: StateFlow<NativeState> = _nativeState
    private var nativeLoadedAt = 0L

    fun loadNative(force: Boolean = false) {
        if (!_sdkReady.value || premium.isPremium) return
        if (_nativeState.value == NativeState.LOADING) return
        val fresh = SystemClock.elapsedRealtime() - nativeLoadedAt < 90_000
        if (!force && _native.value != null && fresh) return
        _nativeState.value = NativeState.LOADING
        val loader = AdLoader.Builder(context, BuildConfig.AD_NATIVE_ID)
            .forNativeAd { ad ->
                val old = _native.value
                _native.value = ad
                old?.destroy()
                nativeLoadedAt = SystemClock.elapsedRealtime()
                _nativeState.value = NativeState.LOADED
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    _nativeState.value = if (_native.value != null) NativeState.LOADED else NativeState.FAILED
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }
}
