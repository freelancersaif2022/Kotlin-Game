package com.zyropay.app.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zyropay.app.data.AppRepository
import com.zyropay.app.ui.screens.auth.BiometricSetupScreen
import com.zyropay.app.ui.screens.auth.CreatePasscodeScreen
import com.zyropay.app.ui.screens.auth.ForgotPasswordScreen
import com.zyropay.app.ui.screens.auth.LockScreen
import com.zyropay.app.ui.screens.auth.LoginScreen
import com.zyropay.app.ui.screens.auth.NotificationPermissionScreen
import com.zyropay.app.ui.screens.auth.OtpScreen
import com.zyropay.app.ui.screens.auth.ResetPasswordScreen
import com.zyropay.app.ui.screens.auth.SignUpScreen
import com.zyropay.app.ui.screens.onboarding.OnboardingScreen
import com.zyropay.app.ui.screens.onboarding.SplashScreen
import com.zyropay.app.ui.screens.onboarding.WelcomeScreen

private const val DURATION = 320

private fun NavBackStackEntry.arg(name: String): String = arguments?.getString(name).orEmpty()

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val locked by AppRepository.locked.collectAsState()
    LaunchedEffect(locked) {
        val route = nav.currentDestination?.route
        if (locked && route != null && route != Routes.SPLASH && !route.startsWith("lock")) {
            nav.navigate(Routes.lock(true)) { launchSingleTop = true }
        }
    }
    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(DURATION)) },
        exitTransition = { slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 8 } + fadeOut(tween(DURATION / 2)) },
        popEnterTransition = { slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 8 } + fadeIn(tween(DURATION)) },
        popExitTransition = { slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { it / 3 } + fadeOut(tween(DURATION / 2)) },
    ) {
        composable(Routes.SPLASH, enterTransition = { fadeIn() }, exitTransition = { fadeOut(tween(400)) }) { SplashScreen(nav) }
        composable(Routes.ONBOARDING, enterTransition = { fadeIn(tween(500)) }) { OnboardingScreen(nav) }
        composable(Routes.WELCOME, enterTransition = { fadeIn(tween(500)) }) { WelcomeScreen(nav) }
        composable(Routes.LOGIN) { LoginScreen(nav) }
        composable(Routes.SIGNUP) { SignUpScreen(nav) }
        composable(Routes.OTP) { OtpScreen(nav, it.arg("target"), it.arg("purpose")) }
        composable(Routes.FORGOT) { ForgotPasswordScreen(nav) }
        composable(Routes.RESET) { ResetPasswordScreen(nav, it.arg("target")) }
        composable(Routes.CREATE_PASSCODE) { CreatePasscodeScreen(nav, it.arg("mode")) }
        composable(Routes.BIOMETRIC_SETUP) { BiometricSetupScreen(nav) }
        composable(Routes.NOTIFICATION_PERMISSION) { NotificationPermissionScreen(nav) }
        composable(
            Routes.LOCK,
            arguments = listOf(navArgument("resume") { type = NavType.BoolType; defaultValue = false }),
            enterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(250)) },
        ) { LockScreen(nav, it.arguments?.getBoolean("resume") ?: false) }
        mainGraph(nav)
    }
}
