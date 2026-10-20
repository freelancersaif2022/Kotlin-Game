package com.vaultly.app.ui.nav

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaultly.app.VaultlyApp
import com.vaultly.app.ui.detail.DetailScreen
import com.vaultly.app.ui.edit.EditScreen
import com.vaultly.app.ui.home.HomeScreen
import com.vaultly.app.ui.onboarding.OnboardingScreen
import com.vaultly.app.ui.paywall.PaywallScreen
import com.vaultly.app.ui.settings.PrivacyScreen
import com.vaultly.app.ui.settings.SettingsScreen
import com.vaultly.app.ui.viewer.ImageViewerScreen
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val EDIT = "edit?id={id}&image={image}"
    const val DETAIL = "detail/{id}"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
    const val VIEWER = "viewer?path={path}"
    const val PRIVACY = "privacy"

    fun edit(id: Long? = null, image: Uri? = null): String =
        "edit?id=${id ?: -1}&image=${Uri.encode(image?.toString() ?: "")}"
    fun detail(id: Long) = "detail/$id"
    fun viewer(path: String) = "viewer?path=${Uri.encode(path)}"
}

@Composable
fun VaultlyNav(app: VaultlyApp, pendingItem: MutableStateFlow<Long?>) {
    val nav = rememberNavController()
    val onboarded = app.prefs.onboarded.value
    val pending by pendingItem.collectAsStateWithLifecycle()

    LaunchedEffect(pending) {
        val id = pending ?: return@LaunchedEffect
        if (app.prefs.onboarded.value) {
            nav.navigate(Routes.detail(id)) { launchSingleTop = true }
        }
        pendingItem.value = null
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = nav,
            startDestination = if (onboarded) Routes.HOME else Routes.ONBOARDING,
            enterTransition = { slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(300)) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    app.prefs.setOnboarded()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(app, nav)
            }
            composable(
                Routes.EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("image") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                val image = entry.arguments?.getString("image")?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                EditScreen(app, id, image, onClose = { nav.popBackStack() }, onSaved = { savedId, isNew ->
                    if (isNew) {
                        nav.navigate(Routes.detail(savedId)) { popUpTo(Routes.HOME) }
                    } else {
                        nav.popBackStack()
                    }
                })
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                DetailScreen(app, entry.arguments!!.getLong("id"), nav)
            }
            composable(Routes.SETTINGS) { SettingsScreen(app, nav) }
            composable(Routes.PAYWALL) { PaywallScreen(app, onClose = { nav.popBackStack() }) }
            composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
            composable(
                Routes.VIEWER,
                arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                ImageViewerScreen(entry.arguments?.getString("path").orEmpty(), onClose = { nav.popBackStack() })
            }
        }
    }
}

fun NavHostController.openPaywall() = navigate(Routes.PAYWALL) { launchSingleTop = true }
