package com.inkly.notes.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.inkly.notes.AppContainer
import com.inkly.notes.ui.editor.EditorScreen
import com.inkly.notes.ui.notes.NotesScreen
import com.inkly.notes.ui.settings.SettingsScreen
import com.inkly.notes.ui.splash.SplashScreen
import com.inkly.notes.ui.welcome.WelcomeScreen

private const val SPLASH = "splash"
private const val WELCOME = "welcome"
private const val NOTES = "notes"
private const val EDITOR = "editor/{id}"
private const val SETTINGS = "settings"

private fun NavController.safePop() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack()
}

private fun NavController.safeNavigate(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}

@Composable
fun InklyNavHost(container: AppContainer, onBrandSurface: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    LaunchedEffect(entry) { onBrandSurface(entry?.destination?.route == SPLASH) }

    val ease = FastOutSlowInEasing
    NavHost(
        navController = nav,
        startDestination = SPLASH,
        enterTransition = { fadeIn(tween(280, easing = ease)) + slideInHorizontally(tween(320, easing = ease)) { it / 8 } },
        exitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(320, easing = ease)) { -it / 12 } },
        popEnterTransition = { fadeIn(tween(280, easing = ease)) + slideInHorizontally(tween(320, easing = ease)) { -it / 12 } },
        popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(320, easing = ease)) { it / 8 } },
    ) {
        composable(
            SPLASH,
            exitTransition = { fadeOut(tween(380)) + scaleOut(tween(380), targetScale = 1.08f) },
        ) {
            SplashScreen(onFinished = {
                val next = if (container.prefs.state.value.onboardingDone) NOTES else WELCOME
                nav.navigate(next) { popUpTo(SPLASH) { inclusive = true } }
            })
        }
        composable(
            WELCOME,
            enterTransition = { fadeIn(tween(420)) + scaleIn(tween(420, easing = ease), initialScale = 0.96f) },
        ) {
            WelcomeScreen(onDone = {
                container.prefs.setOnboardingDone()
                nav.navigate(NOTES) { popUpTo(WELCOME) { inclusive = true } }
            })
        }
        composable(
            NOTES,
            enterTransition = { fadeIn(tween(420)) + scaleIn(tween(420, easing = ease), initialScale = 0.96f) },
        ) {
            NotesScreen(
                container = container,
                onOpenNote = { id -> nav.safeNavigate("editor/$id") },
                onOpenSettings = { nav.safeNavigate(SETTINGS) },
            )
        }
        composable(
            EDITOR,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
            enterTransition = { fadeIn(tween(240)) + scaleIn(tween(320, easing = ease), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(260, easing = ease), targetScale = 0.92f) },
        ) { backStack ->
            EditorScreen(
                container = container,
                noteId = backStack.arguments?.getLong("id") ?: 0L,
                onBack = { nav.safePop() },
            )
        }
        composable(SETTINGS) {
            SettingsScreen(
                onBack = { nav.safePop() },
                onReplayTutorial = {
                    container.prefs.setTutorialDone(false)
                    nav.safePop()
                },
            )
        }
    }
}
