package com.eyescroll.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.ui.components.AnimatedBackground
import com.eyescroll.app.ui.screens.CalibrationScreen
import com.eyescroll.app.ui.screens.HomeScreen
import com.eyescroll.app.ui.screens.OnboardingScreen
import com.eyescroll.app.ui.screens.PracticeScreen
import com.eyescroll.app.ui.screens.SetupScreen
import com.eyescroll.app.ui.theme.EsColors
import com.eyescroll.app.ui.theme.EyeScrollTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.eyescroll.app.R.style.Theme_EyeScroll)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(EsColors.Background.copy(alpha = 0f).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(EsColors.Background.copy(alpha = 0f).toArgb()),
        )
        setContent {
            EyeScrollTheme { EyeScrollRoot() }
        }
    }
}

enum class Screen { Onboarding, Setup, Calibration, Home, Practice }

@Composable
private fun EyeScrollRoot() {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val settings by store.settings.collectAsStateWithLifecycle()
    var screen by rememberSaveable {
        mutableStateOf(if (store.current.onboardingDone) Screen.Home else Screen.Onboarding)
    }
    // Where calibration returns to.
    var afterCalibration by rememberSaveable { mutableStateOf(Screen.Home) }

    BackHandler(enabled = screen == Screen.Practice || screen == Screen.Calibration || (screen == Screen.Setup && settings.onboardingDone)) {
        screen = if (screen == Screen.Calibration) afterCalibration else Screen.Home
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackground()
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 10 } +
                    scaleIn(tween(380), initialScale = 0.98f))
                    .togetherWith(fadeOut(tween(180)))
            },
            label = "nav",
        ) { s ->
            when (s) {
                Screen.Onboarding -> OnboardingScreen(onFinish = {
                    store.update { it.copy(onboardingDone = true) }
                    screen = Screen.Setup
                })
                Screen.Setup -> SetupScreen(
                    onCalibrate = {
                        afterCalibration = Screen.Setup
                        screen = Screen.Calibration
                    },
                    onDone = { screen = Screen.Home },
                )
                Screen.Calibration -> CalibrationScreen(
                    onFinished = { screen = Screen.Home },
                    onCancel = { screen = afterCalibration },
                )
                Screen.Home -> HomeScreen(
                    onCalibrate = {
                        afterCalibration = Screen.Home
                        screen = Screen.Calibration
                    },
                    onSetup = { screen = Screen.Setup },
                    onPractice = { screen = Screen.Practice },
                )
                Screen.Practice -> PracticeScreen(onBack = { screen = Screen.Home })
            }
        }
    }
}
