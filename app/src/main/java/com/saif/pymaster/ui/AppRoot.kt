package com.saif.pymaster.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saif.pymaster.data.AppState
import com.saif.pymaster.ui.screens.CertificateScreen
import com.saif.pymaster.ui.screens.CourseScreen
import com.saif.pymaster.ui.screens.DailyQuizScreen
import com.saif.pymaster.ui.screens.HomeScreen
import com.saif.pymaster.ui.screens.LessonScreen
import com.saif.pymaster.ui.screens.ModuleQuizScreen
import com.saif.pymaster.ui.screens.ModuleScreen
import com.saif.pymaster.ui.screens.NotificationsScreen
import com.saif.pymaster.ui.screens.OnboardingScreen
import com.saif.pymaster.ui.screens.ProfileSetupScreen
import com.saif.pymaster.ui.screens.ProjectDetailScreen
import com.saif.pymaster.ui.screens.ProjectsScreen
import com.saif.pymaster.ui.screens.SettingsScreen
import com.saif.pymaster.ui.screens.SplashScreen
import com.saif.pymaster.ui.screens.TerminalScreen
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.ui.theme.PyMasterTheme
import com.saif.pymaster.ui.theme.ThemeVariant
import com.saif.pymaster.util.LocalHaptics
import com.saif.pymaster.util.rememberHaptics
import kotlinx.coroutines.delay

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SETUP = "setup"
    const val HOME = "home"
    const val COURSE = "course"
    const val TERMINAL = "terminal"
    const val SETTINGS = "settings"
    const val MODULE = "module/{id}"
    const val LESSON = "lesson/{id}"
    const val QUIZ = "quiz/{id}"
    const val DAILY_QUIZ = "daily_quiz"
    const val NOTIFICATIONS = "notifications"
    const val PROJECTS = "projects"
    const val PROJECT = "project/{id}"
    const val CERTIFICATE = "certificate"
    const val EDIT_PROFILE = "edit_profile"

    fun module(id: String) = "module/$id"
    fun lesson(id: String) = "lesson/$id"
    fun quiz(id: String) = "quiz/$id"
    fun project(id: String) = "project/$id"

    val tabs = listOf(HOME, COURSE, TERMINAL, SETTINGS)
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabItems = listOf(
    TabItem(Routes.HOME, "Home", Icons.Rounded.Home),
    TabItem(Routes.COURSE, "Course", Icons.AutoMirrored.Rounded.MenuBook),
    TabItem(Routes.TERMINAL, "Terminal", Icons.Rounded.Terminal),
    TabItem(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
)

/** Navigation helpers shared by all screens. */
class Navigator(private val nav: NavHostController) {
    fun to(route: String) = nav.navigate(route) { launchSingleTop = true }
    fun back() {
        nav.popBackStack()
    }

    fun tab(route: String) = nav.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun replaceAll(route: String) = nav.navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppRoot(pendingOpen: String?, onPendingConsumed: () -> Unit) {
    val vm: MainViewModel = viewModel()
    val terminalVm: TerminalViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state
    val settings = s?.settings
    PyMasterTheme(
        variant = ThemeVariant.of(settings?.theme ?: "navy"),
        fontScale = settings?.fontScale ?: 1f,
    ) {
        val haptics = rememberHaptics(settings?.vibration ?: true)
        CompositionLocalProvider(LocalHaptics provides haptics) {
            Box(Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
                AppNavHost(s, vm, terminalVm, pendingOpen, onPendingConsumed)
            }
        }
    }
}

@Composable
private fun AppNavHost(
    state: AppState?,
    vm: MainViewModel,
    terminalVm: TerminalViewModel,
    pendingOpen: String?,
    onPendingConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val navigator = remember(nav) { Navigator(nav) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showBar = route in Routes.tabs && !imeVisible

    // Open the screen a notification asked for, once the user is inside the app.
    LaunchedEffect(pendingOpen, route, state?.profile) {
        if (pendingOpen != null && state?.profile != null && route != null && route != Routes.SPLASH) {
            when (pendingOpen) {
                "daily_quiz" -> navigator.to(Routes.DAILY_QUIZ)
                else -> navigator.to(Routes.NOTIFICATIONS)
            }
            onPendingConsumed()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
                enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
            ) {
                composable(Routes.SPLASH, enterTransition = { EnterTransition.None }, exitTransition = { fadeOut() }) {
                    SplashScreen(state) { target -> navigator.replaceAll(target) }
                }
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinish = {
                        vm.finishOnboarding()
                        navigator.replaceAll(Routes.SETUP)
                    })
                }
                composable(Routes.SETUP) {
                    ProfileSetupScreen(initial = null, onSave = { profile ->
                        vm.saveProfile(profile, isNew = true)
                        navigator.replaceAll(Routes.HOME)
                    })
                }
                composable(Routes.EDIT_PROFILE) {
                    ProfileSetupScreen(initial = state?.profile, onBack = navigator::back, onSave = { profile ->
                        vm.saveProfile(profile, isNew = false)
                        navigator.back()
                    })
                }
                tab(Routes.HOME) { HomeScreen(state, navigator) }
                tab(Routes.COURSE) { CourseScreen(state, navigator) }
                tab(Routes.TERMINAL) {
                    TrackStudyTime(vm)
                    TerminalScreen(state, terminalVm)
                }
                tab(Routes.SETTINGS) { SettingsScreen(state, vm, navigator) }
                composable(Routes.MODULE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    ModuleScreen(state, it.arguments?.getString("id") ?: "", navigator)
                }
                composable(Routes.LESSON, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    LessonScreen(
                        state, it.arguments?.getString("id") ?: "", vm, navigator,
                        onRunCode = { code ->
                            terminalVm.loadCode(code, run = true)
                            navigator.tab(Routes.TERMINAL)
                        },
                    )
                }
                composable(
                    Routes.QUIZ,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = { slideInVertically { it / 8 } + fadeIn() },
                    popExitTransition = { slideOutVertically { it / 8 } + fadeOut() },
                ) {
                    ModuleQuizScreen(state, it.arguments?.getString("id") ?: "", vm, navigator)
                }
                composable(Routes.DAILY_QUIZ) { DailyQuizScreen(state, vm, navigator) }
                composable(Routes.NOTIFICATIONS) { NotificationsScreen(state, vm, navigator) }
                composable(Routes.PROJECTS) { ProjectsScreen(state, navigator) }
                composable(Routes.PROJECT, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    ProjectDetailScreen(
                        state, it.arguments?.getString("id") ?: "", vm, navigator,
                        onRunCode = { code ->
                            terminalVm.loadCode(code, run = true)
                            navigator.tab(Routes.TERMINAL)
                        },
                    )
                }
                composable(Routes.CERTIFICATE) { CertificateScreen(state, navigator) }
            }
        }
        AnimatedVisibility(
            visible = showBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            BottomBar(route) { navigator.tab(it) }
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.tab(route: String, content: @Composable () -> Unit) {
    composable(
        route,
        enterTransition = { fadeIn() },
        exitTransition = { if (targetState.destination.route in Routes.tabs) fadeOut() else ExitTransition.None },
        popEnterTransition = { fadeIn() },
    ) { content() }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    val colors = LocalAppColors.current
    val haptics = LocalHaptics.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.stroke))
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabItems.forEach { item ->
                val selected = current == item.route
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!selected) {
                                haptics?.tick()
                                onSelect(item.route)
                            }
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(50))
                            .then(
                                if (selected) Modifier
                                    .background(Brush.horizontalGradient(listOf(Brand.PythonBlue.copy(alpha = 0.55f), Brand.Cyan.copy(alpha = 0.35f))))
                                    .border(1.dp, Brand.Cyan.copy(alpha = 0.35f), RoundedCornerShape(50))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, item.label, tint = if (selected) Brand.White else Brand.TextMuted, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Brand.White else Brand.TextMuted,
                    )
                }
            }
        }
    }
}

/** Adds study time while the screen is visible and the app is in the foreground. */
@Composable
fun TrackStudyTime(vm: MainViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(10_000)
                vm.addStudySeconds(10)
            }
        }
    }
}

