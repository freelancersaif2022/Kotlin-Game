package com.voxcast.mic.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voxcast.mic.VoxApp
import com.voxcast.mic.ui.components.RoundIconButton
import com.voxcast.mic.ui.theme.VoxColors

val Context.voxApp: VoxApp get() = applicationContext as VoxApp

@Composable
fun VoxTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        } else {
            Spacer(Modifier.size(44.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = VoxColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { actions() }
        }
    }
}

@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/** Haptic tick that honours the user's haptics setting. */
@Composable
fun rememberHaptic(): (HapticFeedbackType) -> Unit {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    return remember(haptic) {
        { type -> if (context.voxApp.settings.settings.value.haptics) haptic.performHapticFeedback(type) }
    }
}

fun Context.hasPermission(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

fun Context.openAppSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Ends the session flow: shows an interstitial (free users, between sessions only) then continues. */
fun Context.afterSession(next: () -> Unit) {
    val activity = findActivity()
    if (activity == null) next() else voxApp.ads.showInterstitial(activity, next)
}

fun Context.openWifiSettings() {
    runCatching { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun Context.openHotspotSettings() {
    // There is no public action for tethering; this opens the closest settings page on each OEM.
    val tether = Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings")
    runCatching { startActivity(tether.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

class PermissionRequest(
    val launch: () -> Unit,
    val showRationale: Boolean,
    val dismissRationale: () -> Unit,
)

/**
 * Requests [permissions]; calls [onGranted] when all are granted, otherwise flags
 * [PermissionRequest.showRationale] so the screen can show an explanatory dialog.
 */
@Composable
fun rememberPermissionRequest(
    permissions: List<String>,
    optional: List<String> = emptyList(),
    onGranted: () -> Unit,
): PermissionRequest {
    val context = LocalContext.current
    var rationale by remember { mutableStateOf(false) }
    val granted by rememberUpdatedState(onGranted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (permissions.all { result[it] == true || context.hasPermission(it) }) granted() else rationale = true
    }
    return PermissionRequest(
        launch = {
            val missing = (permissions + optional).filterNot { context.hasPermission(it) }
            if (missing.isEmpty()) granted() else launcher.launch(missing.toTypedArray())
        },
        showRationale = rationale,
        dismissRationale = { rationale = false },
    )
}
