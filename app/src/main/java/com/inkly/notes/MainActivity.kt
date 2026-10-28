package com.inkly.notes

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inkly.notes.data.ThemeMode
import com.inkly.notes.ui.InklyNavHost
import com.inkly.notes.ui.theme.InklyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate().alpha(0f).setDuration(180).withEndAction { provider.remove() }.start()
        }
        enableEdgeToEdge()
        val container = (application as InklyApp).container

        setContent {
            val prefs by container.prefs.state.collectAsStateWithLifecycle()
            val dark = when (prefs.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            var onBrandSurface by remember { mutableStateOf(true) }
            val lightIcons = dark || onBrandSurface
            LaunchedEffect(lightIcons) {
                val style = if (lightIcons) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            InklyTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    InklyNavHost(container, onBrandSurface = { onBrandSurface = it })
                }
            }
        }
    }
}
