package com.pdfmaster.app.ui.tool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.ProcessingDialog
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.premium.FreeLimitSheet
import com.pdfmaster.app.ui.premium.rememberRewardLauncher
import com.pdfmaster.app.ui.rememberActivity

/** Shared progress, error, limit and completion handling for every tool. */
@Composable
fun RunStateHost(state: RunState, fromRoute: String, onReset: () -> Unit) {
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    val snackbar = LocalSnackbar.current
    val activity = rememberActivity()
    val reward = rememberRewardLauncher()
    when (state) {
        is RunState.Running -> ProcessingDialog(state.title, state.progress)
        is RunState.Failed -> LaunchedEffect(state) {
            snackbar.error(state.message, title = "Couldn't finish")
            onReset()
        }
        RunState.LimitReached -> FreeLimitSheet(
            onDismiss = onReset,
            onWatchAd = { onReset(); reward.launch() },
            onUpgrade = { onReset(); nav.premium("limit") },
        )
        RunState.Done -> LaunchedEffect(state) {
            deps.ads.showInterstitial(activity) {
                onReset()
                nav.result(fromRoute)
            }
        }
        RunState.Idle -> Unit
    }
}
