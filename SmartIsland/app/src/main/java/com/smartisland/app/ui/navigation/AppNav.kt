package com.smartisland.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.AppSnackbarHost
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.SnackController
import com.smartisland.app.ui.screens.BatteryGuideScreen
import com.smartisland.app.ui.screens.ControlsScreen
import com.smartisland.app.ui.screens.HomeScreen
import com.smartisland.app.ui.screens.IslandScreen
import com.smartisland.app.ui.screens.OnboardingScreen
import com.smartisland.app.ui.screens.PaywallScreen
import com.smartisland.app.ui.screens.PermissionsScreen
import com.smartisland.app.ui.screens.SettingsScreen
import com.smartisland.app.ui.screens.WidgetsScreen
import com.smartisland.app.ui.theme.Palette

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ISLAND = "island"
    const val WIDGETS = "widgets"
    const val CONTROLS = "controls"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
    const val PERMISSIONS = "permissions"
    const val BATTERY_GUIDE = "battery_guide"

    val tabs = setOf(HOME, ISLAND, WIDGETS, CONTROLS, SETTINGS)
    val all = tabs + setOf(PAYWALL, PERMISSIONS, BATTERY_GUIDE)
}

/** A navigation request coming from outside Compose (widget tap, tile, notification). */
data class RouteRequest(val route: String, val highlight: String? = null, val id: Long = System.nanoTime())

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home),
    Tab(Routes.ISLAND, "Island", Icons.Rounded.AutoAwesome),
    Tab(Routes.WIDGETS, "Widgets", Icons.Rounded.Widgets),
    Tab(Routes.CONTROLS, "Controls", Icons.Rounded.Tune),
    Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
)

/** Lets any screen navigate without threading the controller through every composable. */
class AppNavigator(private val nav: NavHostController) {
    fun go(route: String) {
        if (route in Routes.tabs) {
            nav.navigate(route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            nav.navigate(route) { launchSingleTop = true }
        }
    }

    fun back() {
        if (!nav.popBackStack()) go(Routes.HOME)
    }

    fun finishOnboarding() {
        nav.navigate(Routes.HOME) {
            popUpTo(Routes.ONBOARDING) { inclusive = true }
        }
    }
}

@Composable
fun AppRoot(request: RouteRequest?, onRequestHandled: () -> Unit) {
    val container = appContainer()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val navigator = remember(nav) { AppNavigator(nav) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackController(scope) }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val highlight = remember { mutableStateOf<String?>(null) }
    val startRoute = remember { if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING }

    LaunchedEffect(request?.id) {
        val r = request ?: return@LaunchedEffect
        if (r.route in Routes.all && settings.onboardingDone) {
            highlight.value = r.highlight
            navigator.go(r.route)
        }
        onRequestHandled()
    }

    CompositionLocalProvider(LocalSnack provides snack) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Palette.Background),
        ) {
            NavHost(
                navController = nav,
                startDestination = startRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { enter() },
                exitTransition = { exit() },
                popEnterTransition = { popEnter() },
                popExitTransition = { popExit() },
            ) {
                composable(Routes.ONBOARDING) { OnboardingScreen(navigator) }
                composable(Routes.HOME) { HomeScreen(navigator) }
                composable(Routes.ISLAND) { IslandScreen(navigator) }
                composable(Routes.WIDGETS) {
                    WidgetsScreen(navigator, highlight = highlight.value, onHighlightShown = { highlight.value = null })
                }
                composable(Routes.CONTROLS) { ControlsScreen(navigator) }
                composable(Routes.SETTINGS) { SettingsScreen(navigator) }
                composable(Routes.PAYWALL) { PaywallScreen(navigator) }
                composable(Routes.PERMISSIONS) { PermissionsScreen(navigator) }
                composable(Routes.BATTERY_GUIDE) { BatteryGuideScreen(navigator) }
            }

            Column(Modifier.align(Alignment.BottomCenter)) {
                AppSnackbarHost(snack)
                if (currentRoute in Routes.tabs) {
                    BottomBar(currentRoute) { navigator.go(it) }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in Routes.tabs && targetState.destination.route in Routes.tabs

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter() =
    if (isTabSwitch()) fadeIn(tween(180))
    else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn(tween(200))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit() =
    if (isTabSwitch()) fadeOut(tween(120))
    else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280), targetOffset = { it / 4 }) + fadeOut(tween(200))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter() =
    if (isTabSwitch()) fadeIn(tween(180))
    else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280), initialOffset = { it / 4 }) + fadeIn(tween(200))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit() =
    if (isTabSwitch()) fadeOut(tween(120))
    else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut(tween(200))

/** Floating glass tab bar. */
@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xF2141420))
            .border(1.dp, Palette.Outline, shape)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(tab.route) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Palette.Primary.copy(alpha = 0.18f) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                ) {
                    Icon(
                        tab.icon, tab.label,
                        tint = if (selected) Palette.PrimarySoft else Palette.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Palette.TextPrimary else Palette.TextSecondary,
                )
            }
        }
    }
}

/** Bottom padding every tab screen adds so content can scroll above the floating bar. */
val TabContentBottomPadding = 120.dp
