package com.netscope.wifianalyzer.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.billing.BillingEvent
import com.netscope.wifianalyzer.data.ThemeMode
import com.netscope.wifianalyzer.ui.components.AppSnackbarController
import com.netscope.wifianalyzer.ui.components.AppSnackbarHost
import com.netscope.wifianalyzer.ui.components.BannerAd
import com.netscope.wifianalyzer.ui.components.LocalPremiumActions
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.LocalTour
import com.netscope.wifianalyzer.ui.components.PremiumActions
import com.netscope.wifianalyzer.ui.components.RewardUnlockDialog
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.TourOverlay
import com.netscope.wifianalyzer.ui.components.TourState
import com.netscope.wifianalyzer.ui.components.TourStep
import com.netscope.wifianalyzer.ui.components.rememberPremiumState
import com.netscope.wifianalyzer.ui.components.tourTarget
import com.netscope.wifianalyzer.ui.screens.ChannelAnalyzerScreen
import com.netscope.wifianalyzer.ui.screens.DataUsageScreen
import com.netscope.wifianalyzer.ui.screens.DevicesScreen
import com.netscope.wifianalyzer.ui.screens.HeatmapScreen
import com.netscope.wifianalyzer.ui.screens.HelpScreen
import com.netscope.wifianalyzer.ui.screens.HistoryScreen
import com.netscope.wifianalyzer.ui.screens.HomeScreen
import com.netscope.wifianalyzer.ui.screens.IntruderScreen
import com.netscope.wifianalyzer.ui.screens.OnboardingScreen
import com.netscope.wifianalyzer.ui.screens.PermissionsScreen
import com.netscope.wifianalyzer.ui.screens.PremiumScreen
import com.netscope.wifianalyzer.ui.screens.PrivacyScreen
import com.netscope.wifianalyzer.ui.screens.RouterScreen
import com.netscope.wifianalyzer.ui.screens.SettingsScreen
import com.netscope.wifianalyzer.ui.screens.SignalMeterScreen
import com.netscope.wifianalyzer.ui.screens.SpeedTestScreen
import com.netscope.wifianalyzer.ui.screens.ToolsScreen
import com.netscope.wifianalyzer.ui.screens.WidgetsScreen
import com.netscope.wifianalyzer.ui.screens.WifiInfoScreen
import com.netscope.wifianalyzer.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val SPEED = "speed"
    const val SPEED_PATTERN = "speed?auto={auto}"
    const val DEVICES = "devices"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val WIFI_INFO = "wifi_info"
    const val SIGNAL = "signal"
    const val HISTORY = "history"
    const val CHANNELS = "channels"
    const val HEATMAP = "heatmap"
    const val DATA_USAGE = "data_usage"
    const val ROUTER = "router"
    const val INTRUDER = "intruder"
    const val WIDGETS = "widgets"
    const val PREMIUM = "premium"
    const val HELP = "help"
    const val PRIVACY = "privacy"

    fun speed(auto: Boolean) = if (auto) "speed?auto=true" else SPEED
}

private data class TabItem(val route: String, val pattern: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, Routes.HOME, "Home", Icons.Rounded.Home),
    TabItem(Routes.SPEED, Routes.SPEED_PATTERN, "Speed", Icons.Rounded.Speed),
    TabItem(Routes.DEVICES, Routes.DEVICES, "Devices", Icons.Rounded.Devices),
    TabItem(Routes.TOOLS, Routes.TOOLS, "Tools", Icons.Rounded.Apps),
    TabItem(Routes.SETTINGS, Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
)

fun NavHostController.navigateTab(route: String) {
    // A fresh "auto start" speed test must not restore the previously saved tab state.
    val fresh = route.contains("auto=true")
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = !fresh }
        launchSingleTop = true
        restoreState = !fresh
    }
}

