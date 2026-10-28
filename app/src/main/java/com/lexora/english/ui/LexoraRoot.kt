package com.lexora.english.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.ui.practice.PracticeSessionScreen
import com.lexora.english.ui.screens.CertificateScreen
import com.lexora.english.ui.screens.DailyQuizScreen
import com.lexora.english.ui.screens.LessonScreen
import com.lexora.english.ui.screens.MainScreen
import com.lexora.english.ui.screens.ModuleQuizScreen
import com.lexora.english.ui.screens.ModuleScreen
import com.lexora.english.ui.screens.NotificationsScreen
import com.lexora.english.ui.screens.OnboardingScreen
import com.lexora.english.ui.screens.ProfileSetupScreen
import com.lexora.english.ui.screens.ProjectScreen
import com.lexora.english.ui.screens.SplashScreen
import com.lexora.english.ui.theme.Lex

class Nav(private val controller: NavHostController) {
    private fun go(route: String) {
        controller.navigate(route) { launchSingleTop = true }
    }

    fun back() {
        controller.popBackStack()
    }

    fun notifications() = go("notifications")
    fun module(id: String) = go("module/$id")
    fun lesson(id: String) = go("lesson/$id")
    fun moduleQuiz(id: String) = go("moduleQuiz/$id")
    fun daily() = go("daily")
    fun project(id: String) = go("project/$id")
    fun certificate() = go("certificate")
    fun practice(mode: String) = go("practice/$mode")
    fun editProfile() = go("profileEdit")

    fun replaceAll(route: String) {
        controller.navigate(route) {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

private const val ANIM = 320

@Composable
fun LexoraRoot(state: AppState, openNotifications: Boolean, onNotificationsOpened: () -> Unit) {
    val controller = rememberNavController()
    val nav = remember(controller) { Nav(controller) }
    val repo = AppRepository.get(LocalContext.current)

    LaunchedEffect(openNotifications, state.profile != null) {
        if (openNotifications && state.profile != null && controller.currentDestination?.route != "splash") {
            nav.notifications()
            onNotificationsOpened()
        }
    }

    NavHost(
        navController = controller,
        startDestination = "splash",
        modifier = Modifier
            .fillMaxSize()
            .background(Lex.colors.background),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM), initialOffset = { it / 4 }) + fadeIn(tween(ANIM))
        },
        exitTransition = { fadeOut(tween(ANIM / 2)) },
        popEnterTransition = { fadeIn(tween(ANIM)) },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM), targetOffset = { it / 4 }) + fadeOut(tween(ANIM))
        },
    ) {
        composable("splash", enterTransition = { fadeIn() }, exitTransition = { fadeOut(tween(400)) }) {
            SplashScreen(onFinished = {
                val s = repo.current
                val target = when {
                    !s.onboardingDone -> "onboarding"
                    s.profile == null -> "setup"
                    else -> "main"
                }
                nav.replaceAll(target)
                if (target == "main" && openNotifications) {
                    nav.notifications()
                    onNotificationsOpened()
                }
            })
        }
        composable("onboarding") {
            OnboardingScreen(onDone = {
                repo.completeOnboarding()
                nav.replaceAll("setup")
            })
        }
        composable("setup") {
            ProfileSetupScreen(existing = null, onSaved = { nav.replaceAll("main") }, onBack = null)
        }
        composable("profileEdit") {
            ProfileSetupScreen(existing = state.profile, onSaved = { nav.back() }, onBack = { nav.back() })
        }
        composable("main", enterTransition = { fadeIn(tween(500)) }) {
            MainScreen(state, nav)
        }
        composable("notifications") { NotificationsScreen(state, nav) }
        composable("module/{id}") { entry ->
            ModuleScreen(entry.arguments?.getString("id").orEmpty(), state, nav)
        }
        composable("lesson/{id}") { entry ->
            LessonScreen(entry.arguments?.getString("id").orEmpty(), state, nav)
        }
        composable("moduleQuiz/{id}") { entry ->
            ModuleQuizScreen(entry.arguments?.getString("id").orEmpty(), nav)
        }
        composable("daily") { DailyQuizScreen(state, nav) }
        composable("project/{id}") { entry ->
            ProjectScreen(entry.arguments?.getString("id").orEmpty(), state, nav)
        }
        composable("certificate") { CertificateScreen(state, nav) }
        composable("practice/{mode}") { entry ->
            PracticeSessionScreen(entry.arguments?.getString("mode").orEmpty(), state, nav)
        }
    }
}

/** Counts time spent on a study screen while the app is in the foreground. */
@Composable
fun StudyTimer() {
    val repo = AppRepository.get(LocalContext.current)
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        var start = if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) System.currentTimeMillis() else 0L
        fun flush() {
            if (start > 0L) {
                repo.addStudyTime((System.currentTimeMillis() - start) / 1000)
                start = 0L
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (start == 0L) start = System.currentTimeMillis()
                Lifecycle.Event.ON_PAUSE -> flush()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            flush()
        }
    }
}
