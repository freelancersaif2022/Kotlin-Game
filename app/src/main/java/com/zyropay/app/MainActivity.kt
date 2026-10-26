package com.zyropay.app

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.ThemeMode
import com.zyropay.app.navigation.AppNavHost
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.Messenger
import com.zyropay.app.ui.components.MessengerHost
import com.zyropay.app.ui.theme.Z
import com.zyropay.app.ui.theme.ZyroTheme

class MainActivity : FragmentActivity() {

    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by AppRepository.prefs.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (prefs.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose {}
            }
            val messenger = remember { Messenger() }
            ZyroTheme(dark = dark) {
                CompositionLocalProvider(LocalMessenger provides messenger) {
                    Box(Modifier.fillMaxSize().background(Z.colors.background)) {
                        AppNavHost()
                        MessengerHost(messenger)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > LOCK_AFTER_MS) AppRepository.lock()
    }

    companion object {
        private const val LOCK_AFTER_MS = 60_000L
    }
}
