package com.netscope.wifianalyzer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.netscope.wifianalyzer.ui.navigation.AppRoot
import com.netscope.wifianalyzer.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val deepLink = MutableStateFlow<String?>(null)
    private var backgroundedAt = 0L

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            backgroundedAt = System.currentTimeMillis()
        }

        override fun onStart(owner: LifecycleOwner) {
            val away = System.currentTimeMillis() - backgroundedAt
            if (backgroundedAt > 0 && away >= AppConfig.APP_OPEN_MIN_BACKGROUND_MS && AppGraph.prefs.onboardingDone) {
                AppGraph.ads.showAppOpenIfAvailable(this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // Smooth exit: the logo zooms out and fades into the app.
        splash.setOnExitAnimationListener { provider ->
            val view = provider.view
            val icon = runCatching { provider.iconView }.getOrNull()
            icon?.animate()?.scaleX(1.35f)?.scaleY(1.35f)?.setDuration(260)?.start()
            view.animate()
                .alpha(0f)
                .setDuration(280)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { provider.remove() }
                .start()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink.value = intent?.getStringExtra(EXTRA_ROUTE)

        AppGraph.ads.gatherConsent(this)
        AppGraph.billing.connect()
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)

        setContent { AppRoot(deepLink) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_ROUTE)?.let { deepLink.value = it }
    }

    override fun onResume() {
        super.onResume()
        AppGraph.billing.refreshPurchases()
        AppGraph.ads.preload()
    }

    override fun onPause() {
        super.onPause()
        WidgetUpdater.updateAll(this)
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ROUTE = "open_route"
    }
}
