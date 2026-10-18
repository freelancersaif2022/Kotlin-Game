package com.smartisland.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartisland.app.system.Permissions
import com.smartisland.app.ui.components.AppDialog
import com.smartisland.app.ui.components.GhostButton
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.IconBadge
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.ScreenTopBar
import com.smartisland.app.ui.components.StatusChip
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.navigation.Routes
import com.smartisland.app.ui.rememberPermissionStatus
import com.smartisland.app.ui.theme.Palette

private enum class Disclosure { ACCESSIBILITY, NOTIFICATIONS }

@Composable
fun PermissionsScreen(nav: AppNavigator, onDone: (() -> Unit)? = null) {
    val context = LocalContext.current
    val snack = LocalSnack.current
    val status = rememberPermissionStatus()
    var disclosure by remember { mutableStateOf<Disclosure?>(null) }
    val restricted = remember { Permissions.mayNeedRestrictedSettings(context) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Setup", onBack = if (onDone == null) nav::back else null)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Smart Island runs entirely on your phone. These permissions let it draw the island, read calls and music, and stay alive in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${status.grantedCount} of 4 granted", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
                if (status.coreReady) StatusChip("Island ready", Palette.Success)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { status.grantedCount / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Palette.Primary,
                trackColor = Palette.SurfaceHigh,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(18.dp))

            PermissionCard(
                icon = Icons.Rounded.Accessibility,
                color = Palette.Primary,
                title = "Accessibility service",
                why = "Shows the island above every app, powers the lock / power / screenshot buttons and the volume HUD.",
                granted = status.accessibility,
                required = true,
            ) { disclosure = Disclosure.ACCESSIBILITY }

            if (restricted && !status.accessibility) {
                Spacer(Modifier.height(10.dp))
                GlassCard(brush = null) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Warning, null, tint = Palette.Warning)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Android 13+ blocks this toggle for apps not installed from Play. If it's greyed out: open App info → ⋮ menu → \"Allow restricted settings\", then try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    GhostButton("Open App info", { Permissions.launch(context, Permissions.appDetails(context)) }, Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(12.dp))

            PermissionCard(
                icon = Icons.Rounded.NotificationsActive,
                color = Palette.Secondary,
                title = "Notification access",
                why = "Detects calls and music so the island can show the caller, album art and controls.",
                granted = status.notificationAccess,
                required = true,
            ) { disclosure = Disclosure.NOTIFICATIONS }
            Spacer(Modifier.height(12.dp))

            PermissionCard(
                icon = Icons.Rounded.BatterySaver,
                color = Palette.Success,
                title = "Unrestricted battery",
                why = "Stops the system from closing the island. Pick Smart Island → \"Don't optimize\" / \"Unrestricted\".",
                granted = status.batteryUnrestricted,
                required = false,
            ) {
                Permissions.launch(context, Permissions.batteryOptimizationSettings())
                snack.show("Find Smart Island and choose Unrestricted")
            }
            Spacer(Modifier.height(12.dp))

            PermissionCard(
                icon = Icons.Rounded.DoNotDisturbOn,
                color = Palette.Warning,
                title = "Do Not Disturb access",
                why = "Optional. Lets widgets and controls switch to Silent and toggle Focus.",
                granted = status.dndAccess,
                required = false,
            ) { Permissions.launch(context, Permissions.dndSettings()) }
            Spacer(Modifier.height(12.dp))

            GlassCard(onClick = { nav.go(Routes.BATTERY_GUIDE) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.BatterySaver, Palette.Gold)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Xiaomi, Samsung, OPPO, vivo…?", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                        Text("Follow the autostart guide so the island never disappears.", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (onDone != null) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                PrimaryButton(if (status.coreReady) "Continue" else "Skip for now", onDone, Modifier.fillMaxWidth())
            }
        }
    }

    when (disclosure) {
        Disclosure.ACCESSIBILITY -> AppDialog(
            onDismiss = { disclosure = null },
            icon = Icons.Rounded.Accessibility,
            title = "Use of Accessibility",
            message = "Smart Island uses the Accessibility API to display the island over other apps, perform lock screen, power menu and screenshot actions you tap, and (if you enable the Volume HUD) handle the volume keys.\n\n" +
                "It does not read screen content or collect, store or share any personal data.",
            confirmText = "Agree & open settings",
            onConfirm = {
                disclosure = null
                Permissions.launch(context, *Permissions.accessibilitySettings(context))
                snack.show("Tap Smart Island, then turn it on")
            },
            dismissText = "Not now",
        )
        Disclosure.NOTIFICATIONS -> AppDialog(
            onDismiss = { disclosure = null },
            icon = Icons.Rounded.NotificationsActive,
            title = "Notification access",
            message = "Smart Island reads call, media and (optionally) message notifications only to show them on the island. Nothing leaves your device.",
            confirmText = "Agree & open settings",
            accent = Palette.Secondary,
            onConfirm = {
                disclosure = null
                Permissions.launch(context, *Permissions.notificationAccessSettings(context))
                snack.show("Allow notification access for Smart Island")
            },
            dismissText = "Not now",
        )
        null -> Unit
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    color: Color,
    title: String,
    why: String,
    granted: Boolean,
    required: Boolean,
    onGrant: () -> Unit,
) {
    GlassCard(Modifier.animateContentSize(), onClick = if (granted) null else onGrant) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                Text(if (required) "Required" else "Recommended", style = MaterialTheme.typography.labelSmall, color = if (required) Palette.Warning else Palette.TextTertiary)
            }
            if (granted) {
                Icon(Icons.Rounded.CheckCircle, "Granted", tint = Palette.Success)
            } else {
                Text(
                    "Allow",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(color)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        if (!granted) {
            Spacer(Modifier.height(10.dp))
            Text(why, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
    }
}
