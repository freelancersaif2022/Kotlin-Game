package com.smartisland.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.RocketLaunch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartisland.app.system.Oem
import com.smartisland.app.system.OemGuides
import com.smartisland.app.system.Permissions
import com.smartisland.app.ui.components.GhostButton
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.ScreenTopBar
import com.smartisland.app.ui.components.SectionHeader
import com.smartisland.app.ui.components.StatusChip
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
                    Icon(Icons.Rounded.Smartphone, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Detected: ${detected.brand}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            "Some phones close background apps to save battery. Do these steps once.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            SectionHeader("Brand")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Oem.entries.forEach { o ->
                    val selected = o == oem
                    Text(
                        o.brand,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else Palette.TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Palette.Primary else Palette.Surface)
                            .pressable({ oem = o })
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            SectionHeader("Steps")
            GlassCard {
                guide.steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Palette.Primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = Palette.PrimarySoft)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }

            SectionHeader("Shortcuts")
            GlassCard {
                if (guide.autostartIntents.isNotEmpty()) {
                    PrimaryButton("Open Autostart settings", {
                        if (!Permissions.launch(context, *guide.autostartIntents.toTypedArray())) snack.error("Couldn't open it. Use App info instead.")
                    }, Modifier.fillMaxWidth(), icon = Icons.Rounded.RocketLaunch)
                    Spacer(Modifier.height(10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GhostButton(
                        "Battery optimization",
                        {
                            val intents = guide.batteryIntents + Permissions.batteryOptimizationSettings()
                            Permissions.launch(context, *intents.toTypedArray())
                        },
                        Modifier.weight(1f),
                        Icons.Rounded.BatterySaver,
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(if (status.batteryUnrestricted) "Done" else "To do", if (status.batteryUnrestricted) Palette.Success else Palette.Warning)
                }
                Spacer(Modifier.height(10.dp))
                GhostButton("App info", { Permissions.launch(context, Permissions.appDetails(context)) }, Modifier.fillMaxWidth(), Icons.Rounded.Apps)
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    "More tips on dontkillmyapp.com",
                    {
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
                    },
                    Modifier.fillMaxWidth(),
                    Icons.AutoMirrored.Rounded.OpenInNew,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            PrimaryButton("I've done this", {
                snack.success("Great! The island will stay alive in the background")
                nav.back()
            }, Modifier.fillMaxWidth())
        }
    }
}
