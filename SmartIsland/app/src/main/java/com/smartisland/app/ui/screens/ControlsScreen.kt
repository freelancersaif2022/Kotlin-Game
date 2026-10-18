package com.smartisland.app.ui.screens

import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.smartisland.app.system.Permissions
import com.smartisland.app.system.SystemActions
import com.smartisland.app.system.VolumeStream
import com.smartisland.app.ui.SystemBroadcastEffect
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.SectionHeader
import com.smartisland.app.ui.components.SliderRow
import com.smartisland.app.ui.components.TabHeader
import com.smartisland.app.ui.components.pressable
import com.smartisland.app.ui.components.tabBarPadding
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.navigation.Routes
import com.smartisland.app.ui.reportAction
import com.smartisland.app.ui.theme.Palette
import com.smartisland.app.widget.WidgetUpdater

@Composable
fun ControlsScreen(nav: AppNavigator) {
    val context = LocalContext.current
    val container = appContainer()
    val volume = container.volume
    val snack = LocalSnack.current
    val haptics = LocalHapticFeedback.current

    // Bumped whenever the system reports a volume change so every slider re-reads its stream.
    var revision by remember { mutableIntStateOf(0) }
    SystemBroadcastEffect("android.media.VOLUME_CHANGED_ACTION", AudioManager.RINGER_MODE_CHANGED_ACTION) { revision++ }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }
    var torch by remember { mutableStateOf(SystemActions.torchOn) }

    val setup = { nav.go(Routes.PERMISSIONS) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .tabBarPadding(),
    ) {
        TabHeader("Controls", "Power & volume")
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionHeader("Power button")
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BigButton("Power", Icons.Rounded.PowerSettingsNew, Palette.Error) { reportAction(SystemActions.powerMenu(), snack, setup) }
                    BigButton("Lock", Icons.Rounded.Lock, Palette.Primary) { reportAction(SystemActions.lockScreen(), snack, setup) }
                    BigButton("Screenshot", Icons.Rounded.CropFree, Palette.Secondary) { reportAction(SystemActions.screenshot(), snack, setup) }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BigButton("Alerts", Icons.Rounded.Notifications, Color(0xFF5E5CE6)) { reportAction(SystemActions.notifications(), snack, setup) }
                    BigButton("Quick panel", Icons.Rounded.SpaceDashboard, Palette.Cyan) { reportAction(SystemActions.quickSettings(), snack, setup) }
                    BigButton("Recents", Icons.Rounded.ViewCarousel, Palette.Success) { reportAction(SystemActions.recents(), snack, setup) }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Tip: add the Lock tile to Quick Settings or the Power widget to your home screen for one-tap access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextTertiary,
                )
            }

            SectionHeader("Volume")
            GlassCard {
                VolumeStream.entries.forEach { stream ->
                    // Reading revision makes this row recompose after external volume changes.
                    val percent = remember(revision) { volume.percent(stream) }
                    StreamRow(
                        stream = stream,
                        percent = percent,
                        onPercent = { p ->
                            if (!volume.setPercent(stream, p)) {
                                snack.error("Allow Do Not Disturb access to change this volume", "Allow") {
                                    Permissions.launch(context, Permissions.dndSettings())
                                }
                            }
                            revision++
                            WidgetUpdater.updateVolume(context)
                        },
                        onStep = { up ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            volume.step(stream, up)
                            revision++
                            WidgetUpdater.updateVolume(context)
                        },
                    )
                    if (stream != VolumeStream.entries.last()) Spacer(Modifier.height(6.dp))
                }
            }

            SectionHeader("Ringer mode")
            val ringer = remember(revision) { volume.ringerMode }
            RingerSelector(
                selected = when (ringer) {
                    AudioManager.RINGER_MODE_SILENT -> 2
                    AudioManager.RINGER_MODE_VIBRATE -> 1
                    else -> 0
                },
                onSelect = { index ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val mode = when (index) {
                        2 -> AudioManager.RINGER_MODE_SILENT
                        1 -> AudioManager.RINGER_MODE_VIBRATE
                        else -> AudioManager.RINGER_MODE_NORMAL
                    }
                    if (!volume.setRingerMode(mode) && mode == AudioManager.RINGER_MODE_SILENT) {
                        snack.error("Silent mode needs Do Not Disturb access", "Allow") {
                            Permissions.launch(context, Permissions.dndSettings())
                        }
                    }
                    revision++
                },
            )

            SectionHeader("Flashlight")
            GlassCard(onClick = {
                if (SystemActions.setTorch(context, !torch)) {
                    torch = !torch
                    WidgetUpdater.updateToggles(context)
                } else {
                    snack.error("This device has no flashlight available")
                }
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (torch) Palette.Gold else Palette.SurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff, null,
                            tint = if (torch) Color(0xFF3A2600) else Palette.TextSecondary,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Flashlight", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
                        Text(if (torch) "On · tap to turn off" else "Off · tap to turn on", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamRow(stream: VolumeStream, percent: Int, onPercent: (Int) -> Unit, onStep: (Boolean) -> Unit) {
    val icon = when (stream) {
        VolumeStream.MEDIA -> Icons.Rounded.MusicNote
        VolumeStream.RING -> Icons.Rounded.RingVolume
        VolumeStream.NOTIFICATION -> Icons.Rounded.Notifications
        VolumeStream.ALARM -> Icons.Rounded.Alarm
        VolumeStream.CALL -> Icons.Rounded.Call
    }
    val shown = percent.toFloat()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SliderRow(
                title = stream.label,
                value = shown,
                range = 0f..100f,
                valueLabel = if (shown <= 0f) "Muted" else "${shown.toInt()}%",
                onChange = { v -> onPercent(v.toInt()) },
                icon = if (shown <= 0f && stream == VolumeStream.MEDIA) Icons.AutoMirrored.Rounded.VolumeOff else icon,
                color = if (stream == VolumeStream.MEDIA) Palette.Primary else Palette.Secondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton(Icons.Rounded.Add) { onStep(true) }
            StepButton(Icons.Rounded.Remove) { onStep(false) }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Palette.SurfaceHigh)
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Palette.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BigButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color)
                .pressable(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextPrimary, maxLines = 1)
    }
}

private data class RingerChoice(val label: String, val caption: String, val icon: ImageVector, val color: Color)

private val ringerChoices = listOf(
    RingerChoice("Ring", "Sound on", Icons.Rounded.NotificationsActive, Palette.Primary),
    RingerChoice("Vibrate", "Buzz only", Icons.Rounded.Vibration, Palette.Warning),
    RingerChoice("Silent", "Muted", Icons.Rounded.NotificationsOff, Palette.Error),
)

/** Segmented control with a springy sliding highlight, like iOS's ring/silent selector. */
@Composable
private fun RingerSelector(selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Outline, shape)
            .padding(4.dp),
    ) {
        val segment = maxWidth / ringerChoices.size
        val offset by animateDpAsState(segment * selected, spring(dampingRatio = 0.75f, stiffness = 500f), label = "ringerX")
        val color by animateColorAsState(ringerChoices[selected].color, label = "ringerColor")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .height(46.dp)
                .shadow(6.dp, RoundedCornerShape(15.dp), ambientColor = color, spotColor = color)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.82f)))),
        )
        Row(Modifier.fillMaxWidth()) {
            ringerChoices.forEachIndexed { index, choice ->
                val active = index == selected
                val content by animateColorAsState(if (active) Color.White else Palette.TextSecondary, label = "ringerText")
                Row(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(choice.icon, null, tint = content, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(choice.label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
                }
            }
        }
    }
}
