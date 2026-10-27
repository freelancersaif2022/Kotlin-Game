package com.pdfstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.pdfstudio.app.ui.screens.MainScreen
import com.pdfstudio.app.ui.screens.SplashScreen
import com.pdfstudio.app.ui.theme.PdfStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkPref by remember {
                mutableStateOf(if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null)
            }
            val dark = darkPref ?: systemDark

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            PdfStudioTheme(darkTheme = dark) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = {
                            (fadeIn(tween(450, delayMillis = 100)) + scaleIn(tween(500), initialScale = 1.04f))
                                .togetherWith(fadeOut(tween(300)))
                        },
                        label = "splash",
                    ) { splash ->
                        if (splash) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            MainScreen(
                                isDark = dark,
                                onToggleTheme = {
                                    val next = !dark
                                    darkPref = next
                                    prefs.edit().putBoolean(KEY_DARK, next).apply()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val KEY_DARK = "dark_mode"
    }
}
