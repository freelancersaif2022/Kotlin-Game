package com.smartisland.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.system.Permissions
import com.smartisland.app.system.SystemActions
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.AppSwitch
import com.smartisland.app.ui.components.Demo
import com.smartisland.app.ui.components.GetProButton
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.IconBadge
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.ProActiveBadge
import com.smartisland.app.ui.components.ProBadge
import com.smartisland.app.ui.components.SectionHeader
import com.smartisland.app.ui.components.StatusChip
import com.smartisland.app.ui.components.TabHeader
import com.smartisland.app.ui.components.pressable
import com.smartisland.app.ui.components.tabBarPadding
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.navigation.Routes
import com.smartisland.app.ui.rememberBattery
import com.smartisland.app.ui.rememberPermissionStatus
import com.smartisland.app.ui.reportAction
import com.smartisland.app.ui.theme.Palette
import java.util.Calendar

@Composable
fun HomeScreen(nav: AppNavigator) {
    val container = appContainer()
    val context = LocalContext.current
    val snack = LocalSnack.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val premium by container.premium.state.collectAsStateWithLifecycle()
    val running by IslandEngine.serviceRunning.collectAsStateWithLifecycle()
    val perms = rememberPermissionStatus()
    val battery = rememberBattery()

    fun requirePremium(block: () -> Unit) {
        if (premium.isPremium) block() else nav.go(Routes.PAYWALL)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .tabBarPadding(),
    ) {
        TabHeader(
            title = "Smart Island",
            subtitle = greeting(),
            trailing = {
                if (premium.isPremium) {
                    ProActiveBadge(onClick = { nav.go(Routes.SETTINGS) })
                } else {
                    GetProButton({ nav.go(Routes.PAYWALL) })
                }
            },
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            StatusCard(
                ready = perms.coreReady,
                running = running,
                enabled = settings.enabled,
                onSetup = { nav.go(Routes.PERMISSIONS) },
                onRestart = {
                    Permissions.expectReturn(context)
                    Permissions.launch(context, *Permissions.accessibilitySettings(context))
                },
                onToggle = { on -> container.settings.update { it.copy(enabled = on) } },
            )

            SectionHeader("Try a live activity")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DemoChip("Music", Icons.Rounded.MusicNote, Color(0xFFFF375F)) { IslandEngine.showTransient(Demo.media(), 7000) }
                DemoChip("Call", Icons.Rounded.Call, Palette.Success) { IslandEngine.showTransient(Demo.incomingCall(), 6000) }
                DemoChip("Charging", Icons.Rounded.BatteryChargingFull, Palette.Success) { IslandEngine.showTransient(Demo.charging(), 3500) }
                DemoChip("Alert", Icons.Rounded.Notifications, Palette.Secondary) { IslandEngine.showTransient(Demo.notice(), 4500) }
                DemoChip("Volume", Icons.AutoMirrored.Rounded.VolumeUp, Palette.PrimarySoft) { IslandEngine.showTransient(Demo.volume(), 2000) }
                DemoChip("Silent", Icons.Rounded.NotificationsOff, Palette.Error) { IslandEngine.showTransient(Demo.silent(), 2200) }
            }
            AnimatedVisibility(!running) {
                Text(
                    "Finish setup to see these on top of every app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextTertiary,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }

            SectionHeader("Quick toggles")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleTile(
                    "Volume HUD", "Island volume bar", Icons.AutoMirrored.Rounded.VolumeUp, Palette.Secondary,
                    settings.replaceVolumePanel, locked = !premium.isPremium, Modifier.weight(1f),
                ) { requirePremium { container.settings.update { it.copy(replaceVolumePanel = !it.replaceVolumePanel) } } }
                ToggleTile(
                    "Edge panel", "Power & volume", Icons.Rounded.VerticalSplit, Palette.Warning,
                    settings.edgePanelEnabled, locked = !premium.isPremium, Modifier.weight(1f),
                ) { requirePremium { container.settings.update { it.copy(edgePanelEnabled = !it.edgePanelEnabled) } } }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleTile(
                    "Alerts", "Notifications on island", Icons.Rounded.Notifications, Palette.Primary,
                    settings.showNotifications, locked = !premium.isPremium, Modifier.weight(1f),
                ) { requirePremium { container.settings.update { it.copy(showNotifications = !it.showNotifications) } } }
                ToggleTile(
                    "Glow", "Colored island glow", Icons.Rounded.AutoAwesome, Palette.Cyan,
                    settings.glow, locked = !premium.isPremium, Modifier.weight(1f),
                ) { requirePremium { container.settings.update { it.copy(glow = !it.glow) } } }
            }

            SectionHeader("Power buttons")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val setup = { nav.go(Routes.PERMISSIONS) }
                ActionTile("Lock", Icons.Rounded.Lock, Palette.Primary, Modifier.weight(1f)) {
                    reportAction(SystemActions.lockScreen(), snack, setup)
                }
                ActionTile("Power", Icons.Rounded.PowerSettingsNew, Palette.Error, Modifier.weight(1f)) {
                    reportAction(SystemActions.powerMenu(), snack, setup)
                }
                ActionTile("Screenshot", Icons.Rounded.CropFree, Palette.Secondary, Modifier.weight(1f)) {
                    reportAction(SystemActions.screenshot(), snack, setup)
                }
            }

            SectionHeader("Battery")
            BatteryCard(battery.level, battery.isCharging, battery.isFast, battery.watts, battery.temperatureC, battery.healthLabel)

        }
    }
}

