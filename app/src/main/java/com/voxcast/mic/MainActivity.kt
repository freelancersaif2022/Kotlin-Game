package com.voxcast.mic

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voxcast.mic.net.HostMode
import com.voxcast.mic.service.StreamController
import com.voxcast.mic.service.StreamEvent
import com.voxcast.mic.service.StreamState
import com.voxcast.mic.ui.components.Messenger
import com.voxcast.mic.ui.components.VoxSnackbarHost
import com.voxcast.mic.ui.screens.HomeScreen
import com.voxcast.mic.ui.screens.MicScreen
import com.voxcast.mic.ui.screens.PremiumScreen
import com.voxcast.mic.ui.screens.ScanScreen
import com.voxcast.mic.ui.screens.SettingsScreen
import com.voxcast.mic.ui.screens.SpeakerScreen
import com.voxcast.mic.ui.screens.SplashScreen
import com.voxcast.mic.ui.theme.VoxColors
import com.voxcast.mic.ui.theme.VoxTheme

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SPEAKER = "speaker/{mode}"
    const val SCAN = "scan"
    const val MIC = "mic?payload={payload}"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"

    fun speaker(mode: HostMode) = "speaker/${mode.name}"
    fun mic(payload: String) = "mic?payload=${Uri.encode(payload)}"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            VoxTheme {
                val nav = rememberNavController()

                LaunchedEffect(Unit) {
                    StreamController.events.collect { e ->
                        when (e) {
                            is StreamEvent.Success -> Messenger.success(e.text)
                            is StreamEvent.Info -> Messenger.info(e.text)
                            is StreamEvent.Warning -> Messenger.warning(e.text)
                            is StreamEvent.Error -> Messenger.error(e.text)
                            is StreamEvent.SessionEnded -> Unit
                        }
                    }
                }

                Box(Modifier.fillMaxSize().background(VoxColors.Background)) {
                    VoxNavHost(nav, startDestination = if (savedInstanceState == null) Routes.SPLASH else Routes.HOME)
                    VoxSnackbarHost(Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun VoxNavHost(nav: NavHostController, startDestination: String) {
    val goHome: () -> Unit = {
        if (!nav.popBackStack(Routes.HOME, inclusive = false)) {
            nav.navigate(Routes.HOME) { popUpTo(0) }
        }
    }
    NavHost(
        navController = nav,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(tween(380)) { it / 4 } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(380)) { -it / 6 } + fadeOut(tween(250)) },
        popEnterTransition = { slideInHorizontally(tween(380)) { -it / 6 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(380)) { it / 4 } + fadeOut(tween(250)) },
    ) {
        composable(
            Routes.SPLASH,
            exitTransition = { fadeOut(tween(450)) + scaleOut(tween(450), targetScale = 1.08f) },
        ) {
            SplashScreen(onFinished = {
                // Resume an ongoing session (e.g. after the activity was recreated from the notification).
                val next = when (val s = StreamController.state.value) {
                    is StreamState.SpeakerWaiting -> Routes.speaker(s.info.mode)
                    is StreamState.SpeakerLive -> Routes.speaker(s.info.mode)
                    else -> Routes.HOME
                }
                nav.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(
            Routes.HOME,
            enterTransition = { fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.96f) },
        ) {
            HomeScreen(
                onOpenMic = { nav.navigate(Routes.SCAN) },
                onOpenSpeaker = { mode -> nav.navigate(Routes.speaker(mode)) },
                onOpenPremium = { nav.navigate(Routes.PREMIUM) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SPEAKER, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
            val mode = runCatching { HostMode.valueOf(entry.arguments?.getString("mode") ?: "") }.getOrDefault(HostMode.LAN)
            SpeakerScreen(mode = mode, onExit = goHome, onOpenPremium = { nav.navigate(Routes.PREMIUM) })
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { nav.popBackStack() },
                onScanned = { payload -> nav.navigate(Routes.mic(payload)) { popUpTo(Routes.SCAN) { inclusive = true } } },
            )
        }
        composable(
            Routes.MIC,
            arguments = listOf(navArgument("payload") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            MicScreen(
                payload = entry.arguments?.getString("payload").orEmpty(),
                onExit = goHome,
                onScanAgain = { nav.navigate(Routes.SCAN) { popUpTo(Routes.HOME) } },
            )
        }
        composable(
            Routes.PREMIUM,
            enterTransition = { slideInVertically(tween(450)) { it } + fadeIn() },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = { slideOutVertically(tween(400)) { it } + fadeOut() },
        ) {
            PremiumScreen(onClose = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() }, onOpenPremium = { nav.navigate(Routes.PREMIUM) })
        }
    }
}
