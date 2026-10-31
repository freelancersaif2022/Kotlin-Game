package com.spectral.ghostdetector.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spectral.ghostdetector.ui.history.HistoryScreen
import com.spectral.ghostdetector.ui.home.HomeScreen
import com.spectral.ghostdetector.ui.premium.PremiumScreen
import com.spectral.ghostdetector.ui.scanner.ScannerScreen
import com.spectral.ghostdetector.ui.settings.SettingsScreen
import com.spectral.ghostdetector.ui.splash.SplashScreen

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val HISTORY = "history"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    fun back() {
        if (nav.previousBackStackEntry != null) nav.popBackStack()
    }
    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(350)) },
        exitTransition = { fadeOut(tween(250)) },
        popEnterTransition = { fadeIn(tween(350)) },
        popExitTransition = { fadeOut(tween(250)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onFindGhost = { nav.navigate(Routes.SCANNER) { launchSingleTop = true } },
                onHistory = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } },
                onPremium = { nav.navigate(Routes.PREMIUM) { launchSingleTop = true } },
                onSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(Routes.SCANNER) { ScannerScreen(onBack = ::back) }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = ::back,
                onScan = {
                    nav.navigate(Routes.SCANNER) { popUpTo(Routes.HOME); launchSingleTop = true }
                },
            )
        }
        composable(Routes.PREMIUM) { PremiumScreen(onBack = ::back) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = ::back,
                onPremium = { nav.navigate(Routes.PREMIUM) { launchSingleTop = true } },
            )
        }
    }
}
