package com.flashshare.app.ui

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flashshare.app.ads.RewardResult
import com.flashshare.app.container
import com.flashshare.app.data.AppSettings
import com.flashshare.app.net.BatchStatus
import com.flashshare.app.transfer.TransferEvent
import com.flashshare.app.ui.components.GradientButton
import com.flashshare.app.ui.components.LoadingDialog
import com.flashshare.app.ui.screens.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.Perms
import com.flashshare.app.util.findActivity
import com.flashshare.app.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PROFILE = "profile?edit={edit}"
    const val HOME = "home"
    const val PERMS = "perms/{flow}?next={next}"
    const val PICKER = "picker/{mode}"
    const val SCAN = "scan/{mode}"
    const val RECEIVE = "receive"
    const val GROUP = "group"
    const val TRANSFER = "transfer"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PREMIUM = "premium"
    const val CLONE = "clone"
    const val FILES = "files"
    const val CLEANER = "cleaner"

    fun profile(edit: Boolean) = "profile?edit=$edit"
    fun picker(mode: String) = "picker/$mode"
    fun scan(mode: String) = "scan/$mode"
}

/** Navigate to [next], passing through the permissions screen first when something is missing. */
fun NavController.withPerms(ctx: Context, flow: PermFlow, next: String) {
    if (Perms.allGranted(ctx, flow)) navigate(next) { launchSingleTop = true }
    else navigate("perms/${flow.name}?next=${Uri.encode(next)}") { launchSingleTop = true }
}

/** Gate for Group Share, Phone Clone and Cleaner: Premium, or one rewarded ad per use. */
class FeatureGate(val request: (feature: String, onUnlocked: () -> Unit) -> Unit)

val LocalFeatureGate = staticCompositionLocalOf<FeatureGate> { error("no gate") }

private const val ANIM = 320

@Composable
fun AppNav(settings: AppSettings) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()

    // Interstitial after every completed transfer (free plan). At most one every 20 s for group sends.
    LaunchedEffect(Unit) {
        var last = 0L
        c.transfers.events.collect { e ->
            if (e is TransferEvent.BatchDone && e.batch.status == BatchStatus.COMPLETED && !c.prefs.current.premium) {
                val now = System.currentTimeMillis()
                if (now - last > 20_000) {
                    last = now
                    delay(1200) // let the success animation play first
                    ctx.findActivity()?.let { c.ads.showInterstitial(it) {} }
                }
            }
        }
    }

    var gateFeature by remember { mutableStateOf<String?>(null) }
    var gateAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var loadingAd by remember { mutableStateOf(false) }
    val gate = remember {
        FeatureGate { feature, onUnlocked ->
            if (c.prefs.current.premium) onUnlocked() else { gateFeature = feature; gateAction = onUnlocked }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalFeatureGate provides gate) {
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM, easing = FastOutSlowInEasing), initialOffset = { it / 4 }) + fadeIn(tween(ANIM)) },
            exitTransition = { fadeOut(tween(ANIM / 2)) },
            popEnterTransition = { fadeIn(tween(ANIM)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM, easing = FastOutSlowInEasing), targetOffset = { it / 4 }) + fadeOut(tween(ANIM)) },
        ) {
            composable(Routes.SPLASH, enterTransition = { fadeIn() }, exitTransition = { fadeOut(tween(400)) }) {
                SplashScreen {
                    val next = when {
                        !settings.onboarded -> Routes.ONBOARDING
                        !settings.profileDone -> Routes.profile(false)
                        else -> Routes.HOME
                    }
                    nav.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen {
                    c.prefs.setOnboarded()
                    nav.navigate(Routes.profile(false)) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Routes.PROFILE, arguments = listOf(navArgument("edit") { type = NavType.BoolType; defaultValue = false })) { e ->
                val edit = e.arguments?.getBoolean("edit") ?: false
                ProfileScreen(edit = edit, onBack = if (edit) ({ nav.popBackStack(); Unit }) else null) {
                    if (edit) nav.popBackStack()
                    else nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                }
            }
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(
                Routes.PERMS,
                arguments = listOf(navArgument("flow") { type = NavType.StringType }, navArgument("next") { type = NavType.StringType; nullable = true }),
            ) { e ->
                val flow = runCatching { PermFlow.valueOf(e.arguments?.getString("flow")!!) }.getOrDefault(PermFlow.ALL)
                val next = e.arguments?.getString("next")
                PermissionsScreen(flow, onBack = { nav.popBackStack() }) {
                    if (next != null) nav.navigate(next) { popUpTo(Routes.PERMS) { inclusive = true } }
                    else nav.popBackStack()
                }
            }
            composable(Routes.PICKER, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { e ->
                PickerScreen(e.arguments?.getString("mode") ?: "send", nav)
            }
            composable(Routes.SCAN, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { e ->
                ScanScreen(e.arguments?.getString("mode") ?: "send", nav)
            }
            composable(Routes.RECEIVE) { ReceiveScreen(nav) }
            composable(Routes.GROUP) { GroupHostScreen(nav) }
            composable(Routes.TRANSFER) { TransferScreen(nav) }
            composable(Routes.HISTORY) { HistoryScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
            composable(Routes.PREMIUM) { PremiumScreen(nav) }
            composable(Routes.CLONE) { CloneScreen(nav) }
            composable(Routes.FILES) { FilesScreen(nav) }
            composable(Routes.CLEANER) { CleanerScreen(nav) }
        }
    }

    gateFeature?.let { feature ->
        AlertDialog(
            onDismissRequest = { gateFeature = null },
            icon = { Icon(Icons.Rounded.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
            title = { Text("Unlock $feature") },
            text = { Text("Watch a short video to use $feature this time – or go Premium for unlimited use with no ads and uncapped speed.") },
            confirmButton = {
                GradientButton("Watch ad", onClick = {
                    val action = gateAction
                    gateFeature = null
                    val activity = ctx.findActivity() ?: return@GradientButton
                    loadingAd = true
                    scope.launch {
                        val r = c.ads.showRewarded(activity)
                        loadingAd = false
                        when (r) {
                            RewardResult.REWARDED -> action?.invoke()
                            RewardResult.SKIPPED -> ctx.toast("Watch the full video to unlock $feature")
                            RewardResult.UNAVAILABLE -> {
                                ctx.toast("No ad available right now – unlocked for you this time")
                                action?.invoke()
                            }
                        }
                    }
                }, icon = Icons.Rounded.PlayCircle, height = 46.dp)
            },
            dismissButton = {
                TextButton(onClick = { gateFeature = null; nav.navigate(Routes.PREMIUM) }) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Brand.Orange)
                    Spacer(Modifier.size(6.dp))
                    Text("Go Premium", color = Brand.Orange)
                }
            },
        )
    }
    if (loadingAd) LoadingDialog("Loading video…")
}
