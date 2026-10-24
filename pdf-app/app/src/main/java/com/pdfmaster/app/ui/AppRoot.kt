package com.pdfmaster.app.ui

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfmaster.app.ads.BannerAd
import com.pdfmaster.app.billing.BillingEvent
import com.pdfmaster.app.premium.PremiumEvent
import com.pdfmaster.app.ui.components.AppSnackbarHost
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.SnackType
import com.pdfmaster.app.ui.components.SnackbarController
import com.pdfmaster.app.ui.editor.EditorScreen
import com.pdfmaster.app.ui.files.FilesScreen
import com.pdfmaster.app.ui.home.HomeScreen
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.navigation.Navigator
import com.pdfmaster.app.ui.navigation.Routes
import com.pdfmaster.app.ui.onboarding.OnboardingScreen
import com.pdfmaster.app.ui.premium.PremiumScreen
import com.pdfmaster.app.ui.premium.rememberRewardLauncher
import com.pdfmaster.app.ui.result.ResultScreen
import com.pdfmaster.app.ui.search.SearchScreen
import com.pdfmaster.app.ui.settings.SettingsScreen
import com.pdfmaster.app.ui.splash.SplashScreen
import com.pdfmaster.app.ui.tool.ToolScreen
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.ui.tools.ToolsScreen
import com.pdfmaster.app.ui.viewer.ViewerScreen
import kotlinx.coroutines.flow.StateFlow

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(Routes.FILES, "Files", Icons.Outlined.Folder, Icons.Rounded.Folder),
    Tab(Routes.TOOLS, "Tools", Icons.Outlined.Widgets, Icons.Rounded.Widgets),
    Tab(Routes.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Rounded.Settings),
)

@Composable
fun AppRoot(incoming: StateFlow<Uri?>, onIncomingHandled: () -> Unit) {
    val nav = rememberNavController()
    val navigator = remember(nav) { Navigator(nav) }
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarController(hostState, scope) }

    CompositionLocalProvider(LocalNavigator provides navigator, LocalSnackbar provides snackbar) {
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val showBar = route in Routes.tabs
        GlobalEvents()

        val pending by incoming.collectAsStateWithLifecycle()
        LaunchedEffect(pending, route) {
            val uri = pending ?: return@LaunchedEffect
            if (route != null && route != Routes.SPLASH && route != Routes.ONBOARDING) {
                onIncomingHandled()
                navigator.viewer(uri)
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { AppSnackbarHost(hostState, if (showBar) Modifier else Modifier.navigationBarsPadding()) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    Column {
                        BannerAd()
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                            tabs.forEach { tab ->
                                val selected = route == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { if (!selected) navigator.tab(tab.route) },
                                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            val tabPadding = PaddingValues(bottom = padding.calculateBottomPadding())
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(320)) },
                exitTransition = { fadeOut(tween(200)) + scaleOut(targetScale = 0.97f, animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(tween(300)) + scaleIn(initialScale = 0.97f, animationSpec = tween(300)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it / 5 } + fadeOut(tween(250)) },
            ) {
                composable(Routes.SPLASH, exitTransition = { fadeOut(tween(400)) }) { SplashScreen(nav) }
                composable(Routes.ONBOARDING) { OnboardingScreen(nav) }
                composable(Routes.HOME, enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(200)) }) { HomeScreen(tabPadding) }
                composable(Routes.FILES, enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(200)) }) { FilesScreen(tabPadding) }
                composable(Routes.TOOLS, enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(200)) }) { ToolsScreen(tabPadding) }
                composable(Routes.SETTINGS, enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(200)) }) { SettingsScreen(tabPadding) }
                composable(
                    Routes.PREMIUM,
                    arguments = listOf(navArgument("source") { type = NavType.StringType; defaultValue = "app" }),
                    enterTransition = { slideInVertically(tween(420)) { it } },
                    popExitTransition = { slideOutVertically(tween(380)) { it } },
                ) { PremiumScreen(it.arguments?.getString("source") ?: "app") }
                composable(Routes.SEARCH) { SearchScreen() }
                composable(Routes.VIEWER, arguments = listOf(navArgument("uri") { type = NavType.StringType })) {
                    ViewerScreen(Uri.parse(it.arguments?.getString("uri").orEmpty()))
                }
                composable(
                    Routes.EDITOR,
                    arguments = listOf(navArgument("uri") { type = NavType.StringType }, navArgument("mode") { type = NavType.StringType; defaultValue = "annotate" }),
                ) {
                    EditorScreen(Uri.parse(it.arguments?.getString("uri").orEmpty()), it.arguments?.getString("mode") ?: "annotate")
                }
                composable(
                    Routes.TOOL,
                    arguments = listOf(
                        navArgument("tool") { type = NavType.StringType },
                        navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) {
                    val tool = runCatching { PdfTool.valueOf(it.arguments?.getString("tool").orEmpty()) }.getOrDefault(PdfTool.MERGE)
                    ToolScreen(tool, it.arguments?.getString("uri")?.let(Uri::parse))
                }
                composable(Routes.RESULT, enterTransition = { fadeIn(tween(350)) + scaleIn(initialScale = 0.94f, animationSpec = tween(350)) }) { ResultScreen() }
            }
        }
    }
}

/** App-wide feedback for Premium and billing events. */
@Composable
private fun GlobalEvents() {
    val deps = LocalAppDeps.current
    val snackbar = LocalSnackbar.current
    val reward = rememberRewardLauncher()
    LaunchedEffect(Unit) {
        deps.premium.events.collect { event ->
            when (event) {
                PremiumEvent.PassStarted -> snackbar.show("All Pro tools are unlocked and ads are off for the next 5 hours.", SnackType.PREMIUM, title = "Premium unlocked!")
                PremiumEvent.PassExtended -> snackbar.show("5 more hours added to your Premium Pass.", SnackType.PREMIUM, title = "Pass extended")
                PremiumEvent.PassExpired -> snackbar.show(
                    "Your 5-hour Premium Pass has ended.", SnackType.PREMIUM, action = "Renew",
                ) { reward.launch() }
                PremiumEvent.Purchased -> snackbar.show("Thank you! Every feature is now unlocked.", SnackType.PREMIUM, title = "Welcome to Premium")
            }
        }
    }
    LaunchedEffect(Unit) {
        deps.billing.events.collect { event ->
            when (event) {
                BillingEvent.Success -> Unit
                BillingEvent.Restored -> snackbar.show("Your Premium purchase has been restored.", SnackType.SUCCESS, title = "Restored")
                BillingEvent.NothingToRestore -> snackbar.show("No previous purchases were found for this Google account.", SnackType.INFO)
                BillingEvent.Pending -> snackbar.show("Your payment is pending. Premium unlocks as soon as it's confirmed.", SnackType.INFO, long = true)
                BillingEvent.Cancelled -> snackbar.show("Purchase cancelled.", SnackType.INFO)
                is BillingEvent.Error -> snackbar.show(event.message, SnackType.ERROR, title = "Purchase failed")
            }
        }
    }
}
