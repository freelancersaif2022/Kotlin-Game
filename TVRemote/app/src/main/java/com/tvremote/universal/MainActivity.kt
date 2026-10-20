package com.tvremote.universal

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.data.ThemeMode
import com.tvremote.universal.ui.AppNavHost
import com.tvremote.universal.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = App.container
        container.ads.gatherConsent(this)
        setContent {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            AppTheme(dark = dark) {
                AppNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        App.container.billing.refreshPurchases()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val handler = RemoteSession.onVolumeKey
        if (handler != null && App.container.settings.volumeKeys.value) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> { handler(true); return true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { handler(false); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (RemoteSession.onVolumeKey != null && App.container.settings.volumeKeys.value &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
