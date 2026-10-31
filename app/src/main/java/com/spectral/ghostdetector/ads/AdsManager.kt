package com.spectral.ghostdetector.ads

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AdsManager(
    private val app: Application,
    private val scope: CoroutineScope,
    private val isPremium: () -> Boolean,
) {
    private val started = AtomicBoolean(false)

    /** Compose-observable: true once the Mobile Ads SDK is initialized. */
    var ready by mutableStateOf(false)
        private set

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastShownAt = 0L
    private var opportunities = 0

    fun initialize() {
        if (isPremium() || !started.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(app) {
                scope.launch {
                    ready = true
                    loadInterstitial()
                }
            }
        }
    }

    private fun loadInterstitial() {
        if (!ready || isPremium() || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(app, AdIds.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
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

    /**
     * Shows an interstitial at a natural break (e.g. leaving a scan), frequency capped:
     * every 2nd opportunity and at most once per [COOLDOWN_MS]. [onDone] always runs.
     */
    fun showInterstitial(activity: Activity?, onDone: () -> Unit) {
        opportunities++
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val allowed = activity != null && !isPremium() && ad != null &&
            opportunities % 2 == 0 && now - lastShownAt > COOLDOWN_MS
        if (!allowed) {
            loadInterstitial()
            onDone()
            return
        }
        ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial()
                onDone()
            }
        }
        lastShownAt = now
        ad.show(activity!!)
    }

    companion object {
        private const val COOLDOWN_MS = 60_000L
    }
}
