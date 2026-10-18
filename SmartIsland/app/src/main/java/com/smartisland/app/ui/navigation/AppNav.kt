package com.smartisland.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class Tab(val route: String, val label: String, val icon: ImageVector, val idleIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    Tab(Routes.ISLAND, "Island", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    Tab(Routes.WIDGETS, "Widgets", Icons.Rounded.Widgets, Icons.Outlined.Widgets),
    Tab(Routes.CONTROLS, "Controls", Icons.Rounded.Tune, Icons.Outlined.Tune),
    Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
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

            val onTab = currentRoute in Routes.tabs
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .then(if (onTab) Modifier else Modifier.navigationBarsPadding()),
            ) {
                // Keep snackbars above the screen's own bottom buttons and the system navigation bar.
                AppSnackbarHost(snack, Modifier.padding(bottom = if (!onTab && currentRoute in routesWithBottomCta) 78.dp else 0.dp))
                if (onTab) {
                    BottomBar(currentRoute) { navigator.go(it) }
                }
            }
        }
    }
}

/** Screens with a pinned bottom button that snackbars must sit above. */
private val routesWithBottomCta = setOf(Routes.PAYWALL, Routes.BATTERY_GUIDE, Routes.ONBOARDING)

private val slideSpec = tween<IntOffset>(240, easing = FastOutSlowInEasing)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in Routes.tabs && targetState.destination.route in Routes.tabs

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter() =
    if (isTabSwitch()) EnterTransition.None
    else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slideSpec) + fadeIn(tween(160))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit() =
    if (isTabSwitch()) ExitTransition.None
    else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, slideSpec, targetOffset = { it / 4 }) + fadeOut(tween(160))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter() =
    if (isTabSwitch()) EnterTransition.None
    else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, slideSpec, initialOffset = { it / 4 }) + fadeIn(tween(160))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit() =
    if (isTabSwitch()) ExitTransition.None
    else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slideSpec) + fadeOut(tween(160))

/** Floating glass tab bar. The tapped tab highlights instantly, before navigation even starts. */
@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    var tapped by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(current) { tapped = null }
    val active = tapped ?: current
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(shape)
            .background(Palette.TabBar)
            .border(1.dp, Palette.Outline, shape)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == active
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        tapped = tab.route
                        onSelect(tab.route)
                    }
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
                        if (selected) tab.icon else tab.idleIcon, tab.label,
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
