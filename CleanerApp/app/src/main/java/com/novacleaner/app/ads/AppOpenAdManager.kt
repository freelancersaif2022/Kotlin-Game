package com.novacleaner.app.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.novacleaner.app.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * App-open ads: shown on cold start from the splash (never on the very first launch) and when
 * the user returns to the app after a while. Returns from screens the app itself opened
 * (system settings, uninstall dialogs, share sheets) are skipped.
 */
class AppOpenAdManager(
    private val app: Application,
    private val ads: AdsManager,
    private val isPremium: () -> Boolean,
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var currentActivity: Activity? = null
    private var skipNext = false
    private var backgroundAt = 0L
    private var lastShownAt = 0L

    /** Set by the splash once it has finished; resume ads are only allowed after that. */
    var coldStartHandled = false

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun isAvailable(): Boolean =
        appOpenAd != null && System.currentTimeMillis() - loadedAt < AdConfig.APP_OPEN_EXPIRY_MS

    fun load() {
        if (!ads.ready.value || isPremium() || loading || isAvailable()) return
        loading = true
        AppOpenAd.load(
            app,
            BuildConfig.AD_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadedAt = System.currentTimeMillis()
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }

    /** Waits (bounded) for an ad to become available. Used by the splash screen. */
    suspend fun awaitAvailable(timeoutMs: Long): Boolean = withTimeoutOrNull(timeoutMs) {
        while (!isAvailable()) {
            if (ads.ready.value && !loading) load()
            delay(120)
        }
        true
    } ?: false

    fun show(activity: Activity, onDone: () -> Unit) {
        val ad = appOpenAd?.takeIf { isAvailable() }
        if (ad == null || isPremium() || ads.isShowingFullScreen) {
            load()
            onDone(); return
        }
        appOpenAd = null
        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                onDone()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ads.isShowingFullScreen = false
                ads.lastFullScreenAt = System.currentTimeMillis()
                lastShownAt = System.currentTimeMillis()
                load()
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ads.isShowingFullScreen = false
                load()
                finish()
            }
        }
        ads.isShowingFullScreen = true
        ad.show(activity)
    }

    /** Call right before launching an external screen so returning doesn't trigger an ad. */
    fun skipNextResume() {
        skipNext = true
    }

    override fun onStart(owner: LifecycleOwner) {
        val now = System.currentTimeMillis()
        val wasInBackground = backgroundAt != 0L
        val backgroundFor = now - backgroundAt
        if (!wasInBackground || !coldStartHandled) return
        if (skipNext) {
            skipNext = false; return
        }
        if (backgroundFor < AdConfig.APP_OPEN_MIN_BACKGROUND_MS) return
        if (now - lastShownAt < AdConfig.APP_OPEN_RESUME_COOLDOWN_MS) return
        if (now - ads.lastFullScreenAt < AdConfig.FULLSCREEN_COOLDOWN_MS) return
        val activity = currentActivity ?: return
        show(activity) { }
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundAt = System.currentTimeMillis()
    }

    private fun Activity.isOurs() = javaClass.name.startsWith("com.novacleaner")

    override fun onActivityStarted(activity: Activity) {
        if (activity.isOurs()) currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity.isOurs()) currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
