package com.eyescroll.app.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.eyescroll.app.ui.components.GhostButton
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.GradientButton
import com.eyescroll.app.ui.components.StepBadge
import com.eyescroll.app.ui.theme.EsColors

@Composable
fun SetupScreen(onCalibrate: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val holder = rememberSetupState()
    val state = holder.state.value
    var cameraDenials by rememberSaveable { mutableStateOf(0) }
    var showA11yDisclosure by remember { mutableStateOf(false) }
    var showA11yGuide by rememberSaveable { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) cameraDenials++
        holder.refresh()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { holder.refresh() }

    val progress by animateFloatAsState(state.doneCount / 4f, tween(600), label = "progress")

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text("Quick setup", style = MaterialTheme.typography.headlineMedium, color = EsColors.Text, modifier = Modifier.enterAnimation(0))
            Spacer(Modifier.height(6.dp))
            Text(
                "Four short steps. Each one explains exactly why it's needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = EsColors.TextMuted,
                modifier = Modifier.enterAnimation(1),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.07f)).enterAnimation(1),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(50)).background(EsColors.Accent))
            }
            Text(
                "${state.doneCount} of 4 done",
                style = MaterialTheme.typography.labelMedium,
                color = EsColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            // 1. Camera
            StepCard(
                index = 1,
                done = state.camera,
                icon = Icons.Filled.Face,
                title = "Camera",
                body = "Used only while eye scrolling is ON, to see where your eyes look. Processed on your phone, never saved or uploaded.",
                action = if (cameraDenials >= 2) "Open app settings" else "Allow camera",
                modifier = Modifier.enterAnimation(2),
            ) {
                val activity = context as? Activity
                val permanentlyDenied = cameraDenials >= 1 && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                if (cameraDenials >= 2 || permanentlyDenied) openAppDetails(context)
                else cameraLauncher.launch(Manifest.permission.CAMERA)
            }

            // 2. Notifications
            StepCard(
                index = 2,
                done = state.notifications,
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                tag = "Recommended",
                body = "Shows a small notification while the camera is on, with a one-tap \"Turn off\" button.",
                action = "Allow notifications",
                modifier = Modifier.enterAnimation(3),
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // 3. Accessibility
            StepCard(
                index = 3,
                done = state.accessibility,
                icon = Icons.Filled.Settings,
                title = "Accessibility service",
                body = "Lets EyeScroll do the scroll swipe for you in any app and show the floating on/off dot. It does not read your screen or typing.",
                action = "Turn on in Settings",
                modifier = Modifier.enterAnimation(4),
                secondary = if (!state.accessibility) {
                    { showA11yGuide = !showA11yGuide }
                } else null,
                secondaryLabel = if (showA11yGuide) "Hide guide" else "Show me how",
                extra = {
                    AnimatedVisibility(
                        visible = showA11yGuide && !state.accessibility,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) { AccessibilityGuide(onOpenAppInfo = { openAppDetails(context) }) }
                },
            ) { showA11yDisclosure = true }

            // 4. Calibration
            StepCard(
                index = 4,
                done = state.calibrated,
                icon = Icons.Filled.PlayArrow,
                title = "Calibration · 10 seconds",
                body = "Look at a dot at the top, middle and bottom. This teaches EyeScroll your eyes, face and how you hold the phone.",
                action = if (state.calibrated) "Calibrate again" else "Start calibration",
                actionEnabled = state.camera,
                showActionWhenDone = true,
                modifier = Modifier.enterAnimation(5),
            ) { onCalibrate() }

            GlassCard(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp).enterAnimation(6), padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = EsColors.Success, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Your privacy: the camera is used only while eye scrolling is on. No photos, no video, no internet use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EsColors.TextMuted,
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            GradientButton(
                text = if (state.requiredDone) "All set — let's go" else "Finish the steps above",
                onClick = onDone,
                enabled = state.requiredDone,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!state.requiredDone) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "I'll finish later",
                    style = MaterialTheme.typography.labelLarge,
                    color = EsColors.TextMuted,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(10.dp))
                        .clickableNoRipple(onDone)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (showA11yDisclosure) {
        AlertDialog(
            onDismissRequest = { showA11yDisclosure = false },
            containerColor = EsColors.SurfaceHigh,
            icon = { Icon(Icons.Filled.Info, null, tint = EsColors.Primary) },
            title = { Text("Accessibility permission", color = EsColors.Text) },
            text = {
                Text(
                    "EyeScroll uses Android's Accessibility API to:\n\n" +
                        "• perform scroll swipes for you when you look at the top or bottom of the screen\n" +
                        "• show the floating on/off dot above other apps\n\n" +
                        "It does NOT read the content of your screen, your messages, typing or passwords, and it sends no data anywhere.\n\n" +
                        "On the next screen tap \"EyeScroll\" and switch it on.",
                    color = EsColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showA11yDisclosure = false
                    showA11yGuide = true
                    openAccessibilitySettings(context)
                }) { Text("Agree & open settings", color = EsColors.Primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showA11yDisclosure = false }) { Text("Not now", color = EsColors.TextMuted) }
            },
        )
    }
}

@Composable
private fun StepCard(
    index: Int,
    done: Boolean,
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    actionEnabled: Boolean = true,
    showActionWhenDone: Boolean = false,
    secondary: (() -> Unit)? = null,
    secondaryLabel: String = "",
    extra: @Composable () -> Unit = {},
    onAction: () -> Unit,
) {
    GlassCard(modifier.fillMaxWidth().padding(bottom = 12.dp).animateContentSize(), highlighted = !done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepBadge(index, done)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = EsColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
                }
                if (tag != null && !done) {
                    Text(tag, style = MaterialTheme.typography.labelSmall, color = EsColors.Warning, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (done) {
                Text("Done", style = MaterialTheme.typography.labelMedium, color = EsColors.Success)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = EsColors.TextMuted)
        extra()
        if (!done || showActionWhenDone) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (done) {
                    GhostButton(action, onAction, Modifier.weight(1f))
                } else {
                    GradientButton(action, onAction, Modifier.weight(1f), enabled = actionEnabled)
                }
                if (secondary != null) {
                    GhostButton(secondaryLabel, secondary)
                }
            }
        }
    }
}

@Composable
private fun AccessibilityGuide(onOpenAppInfo: () -> Unit) {
    Column(Modifier.padding(top = 14.dp)) {
        val steps = listOf(
            "Tap \"Agree & open settings\" below",
            "Find \"Installed apps\" or \"Downloaded apps\"",
            "Tap \"EyeScroll – hands-free scrolling\"",
            "Switch \"Use EyeScroll\" ON and tap Allow",
            "Come back here — this step turns green",
        )
        steps.forEachIndexed { i, s ->
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(EsColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = EsColors.Primary) }
                Spacer(Modifier.width(12.dp))
                Text(s, style = MaterialTheme.typography.bodyMedium, color = EsColors.Text)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(EsColors.Warning.copy(alpha = 0.08f)).padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = EsColors.Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Says \"Restricted setting\"?", style = MaterialTheme.typography.titleMedium, color = EsColors.Warning)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Android 13+ protects apps installed from an APK file. Open App info → tap ⋮ (top-right) → \"Allow restricted settings\", then switch EyeScroll on again.",
                style = MaterialTheme.typography.bodySmall,
                color = EsColors.TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            GhostButton("Open App info", onOpenAppInfo, Modifier.fillMaxWidth())
        }
    }
}
