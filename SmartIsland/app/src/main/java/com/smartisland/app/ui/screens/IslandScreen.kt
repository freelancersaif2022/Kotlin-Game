package com.smartisland.app.ui.screens

import android.os.Build
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.data.SettingsRepository
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.Demo
import com.smartisland.app.ui.components.Divider
import com.smartisland.app.ui.components.GhostButton
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PhoneTopPreview
import com.smartisland.app.ui.components.ProBadge
import com.smartisland.app.ui.components.SectionHeader
import com.smartisland.app.ui.components.SliderRow
import com.smartisland.app.ui.components.SnackController
import com.smartisland.app.ui.components.SwitchRow
import com.smartisland.app.ui.components.TabHeader
import com.smartisland.app.ui.components.pressable
import com.smartisland.app.ui.components.tabBarPadding
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.navigation.Routes
import com.smartisland.app.ui.theme.Palette
import kotlin.math.roundToInt

private enum class PreviewKind(val label: String) { IDLE("Idle"), MUSIC("Music"), CALL("Call"), CHARGING("Charging"), ALERT("Alert") }

@Composable
fun IslandScreen(nav: AppNavigator) {
    val container = appContainer()
    val repo = container.settings
    val snack = LocalSnack.current
    val view = LocalView.current
    val s by repo.settings.collectAsStateWithLifecycle()
    val premium by container.premium.state.collectAsStateWithLifecycle()
    val pro = premium.isPremium

    var kind by remember { mutableStateOf(PreviewKind.MUSIC) }
    var expanded by remember { mutableStateOf(false) }
    val previewActivity: IslandActivity = when (kind) {
        PreviewKind.IDLE -> IslandActivity.Idle
        PreviewKind.MUSIC -> remember { Demo.media() }
        PreviewKind.CALL -> remember { Demo.ongoingCall() }
        PreviewKind.CHARGING -> Demo.charging()
        PreviewKind.ALERT -> Demo.notice()
    }
    val paywall = { nav.go(Routes.PAYWALL) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .tabBarPadding(),
    ) {
        TabHeader("Island", "Customize")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(10.dp))
            PhoneTopPreview(s, previewActivity, expanded, height = 250.dp, onExpandedChange = { expanded = it })
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewKind.entries.forEach { k ->
                    Chip(k.label, k == kind, Modifier.weight(1f)) {
                        kind = k
                        expanded = false
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Compact", !expanded, Modifier.weight(1f)) { expanded = false }
                Chip("Expanded", expanded, Modifier.weight(1f)) { expanded = true }
            }

            SectionHeader("Size & position") {
                Text(
                    "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.PrimarySoft,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pressable({
                            repo.resetGeometry()
                            snack.success("Island size reset")
                        })
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            GlassCard {
                SliderRow("Width", s.widthDp, IslandSettings.WIDTH_RANGE, "${s.widthDp.roundToInt()} dp", { v -> repo.update { it.copy(widthDp = v) } }, Icons.Rounded.SwapHoriz)
                SliderRow("Height", s.heightDp, IslandSettings.HEIGHT_RANGE, "${s.heightDp.roundToInt()} dp", { v -> repo.update { it.copy(heightDp = v) } }, Icons.Rounded.Height)
                SliderRow("Top position", s.topOffsetDp, IslandSettings.TOP_RANGE, "${s.topOffsetDp.roundToInt()} dp", { v -> repo.update { it.copy(topOffsetDp = v) } }, Icons.Rounded.VerticalAlignTop)
                SliderRow("Horizontal offset", s.xOffsetDp, IslandSettings.X_RANGE, "${s.xOffsetDp.roundToInt()} dp", { v -> repo.update { it.copy(xOffsetDp = v) } }, Icons.Rounded.CenterFocusStrong)
                SliderRow("Expanded width", s.expandedWidthDp, IslandSettings.EXPANDED_RANGE, "${s.expandedWidthDp.roundToInt()} dp", { v -> repo.update { it.copy(expandedWidthDp = v) } }, Icons.Rounded.OpenInFull)
                SliderRow("Roundness", s.roundness, 0f..1f, "${(s.roundness * 100).roundToInt()}%", { v -> repo.update { it.copy(roundness = v) } }, Icons.Rounded.RoundedCorner)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("Fit to camera", { fitToCamera(view, repo, snack) }, Modifier.weight(1f), Icons.Rounded.CenterFocusStrong, Palette.PrimarySoft)
                    GhostButton("Reset", { repo.resetGeometry() }, Modifier.weight(1f), Icons.Rounded.RestartAlt)
                }
            }

            // Split into sections so a slider drag only recomposes the preview and size card.
            ActivitiesSection(repo, pro, paywall, s.showMedia, s.showCalls, s.showCharging, s.showRinger, s.showNotifications, s.replaceVolumePanel)
            StyleSection(repo, snack, pro, paywall, s.accentColor, s.glow, s.outline, s.outlineColor, s.haptics)
            BehaviorSection(repo, pro, paywall, s.hideWhenIdle, s.hideInLandscape, s.edgePanelEnabled, s.edgePanelRight)
        }
    }
}

