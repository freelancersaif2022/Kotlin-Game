package com.saif.imagetopdf

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.saif.imagetopdf.ui.AppTheme
import com.saif.imagetopdf.ui.FailedScreen
import com.saif.imagetopdf.ui.HomeScreen
import com.saif.imagetopdf.ui.ProcessingScreen
import com.saif.imagetopdf.ui.ResultScreen
import com.saif.imagetopdf.ui.SplashScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Every screen has a dark header, so light status-bar icons everywhere.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            AppTheme {
                val state by vm.state.collectAsState()
                BackHandler(enabled = state is UiState.Done || state is UiState.Failed) { vm.reset() }
                AnimatedContent(
                    targetState = state,
                    contentKey = { it::class },
                    transitionSpec = {
                        (fadeIn(tween(520, delayMillis = 90, easing = FastOutSlowInEasing)) +
                            scaleIn(tween(520, delayMillis = 90, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                            (fadeOut(tween(260)) + scaleOut(tween(260), targetScale = 1.04f))
                    },
                    label = "screen",
                ) { s ->
                    when (s) {
                        UiState.Splash -> SplashScreen(onFinished = vm::splashFinished)
                        UiState.Home -> HomeScreen(onImagePicked = vm::convert)
                        is UiState.Processing -> ProcessingScreen(s)
                        is UiState.Done -> ResultScreen(s, onNew = vm::reset)
                        is UiState.Failed -> FailedScreen(s.message, onBack = vm::reset)
                    }
                }
            }
        }
    }
}
