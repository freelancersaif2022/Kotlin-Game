package com.saif.imagetopdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
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
        setContent {
            AppTheme {
                val state by vm.state.collectAsState()
                BackHandler(enabled = state is UiState.Done || state is UiState.Failed) { vm.reset() }
                AnimatedContent(targetState = state, contentKey = { it::class }, label = "screen") { s ->
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
