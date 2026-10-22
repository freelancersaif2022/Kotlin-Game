package com.calorielens.app.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calorielens.app.MainState
import com.calorielens.app.MainViewModel
import com.calorielens.app.data.prefs.ScanQuota
import com.calorielens.app.ui.camera.CameraScreen
import com.calorielens.app.ui.components.AppSnackbarHost
import com.calorielens.app.ui.components.LocalSnackbar
import com.calorielens.app.ui.components.SnackType
import com.calorielens.app.ui.components.SnackbarController
import com.calorielens.app.ui.history.HistoryScreen
import com.calorielens.app.ui.home.HomeScreen
import com.calorielens.app.ui.insights.InsightsScreen
import com.calorielens.app.ui.onboarding.OnboardingScreen
import com.calorielens.app.ui.premium.PremiumScreen
import com.calorielens.app.ui.profile.ProfileScreen
import com.calorielens.app.ui.result.ResultScreen
import com.calorielens.app.ui.scan.ScanLimitDialog
import com.calorielens.app.ui.scan.ScanScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val INSIGHTS = "insights"
    const val PROFILE = "profile"
    const val CAMERA = "camera"
    const val SCAN = "scan/{path}"
    const val RESULT = "result/{id}?fresh={fresh}"
    const val PREMIUM = "premium"

    val tabs = setOf(HOME, HISTORY, INSIGHTS, PROFILE)

    fun scan(path: String) = "scan/${Uri.encode(path)}"
    fun result(id: Long, fresh: Boolean = false) = "result/$id?fresh=$fresh"
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun AppRoot(mainViewModel: MainViewModel, state: MainState) {
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarController(snackbarHostState, scope) }
    val context = LocalContext.current
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in Routes.tabs
    var showLimitDialog by rememberSaveable { mutableStateOf(false) }

    val startDestination = remember { if (state.settings.onboardingDone) Routes.HOME else Routes.ONBOARDING }

    LaunchedEffect(state.settings.onboardingDone) {
        if (!state.settings.onboardingDone && nav.currentDestination?.route != Routes.ONBOARDING) {
            nav.navigate(Routes.ONBOARDING) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    val openCamera: () -> Unit = {
        if (mainViewModel.state.value.canScan) nav.navigate(Routes.CAMERA) { launchSingleTop = true }
        else showLimitDialog = true
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 12 } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 12 } },
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinished = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                tab(Routes.HOME) {
                    HomeScreen(
                        isPremium = state.settings.isPremium,
                        quota = state.quota,
                        onScan = openCamera,
                        onOpenMeal = { nav.navigate(Routes.result(it)) },
                        onPremium = { nav.navigate(Routes.PREMIUM) },
                    )
                }
                tab(Routes.HISTORY) {
                    HistoryScreen(
                        isPremium = state.settings.isPremium,
                        onOpenMeal = { nav.navigate(Routes.result(it)) },
                        onPremium = { nav.navigate(Routes.PREMIUM) },
                    )
                }
                tab(Routes.INSIGHTS) {
                    InsightsScreen(
                        isPremium = state.settings.isPremium,
                        onPremium = { nav.navigate(Routes.PREMIUM) },
                    )
                }
                tab(Routes.PROFILE) {
                    ProfileScreen(
                        settings = state.settings,
                        onPremium = { nav.navigate(Routes.PREMIUM) },
                    )
                }
                composable(
                    Routes.CAMERA,
                    enterTransition = { fadeIn(tween(220)) + scaleIn(tween(320), initialScale = 1.08f) },
                    exitTransition = { fadeOut(tween(220)) },
                    popExitTransition = { fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 1.06f) },
                ) {
                    CameraScreen(
                        onClose = { nav.popBackStack() },
                        onCaptured = { path ->
                            nav.navigate(Routes.scan(path)) { popUpTo(Routes.CAMERA) { inclusive = true } }
                        },
                    )
                }
                composable(
                    Routes.SCAN,
                    arguments = listOf(navArgument("path") { type = NavType.StringType }),
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(250)) },
                ) { entry ->
                    val path = entry.arguments?.getString("path").orEmpty()
                    ScanScreen(
                        imagePath = path,
                        onDone = { id ->
                            val proceed = {
                                nav.navigate(Routes.result(id, fresh = true)) { popUpTo(Routes.HOME) }
                            }
                            val activity = context.findActivity()
                            if (!state.settings.isPremium && activity != null) {
                                mainViewModel.ads.maybeShowInterstitialAfterScan(activity) { proceed() }
                            } else {
                                proceed()
                            }
                        },
                        onRetake = {
                            nav.navigate(Routes.CAMERA) { popUpTo(Routes.HOME) }
                        },
                        onClose = { nav.popBackStack(Routes.HOME, inclusive = false) },
                    )
                }
                composable(
                    Routes.RESULT,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("fresh") { type = NavType.BoolType; defaultValue = false },
                    ),
                    enterTransition = { fadeIn(tween(300)) + slideInVertically(tween(380)) { it / 8 } },
                    popExitTransition = { fadeOut(tween(220)) + slideOutVertically(tween(300)) { it / 8 } },
                ) {
                    ResultScreen(
                        isPremium = state.settings.isPremium,
                        onBack = { nav.popBackStack() },
                        onPremium = { nav.navigate(Routes.PREMIUM) },
                        onScanAgain = openCamera,
                    )
                }
                composable(
                    Routes.PREMIUM,
                    enterTransition = { slideInVertically(tween(420)) { it } + fadeIn(tween(300)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(200)) },
                    popExitTransition = { slideOutVertically(tween(380)) { it } + fadeOut(tween(300)) },
                ) {
                    PremiumScreen(onClose = { nav.popBackStack() })
                }
            }

            AnimatedVisibility(
                visible = showBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(300)) { it } + fadeIn(),
                exit = slideOutVertically(tween(250)) { it } + fadeOut(),
            ) {
                BottomBar(
                    current = route,
                    onSelect = { target -> nav.navigateTab(target) },
                    onScan = openCamera,
                )
            }

            AppSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (showBar) 96.dp else 12.dp),
            )
        }
    }

    if (showLimitDialog) {
        ScanLimitDialog(
            quota = state.quota,
            onWatchAd = {
                showLimitDialog = false
                val activity = context.findActivity()
                if (activity == null) {
                    snackbar.show("Ad not available right now. Try again shortly.", SnackType.ERROR)
                } else {
                    mainViewModel.ads.showRewarded(activity) { earned ->
                        if (earned) {
                            mainViewModel.grantRewardScans()
                            snackbar.show(
                                "+${ScanQuota.REWARD_BONUS_SCANS} scans added for today",
                                SnackType.SUCCESS,
                                title = "Reward unlocked",
                            )
                        } else {
                            snackbar.show("Ad isn't ready yet. Please try again in a moment.", SnackType.INFO)
                        }
                    }
                }
            },
            onPremium = {
                showLimitDialog = false
                nav.navigate(Routes.PREMIUM)
            },
            onDismiss = { showLimitDialog = false },
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.tab(route: String, content: @Composable () -> Unit) {
    composable(
        route,
        enterTransition = { fadeIn(tween(240)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) { content() }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