@Composable
private fun ActivitiesSection(
    repo: SettingsRepository,
    pro: Boolean,
    paywall: () -> Unit,
    showMedia: Boolean,
    showCalls: Boolean,
    showCharging: Boolean,
    showRinger: Boolean,
    showNotifications: Boolean,
    volumeHud: Boolean,
) {
    SectionHeader("Live activities")
    GlassCard(padding = 12.dp) {
        SwitchRow(Icons.Rounded.MusicNote, Color(0xFFFF375F), "Music & media", "Album art, waveform and controls", showMedia, { v -> repo.update { it.copy(showMedia = v) } })
        Divider()
        SwitchRow(Icons.Rounded.Call, Palette.Success, "Calls", "Answer, decline and live call timer", showCalls, { v -> repo.update { it.copy(showCalls = v) } })
        Divider()
        SwitchRow(Icons.Rounded.BatteryChargingFull, Palette.Success, "Charging & battery", "Charging animation and low battery", showCharging, { v -> repo.update { it.copy(showCharging = v) } })
        Divider()
        SwitchRow(Icons.Rounded.NotificationsActive, Palette.Warning, "Ringer mode", "Silent, vibrate and ring changes", showRinger, { v -> repo.update { it.copy(showRinger = v) } })
        Divider()
        SwitchRow(
            Icons.Rounded.Notifications, Palette.Secondary, "Notification alerts", "Messages pop out of the island",
            showNotifications, { v -> repo.update { it.copy(showNotifications = v) } }, locked = !pro, onLockedClick = paywall,
        )
        Divider()
        SwitchRow(
            Icons.AutoMirrored.Rounded.VolumeUp, Palette.Primary, "Volume HUD", "Volume keys show a 0–100% bar on the island",
            volumeHud, { v -> repo.update { it.copy(replaceVolumePanel = v) } }, locked = !pro, onLockedClick = paywall,
        )
    }
}