@Composable
private fun StatusCard(ready: Boolean, running: Boolean, enabled: Boolean, onSetup: () -> Unit, onRestart: () -> Unit, onToggle: (Boolean) -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                when {
                    !ready -> StatusChip("Setup required", Palette.Warning)
                    !enabled -> StatusChip("Island paused", Palette.TextSecondary)
                    running -> StatusChip("Island is live", Palette.Success)
                    else -> StatusChip("Island stopped", Palette.Error)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        !ready -> "Grant 2 permissions to show the island over every app."
                        !enabled -> "Turn the island back on any time."
                        !running -> "Android stopped the island service. Turn Smart Island off and on in Accessibility to restart it."
                        else -> "Calls, music and charging appear automatically."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (ready) AppSwitch(enabled, onToggle)
        }
        if (!ready) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Finish setup", onSetup, Modifier.fillMaxWidth())
        } else if (enabled && !running) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Restart island", onRestart, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DemoChip(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .pressable(onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.TextPrimary)
    }
}

@Composable
private fun ToggleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    on: Boolean,
    locked: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val active = on && !locked
    GlassCard(modifier, onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, if (active) color else Palette.TextSecondary)
            Spacer(Modifier.weight(1f))
            if (locked) ProBadge() else StatusChip(if (on) "On" else "Off", if (on) Palette.Success else Palette.TextTertiary)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, onClick = onClick, padding = 14.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextPrimary)
        }
    }
}

@Composable
private fun BatteryCard(level: Int, charging: Boolean, fast: Boolean, watts: Float, temp: Float, health: String) {
    val color = when {
        charging -> Palette.Success
        level <= 20 -> Palette.Error
        level <= 40 -> Palette.Warning
        else -> Palette.Success
    }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        fast -> "Fast charging"
                        charging -> "Charging"
                        else -> "On battery"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                )
                Text("$level%", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = Palette.TextPrimary)
            }
            com.smartisland.app.island.ui.BatteryGlyph(level, color, Modifier.size(width = 74.dp, height = 34.dp), charging = charging)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Power", if (charging && watts >= 1f) "%.1f W".format(watts) else "—", Modifier.weight(1f))
            Stat("Temp", "%.1f°C".format(temp), Modifier.weight(1f))
            Stat("Health", health, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.SurfaceHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.TextSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, maxLines = 1)
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
