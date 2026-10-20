package com.smartisland.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartisland.app.ui.components.BrandSplash
import com.smartisland.app.ui.navigation.AppRoot
import com.smartisland.app.ui.navigation.RouteRequest
import com.smartisland.app.ui.screens.PreviewCache
import com.smartisland.app.ui.theme.SmartIslandTheme

class MainActivity : ComponentActivity() {

    private val routeRequest = mutableStateOf<RouteRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // The launch screen is always light (it matches the system splash color, so there is no flash).
        applySystemBars(false)
        super.onCreate(savedInstanceState)
        container.premium.renewIfNeeded()
        container.ads.gatherConsent(this)
        if (savedInstanceState == null) handle(intent)
        val coldStart = savedInstanceState == null

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val dark = settings.darkMode
            var showSplash by remember { mutableStateOf(coldStart) }
            LaunchedEffect(dark, showSplash) { applySystemBars(dark && !showSplash) }
            SmartIslandTheme(dark) {
                Box {
                    AppRoot(routeRequest.value) { routeRequest.value = null }
                    if (showSplash) BrandSplash(onFinished = { showSplash = false })
                }
            }
        }
        PreviewCache.prewarm(this)
    }

    override fun onStop() {
        container.settings.flush()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun handle(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE) ?: return
        routeRequest.value = RouteRequest(route, intent.getStringExtra(EXTRA_HIGHLIGHT))
    }

    companion object {
        const val EXTRA_ROUTE = "route"
        const val EXTRA_HIGHLIGHT = "highlight"
    }
}
