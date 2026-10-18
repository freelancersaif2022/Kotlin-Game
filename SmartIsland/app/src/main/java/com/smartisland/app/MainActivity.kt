package com.smartisland.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.smartisland.app.ui.navigation.AppRoot
import com.smartisland.app.ui.navigation.RouteRequest
import com.smartisland.app.ui.theme.SmartIslandTheme

class MainActivity : ComponentActivity() {

    private val routeRequest = mutableStateOf<RouteRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        container.premium.renewIfNeeded()
        container.ads.gatherConsent(this)
        if (savedInstanceState == null) handle(intent)

        setContent {
            SmartIslandTheme {
                AppRoot(routeRequest.value) { routeRequest.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
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