@Composable
fun AppRoot(deepLink: MutableStateFlow<String?>) {
    val themeMode by AppGraph.prefs.theme.collectAsState()
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    AppTheme(dark = dark) {
        val nav = rememberNavController()
        val scope = rememberCoroutineScope()
        val snackbar = remember { AppSnackbarController(scope) }
        val tour = remember { TourState() }
        var showReward by remember { mutableStateOf(false) }
        var showTour by remember { mutableStateOf(false) }
        val premiumActions = remember {
            PremiumActions(
                openRewardUnlock = { showReward = true },
                openPaywall = { nav.navigate(Routes.PREMIUM) { launchSingleTop = true } },
            )
        }
        val startRoute = remember {
            when {
                !AppGraph.prefs.onboardingDone -> Routes.ONBOARDING
                !AppGraph.prefs.permissionsDone -> Routes.PERMISSIONS
                else -> Routes.HOME
            }
        }
        val backStack by nav.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val showBottomBar = tabs.any { it.pattern == currentRoute }
        val premium by rememberPremiumState()

        // Deep links from notifications and widgets.
        val pending by deepLink.collectAsState()
        LaunchedEffect(pending, currentRoute) {
            val target = pending ?: return@LaunchedEffect
            if (currentRoute == null || currentRoute == Routes.ONBOARDING || currentRoute == Routes.PERMISSIONS) return@LaunchedEffect
            deepLink.value = null
            if (target == Routes.HOME || target == Routes.DEVICES || target.startsWith(Routes.SPEED)) {
                nav.navigateTab(target)
            } else {
                nav.navigate(target) { launchSingleTop = true }
            }
        }

        LaunchedEffect(Unit) {
            AppGraph.billing.events.collect { e ->
                when (e) {
                    is BillingEvent.Purchased -> snackbar.show("Welcome to Premium! All features unlocked, no ads.", SnackType.Premium)
                    BillingEvent.Pending -> snackbar.show("Payment pending. Premium unlocks once Google Play confirms it.", SnackType.Info)
                    BillingEvent.Cancelled -> Unit
                    is BillingEvent.Failed -> snackbar.show(e.message, SnackType.Error)
                    is BillingEvent.Restored -> snackbar.show(
                        if (e.active) "Your Premium subscription was restored" else "No active subscription found for this Google account",
                        if (e.active) SnackType.Success else SnackType.Info,
                    )
                }
            }
        }

        LaunchedEffect(currentRoute) {
            if (currentRoute == Routes.HOME && !AppGraph.prefs.homeTourDone) {
                kotlinx.coroutines.delay(900)
                showTour = true
            }
        }

        CompositionLocalProvider(
            LocalSnackbar provides snackbar,
            LocalPremiumActions provides premiumActions,
            LocalTour provides tour,
        ) {
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0),
                    snackbarHost = { AppSnackbarHost(snackbar) },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                        ) {
                            Column {
                                if (!premium.isPremium && currentRoute != Routes.SPEED_PATTERN) BannerAd()
                                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                                    tabs.forEach { tab ->
                                        val selected = currentRoute == tab.pattern
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { if (!selected) nav.navigateTab(tab.route) },
                                            icon = { Icon(tab.icon, tab.label) },
                                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.tourTarget(tour, "tab_${tab.route}"),
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    },
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = startRoute,
                        modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
                        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
                        exitTransition = { fadeOut(tween(160)) },
                        popEnterTransition = { fadeIn(tween(220)) },
                        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 12 } },
                    ) {
                        composable(Routes.ONBOARDING, enterTransition = { EnterTransition.None }) {
                            OnboardingScreen(onFinish = {
                                AppGraph.prefs.onboardingDone = true
                                if (AppGraph.prefs.permissionsDone) {
                                    // Replayed from Settings: just go back.
                                    if (!nav.popBackStack()) nav.navigate(Routes.HOME)
                                } else {
                                    nav.navigate(Routes.PERMISSIONS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                                }
                            })
                        }
                        composable(Routes.PERMISSIONS) {
                            PermissionsScreen(onDone = {
                                AppGraph.prefs.permissionsDone = true
                                nav.navigate(Routes.HOME) { popUpTo(Routes.PERMISSIONS) { inclusive = true } }
                            })
                        }
                        composable(Routes.HOME, exitTransition = { ExitTransition.None }) { HomeScreen(nav) }
                        composable(
                            Routes.SPEED_PATTERN,
                            arguments = listOf(navArgument("auto") { type = NavType.BoolType; defaultValue = false }),
                        ) { entry -> SpeedTestScreen(nav, autoStart = entry.arguments?.getBoolean("auto") == true) }
                        composable(Routes.DEVICES) { DevicesScreen(nav) }
                        composable(Routes.TOOLS) { ToolsScreen(nav) }
                        composable(Routes.SETTINGS) { SettingsScreen(nav) }
                        composable(Routes.WIFI_INFO) { WifiInfoScreen(nav) }
                        composable(Routes.SIGNAL) { SignalMeterScreen(nav) }
                        composable(Routes.HISTORY) { HistoryScreen(nav) }
                        composable(Routes.CHANNELS) { ChannelAnalyzerScreen(nav) }
                        composable(Routes.HEATMAP) { HeatmapScreen(nav) }
                        composable(Routes.DATA_USAGE) { DataUsageScreen(nav) }
                        composable(Routes.ROUTER) { RouterScreen(nav) }
                        composable(Routes.INTRUDER) { IntruderScreen(nav) }
                        composable(Routes.WIDGETS) { WidgetsScreen(nav) }
                        composable(
                            Routes.PREMIUM,
                            enterTransition = { slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(240)) },
                            popExitTransition = { slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(200)) },
                        ) { PremiumScreen(nav) }
                        composable(Routes.HELP) { HelpScreen(nav) }
                        composable(Routes.PRIVACY) { PrivacyScreen(nav) }
                    }
                }

                if (showTour && currentRoute == Routes.HOME) {
                    TourOverlay(
                        state = tour,
                        steps = listOf(
                            TourStep("hero", "Your Wi-Fi at a glance", "See your network name, live signal strength, band and channel. Tap the card for full details."),
                            TourStep("go", "Test your speed", "Tap GO to measure download, upload, ping and jitter in about 20 seconds."),
                            TourStep("unlock", "Unlock Pro for free", "Watch 3 short ads to use every Pro feature for 12 hours, or go Premium to remove ads forever."),
                            TourStep("tab_devices", "Who's on my Wi-Fi?", "Scan your network to see every connected device and get alerts when someone new joins."),
                        ),
                        onFinish = {
                            showTour = false
                            AppGraph.prefs.homeTourDone = true
                        },
                    )
                }

                if (showReward) {
                    RewardUnlockDialog(
                        onDismiss = { showReward = false },
                        onGoPremium = {
                            showReward = false
                            nav.navigate(Routes.PREMIUM) { launchSingleTop = true }
                        },
                    )
                }
            }
        }
    }
}

