package com.novacleaner.app.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novacleaner.app.ads.AdPlacement
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.ui.apps.AppManagerScreen
import com.novacleaner.app.ui.battery.BatteryScreen
import com.novacleaner.app.ui.datausage.DataUsageScreen
import com.novacleaner.app.ui.files.FileListMode
import com.novacleaner.app.ui.files.FileListScreen
import com.novacleaner.app.ui.home.HomeScreen
import com.novacleaner.app.ui.junk.JunkScreen
import com.novacleaner.app.ui.media.DuplicatesScreen
import com.novacleaner.app.ui.media.ScreenshotsScreen
import com.novacleaner.app.ui.onboarding.OnboardingScreen
import com.novacleaner.app.ui.permissions.PermissionsScreen
import com.novacleaner.app.ui.premium.PremiumScreen
import com.novacleaner.app.ui.result.ResultScreen
import com.novacleaner.app.ui.settings.SettingsScreen
import com.novacleaner.app.ui.splash.SplashScreen
import com.novacleaner.app.ui.traffic.TrafficScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"
    const val RESULT = "result/{tool}/{bytes}/{count}"
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Navigation entry points that weave in interstitials at the right moments. */
class Navigator(val nav: NavHostController, private val activity: () -> Activity?) {

    fun openTool(tool: Tool) {
        AppGraph.ads.showInterstitial(activity(), AdPlacement.TOOL_OPEN) {
            nav.navigate(tool.route) { launchSingleTop = true }
        }
    }

    /** Leave a tool screen (top bar back or system back). */
    fun exitTool() {
        val from = nav.currentBackStackEntry?.id
        AppGraph.ads.showInterstitial(activity(), AdPlacement.TOOL_EXIT) {
            if (nav.currentBackStackEntry?.id == from) back()
        }
    }

    /** After a clean/delete: interstitial, then the result screen replacing the tool. */
    fun showResult(tool: Tool, bytes: Long, count: Int) {
        AppGraph.ads.showInterstitial(activity(), AdPlacement.RESULT) {
            nav.navigate("result/${tool.name}/$bytes/$count") {
                popUpTo(tool.route) { inclusive = true }
            }
        }
    }

    fun openPremium() = nav.navigate(Routes.PREMIUM) { launchSingleTop = true }

    fun openSettings() = nav.navigate(Routes.SETTINGS) { launchSingleTop = true }

    fun back() {
        if (nav.previousBackStackEntry != null) nav.popBackStack()
        else nav.navigate(Routes.HOME) { popUpTo(0) }
    }

    fun goHome() {
        if (!nav.popBackStack(Routes.HOME, inclusive = false)) {
            nav.navigate(Routes.HOME) { popUpTo(0) }
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator not provided") }

private val easing = FastOutSlowInEasing

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition =
    slideInHorizontally(tween(340, easing = easing)) { it / 5 } + fadeIn(tween(300))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    slideOutHorizontally(tween(340, easing = easing)) { -it / 10 } + fadeOut(tween(220))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideInHorizontally(tween(340, easing = easing)) { -it / 10 } + fadeIn(tween(300))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutHorizontally(tween(320, easing = easing)) { it / 5 } + fadeOut(tween(220))

private fun NavGraphBuilder.sheet(route: String, content: @Composable (NavBackStackEntry) -> Unit) = composable(
    route,
    enterTransition = { slideInVertically(tween(380, easing = easing)) { it / 3 } + fadeIn(tween(300)) },
    exitTransition = { fadeOut(tween(200)) },
    popEnterTransition = { fadeIn(tween(250)) },
    popExitTransition = { slideOutVertically(tween(320, easing = easing)) { it / 3 } + fadeOut(tween(220)) },
) { content(it) }

@Composable
fun NovaNavHost() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val navigator = remember(nav) { Navigator(nav) { context.findActivity() } }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            enterTransition = { enter() },
            exitTransition = { exit() },
            popEnterTransition = { popEnter() },
            popExitTransition = { popExit() },
        ) {
            composable(Routes.SPLASH, exitTransition = { fadeOut(tween(350)) }) { SplashScreen() }
            composable(
                Routes.ONBOARDING,
                enterTransition = { fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 0.96f) },
            ) { OnboardingScreen() }
            composable(Routes.PERMISSIONS) { PermissionsScreen() }
            composable(
                Routes.HOME,
                enterTransition = { fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.97f) },
            ) { HomeScreen() }

            composable(Tool.JUNK.route) { JunkScreen() }
            composable(Tool.LARGE_FILES.route) { FileListScreen(FileListMode.LARGE) }
            composable(Tool.APK.route) { FileListScreen(FileListMode.APK) }
            composable(Tool.DUPLICATES.route) { DuplicatesScreen() }
            composable(Tool.SCREENSHOTS.route) { ScreenshotsScreen() }
            composable(Tool.APPS.route) { AppManagerScreen() }
            composable(Tool.DATA_USAGE.route) { DataUsageScreen() }
            composable(Tool.SPEED.route) { TrafficScreen() }
            composable(Tool.BATTERY.route) { BatteryScreen() }

            sheet(Routes.PREMIUM) { PremiumScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(
                Routes.RESULT,
                arguments = listOf(
                    navArgument("tool") { type = NavType.StringType },
                    navArgument("bytes") { type = NavType.LongType },
                    navArgument("count") { type = NavType.IntType },
                ),
                enterTransition = { fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f) },
            ) { entry ->
                val tool = runCatching { Tool.valueOf(entry.arguments?.getString("tool").orEmpty()) }.getOrDefault(Tool.JUNK)
                ResultScreen(
                    tool = tool,
                    bytes = entry.arguments?.getLong("bytes") ?: 0L,
                    count = entry.arguments?.getInt("count") ?: 0,
                )
            }
        }
    }
}
