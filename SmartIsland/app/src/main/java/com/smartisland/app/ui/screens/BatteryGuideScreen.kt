package com.smartisland.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.system.Oem
import com.smartisland.app.system.OemGuides
import com.smartisland.app.system.Permissions
import com.smartisland.app.ui.components.Divider
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.ScreenTopBar
import com.smartisland.app.ui.components.SectionHeader
import com.smartisland.app.ui.components.SettingRow
import com.smartisland.app.ui.components.pressable
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.rememberPermissionStatus
import com.smartisland.app.ui.theme.Palette

@Composable
fun BatteryGuideScreen(nav: AppNavigator) {
    val context = LocalContext.current
    val snack = LocalSnack.current
    val detected = remember { OemGuides.detect() }
    var oem by remember { mutableStateOf(detected) }
    val guide = remember(oem) { OemGuides.guide(oem, context) }
    val status = rememberPermissionStatus()
    val running by IslandEngine.serviceRunning.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Keep island running", onBack = nav::back)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            GlassCard(brush = Palette.PrimaryGradient) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Smartphone, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your device", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
                        Text(detected.brand, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Battery savers on many phones close background apps. Finish these once and the island stays on screen all day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }

            SectionHeader("Health check")
            GlassCard(padding = 14.dp) {
                CheckRow("Island service", if (running) "Running" else "Not running", running)
                Divider()
                CheckRow("Battery optimization", if (status.batteryUnrestricted) "Unrestricted" else "Restricted", status.batteryUnrestricted)
                Divider()
                CheckRow("Notification access", if (status.notificationAccess) "Allowed" else "Not allowed", status.notificationAccess)
            }

            SectionHeader("Choose your brand")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Oem.entries.forEach { o -> BrandChip(o.brand, o == oem) { oem = o } }
            }

            SectionHeader("Steps for ${oem.brand}")
            GlassCard {
                guide.steps.forEachIndexed { i, step -> TimelineStep(i + 1, step, last = i == guide.steps.lastIndex) }
            }

            SectionHeader("Shortcuts")
            GlassCard(padding = 12.dp) {
                if (guide.autostartIntents.isNotEmpty()) {
                    ShortcutRow(Icons.Rounded.RocketLaunch, Palette.Primary, "Autostart", "Allow Smart Island to start by itself") {
                        if (!Permissions.launch(context, *guide.autostartIntents.toTypedArray())) snack.error("Couldn't open it. Use App info instead.")
                    }
                    Divider()
                }
                ShortcutRow(Icons.Rounded.BatterySaver, Palette.Success, "Battery optimization", "Choose \"Unrestricted\" / \"Don't optimize\"") {
                    val intents = guide.batteryIntents + Permissions.batteryOptimizationSettings()
                    Permissions.launch(context, *intents.toTypedArray())
                }
                Divider()
                ShortcutRow(Icons.Rounded.Apps, Palette.Secondary, "App info", "Permissions, battery and background data") {
                    Permissions.launch(context, Permissions.appDetails(context))
                }
                Divider()
                ShortcutRow(Icons.AutoMirrored.Rounded.OpenInNew, Palette.Warning, "More device tips", "dontkillmyapp.com guide for your brand") {
                    val slug = when (oem) {
                        Oem.XIAOMI -> "xiaomi"
                        Oem.SAMSUNG -> "samsung"
                        Oem.OPPO -> "oppo"
                        Oem.VIVO -> "vivo"
                        Oem.HUAWEI -> "huawei"
                        Oem.ASUS -> "asus"
                        Oem.OTHER -> ""
                    }
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/$slug")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(
            Modifier
                .background(Palette.Background)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PrimaryButton(
                "I've done this",
                {
                    snack.success("Great! The island will stay alive in the background")
                    nav.back()
                },
                Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Shield,
            )
        }
    }
}

@Composable
private fun CheckRow(title: String, value: String, ok: Boolean) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
            tint = if (ok) Palette.Success else Palette.Warning,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = if (ok) Palette.Success else Palette.Warning)
    }
}

@Composable
private fun BrandChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.Primary else Palette.Surface, label = "brand")
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else Palette.TextSecondary,
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else Palette.Outline, shape)
            .pressable(onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** A numbered step with a connecting line to the next one. */
@Composable
private fun TimelineStep(number: Int, text: String, last: Boolean) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Palette.PrimaryGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text("$number", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            if (!last) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Palette.Primary.copy(alpha = 0.25f)),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, bottom = if (last) 0.dp else 18.dp),
        )
    }
}

@Composable
private fun ShortcutRow(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    SettingRow(icon, color, title, subtitle, onClick = onClick)
}