@Composable
private fun StyleSection(
    repo: SettingsRepository,
    snack: SnackController,
    pro: Boolean,
    paywall: () -> Unit,
    accent: Int,
    glow: Boolean,
    outline: Int,
    outlineColor: Int,
    haptics: Boolean,
) {
    SectionHeader("Style") { if (!pro) ProBadge() }
    GlassCard {
        Text("Accent color", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        Text("Colors the glow, volume bar and alerts", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IslandSettings.ACCENTS.forEach { c ->
                ColorDot(Color(c), selected = accent == c) {
                    if (pro) repo.update { it.copy(accentColor = c) } else paywall()
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            Icons.Rounded.AutoAwesome, Palette.Cyan, "Glow", "Soft halo in your accent color", glow,
            { v ->
                if (v && outline != IslandSettings.OUTLINE_OFF) {
                    snack.error("Turn off the outline first to use Glow")
                } else {
                    repo.update { it.copy(glow = v) }
                }
            },
            locked = !pro, onLockedClick = paywall,
        )
        Spacer(Modifier.height(10.dp))
        Text("Outline", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        Text(
            if (glow) "Turn off Glow to use a running outline" else "A running RGB or single-color ring around the island",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(IslandSettings.OUTLINE_OFF to "Off", IslandSettings.OUTLINE_RGB to "RGB", IslandSettings.OUTLINE_COLOR to "Color").forEach { (mode, label) ->
                Chip(label, outline == mode, Modifier.weight(1f)) {
                    when {
                        !pro -> paywall()
                        mode != IslandSettings.OUTLINE_OFF && glow -> snack.error("Turn off Glow first to use the outline")
                        else -> repo.update { it.copy(outline = mode) }
                    }
                }
            }
        }
        if (outline == IslandSettings.OUTLINE_COLOR) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IslandSettings.ACCENTS.forEach { c ->
                    ColorDot(Color(c), selected = outlineColor == c) {
                        if (pro) repo.update { it.copy(outlineColor = c) } else paywall()
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        SwitchRow(Icons.Rounded.TouchApp, Palette.Primary, "Haptics", "Vibrate on tap and long-press", haptics, { v -> repo.update { it.copy(haptics = v) } })
    }
}

@Composable
private fun BehaviorSection(
    repo: SettingsRepository,
    pro: Boolean,
    paywall: () -> Unit,
    hideWhenIdle: Boolean,
    hideLandscape: Boolean,
    edge: Boolean,
    edgeRight: Boolean,
) {
    SectionHeader("Behavior")
    GlassCard(padding = 12.dp) {
        SwitchRow(
            Icons.Rounded.VisibilityOff, Palette.Primary, "Hide island", "Only appears for calls, music, alerts and more, then vanishes",
            hideWhenIdle, { v -> repo.update { it.copy(hideWhenIdle = v) } },
        )
        Divider()
        SwitchRow(Icons.Rounded.ScreenRotation, Palette.Secondary, "Hide in landscape", "Stay out of the way in games and videos", hideLandscape, { v -> repo.update { it.copy(hideInLandscape = v) } })
        Divider()
        SwitchRow(
            Icons.Rounded.VerticalSplit, Palette.Warning, "Edge panel", "Floating power & volume buttons. Drag the handle up or down to move it",
            edge, { v -> repo.update { it.copy(edgePanelEnabled = v) } }, locked = !pro, onLockedClick = paywall,
        )
        if (pro && edge) {
            Row(Modifier.padding(start = 52.dp, top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Left edge", !edgeRight, Modifier.weight(1f)) { repo.update { it.copy(edgePanelRight = false) } }
                Chip("Right edge", edgeRight, Modifier.weight(1f)) { repo.update { it.copy(edgePanelRight = true) } }
            }
        }
    }
}

/** Centers the island on the front camera using the display cutout reported by the system. */
private fun fitToCamera(view: View, repo: SettingsRepository, snack: SnackController) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        snack.error("Camera detection needs Android 9 or newer")
        return
    }
    val cutout = view.rootWindowInsets?.displayCutout
    val rect = cutout?.boundingRects?.firstOrNull { it.top <= 4 }
    if (rect == null) {
        snack.show("No camera cutout found. Adjust the sliders manually.")
        return
    }
    val density = view.resources.displayMetrics.density
    val screenW = view.resources.displayMetrics.widthPixels
    val camW = rect.width() / density
    val camH = rect.height() / density
    val centerX = (rect.exactCenterX() - screenW / 2f) / density
    val height = (camH + 6f).coerceIn(IslandSettings.HEIGHT_RANGE)
    val top = (rect.exactCenterY() / density - height / 2f).coerceIn(IslandSettings.TOP_RANGE)
    repo.update {
        it.copy(
            widthDp = (camW * 3.2f).coerceIn(IslandSettings.WIDTH_RANGE),
            heightDp = height,
            topOffsetDp = top,
            xOffsetDp = centerX.coerceIn(IslandSettings.X_RANGE),
        )
    }
    snack.success("Island aligned to your camera")
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.Primary.copy(alpha = 0.22f) else Palette.Surface, label = "chip")
    val border by animateColorAsState(if (selected) Palette.Primary else Palette.Outline, label = "chipBorder")
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .pressable(onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) Palette.TextPrimary else Palette.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 0.dp, Palette.TextPrimary.copy(alpha = 0.85f), CircleShape)
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}
