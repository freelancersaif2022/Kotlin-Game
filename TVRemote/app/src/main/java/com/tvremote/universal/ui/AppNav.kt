package com.tvremote.universal.ui

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tvremote.universal.App
import com.tvremote.universal.data.DiscoveredTv
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.ui.screens.DeviceSettingsScreen
import com.tvremote.universal.ui.screens.DiscoverScreen
import com.tvremote.universal.ui.screens.HelpScreen
import com.tvremote.universal.ui.screens.HomeScreen
import com.tvremote.universal.ui.screens.ManualAddScreen
import com.tvremote.universal.ui.screens.OnboardingScreen
import com.tvremote.universal.ui.screens.PairScreen
import com.tvremote.universal.ui.screens.PremiumScreen
import com.tvremote.universal.ui.screens.RemoteScreen
import com.tvremote.universal.ui.screens.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val MANUAL = "manual"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"

    fun pair(tv: DiscoveredTv, deviceId: String? = null): String =
        "pair?platform=${tv.platform.name}&host=${Uri.encode(tv.host)}&name=${Uri.encode(tv.name)}" +
            "&model=${Uri.encode(tv.model)}&mac=${Uri.encode(tv.mac)}&deviceId=${Uri.encode(deviceId.orEmpty())}"

    fun remote(id: String) = "remote/$id"
    fun device(id: String) = "device/$id"
    fun help(platform: TvPlatform? = null) = "help?platform=${platform?.name.orEmpty()}"
}

/** Navigates to the add-TV flow, or to the paywall when the free TV slot is used. */
fun NavHostController.addTv() {
    val c = App.container
    if (c.tvStore.devices.value.isNotEmpty() && !c.premium.isPremium.value) navigate(Routes.PREMIUM)
    else navigate(Routes.DISCOVER)
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val container = App.container
    val activity = LocalActivity.current
    val start = if (container.settings.onboardingDone.value) Routes.HOME else Routes.ONBOARDING
    var restored by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Jump straight back to the last used remote for a fast start.
        if (!restored && start == Routes.HOME) {
            restored = true
            val last = container.settings.lastDeviceId.value
            if (last != null && container.tvStore.get(last) != null) nav.navigate(Routes.remote(last))
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { it / 10 } },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                container.settings.setOnboardingDone()
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                nav.navigate(Routes.DISCOVER)
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpen = { id ->
                    val switching = container.settings.lastDeviceId.value != id
                    val go = { nav.navigate(Routes.remote(id)) }
                    if (switching && container.tvStore.devices.value.size > 1) container.ads.showInterstitial(activity) { go() } else go()
                },
                onAdd = { nav.addTv() },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onPremium = { nav.navigate(Routes.PREMIUM) },
                onEdit = { id -> nav.navigate(Routes.device(id)) },
            )
        }
        composable(Routes.DISCOVER) {
            DiscoverScreen(
                onBack = { nav.popBackStack() },
                onSelect = { tv ->
                    val existing = container.tvStore.findByHost(tv.platform, tv.host)
                    nav.navigate(Routes.pair(tv, existing?.id))
                },
                onManual = { nav.navigate(Routes.MANUAL) },
                onHelp = { nav.navigate(Routes.help()) },
            )
        }
        composable(Routes.MANUAL) {
            ManualAddScreen(
                onBack = { nav.popBackStack() },
                onContinue = { tv -> nav.navigate(Routes.pair(tv, container.tvStore.findByHost(tv.platform, tv.host)?.id)) },
            )
        }
        composable(
            "pair?platform={platform}&host={host}&name={name}&model={model}&mac={mac}&deviceId={deviceId}",
            arguments = listOf("platform", "host", "name", "model", "mac", "deviceId").map {
                navArgument(it) { type = NavType.StringType; defaultValue = "" }
            },
        ) { entry ->
            val args = entry.arguments
            val tv = DiscoveredTv(
                platform = TvPlatform.valueOf(args?.getString("platform").orEmpty().ifBlank { TvPlatform.ANDROID_TV.name }),
                host = args?.getString("host").orEmpty(),
                name = args?.getString("name").orEmpty(),
                model = args?.getString("model").orEmpty(),
                mac = args?.getString("mac").orEmpty(),
            )
            PairScreen(
                tv = tv,
                existingId = args?.getString("deviceId")?.takeIf { it.isNotBlank() },
                onBack = { nav.popBackStack() },
                onHelp = { nav.navigate(Routes.help(tv.platform)) },
                onPaired = { id ->
                    container.settings.setLastDevice(id)
                    container.ads.showInterstitial(activity) {
                        nav.navigate(Routes.remote(id)) { popUpTo(Routes.HOME) }
                    }
                },
            )
        }
        composable(Routes.remote("{id}"), arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            RemoteScreen(
                deviceId = id,
                onBack = {
                    if (!nav.popBackStack(Routes.HOME, inclusive = false)) {
                        nav.navigate(Routes.HOME) { popUpTo(0) }
                    }
                },
                onPremium = { nav.navigate(Routes.PREMIUM) },
                onDeviceSettings = { nav.navigate(Routes.device(id)) },
                onRepair = { tv -> nav.navigate(Routes.pair(tv, id)) },
                onHelp = { platform -> nav.navigate(Routes.help(platform)) },
                onSwitchTv = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
            )
        }
        composable(Routes.PREMIUM) {
            PremiumScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val back: () -> Unit = { container.ads.showInterstitial(activity) { nav.popBackStack() } }
            SettingsScreen(
                onBack = back,
                onPremium = { nav.navigate(Routes.PREMIUM) },
                onHelp = { nav.navigate(Routes.help()) },
                onManageTvs = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(
            "help?platform={platform}",
            arguments = listOf(navArgument("platform") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            val p = entry.arguments?.getString("platform").orEmpty()
            HelpScreen(
                initial = TvPlatform.entries.firstOrNull { it.name == p },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.device("{id}"), arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            DeviceSettingsScreen(
                deviceId = id,
                onBack = { nav.popBackStack() },
                onRepair = { tv -> nav.navigate(Routes.pair(tv, id)) },
                onDeleted = {
                    if (container.settings.lastDeviceId.value == id) container.settings.setLastDevice(null)
                    nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onHelp = { platform -> nav.navigate(Routes.help(platform)) },
            )
        }
    }
}
