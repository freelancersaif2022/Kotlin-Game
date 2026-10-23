package com.voxcast.mic.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxcast.mic.ads.BannerAd
import com.voxcast.mic.ads.NativeAdCard
import com.voxcast.mic.data.PremiumManager
import com.voxcast.mic.net.HostMode
import com.voxcast.mic.ui.components.GlassCard
import com.voxcast.mic.ui.components.IconBadge
import com.voxcast.mic.ui.components.RoundIconButton
import com.voxcast.mic.ui.components.SectionLabel
import com.voxcast.mic.ui.components.StatusPill
import com.voxcast.mic.ui.components.VoxBackground
import com.voxcast.mic.ui.components.VoxDialog
import com.voxcast.mic.ui.components.bounceClick
import com.voxcast.mic.ui.components.glass
import com.voxcast.mic.ui.components.gradientTint
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMic: () -> Unit,
    onOpenSpeaker: (HostMode) -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by context.voxApp.settings.settings.collectAsStateWithLifecycle()
    val premium = settings.premium.isEntitled
    val network = rememberNetworkStatus()
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var showModes by rememberSaveable { mutableStateOf(false) }

    val micPermission = rememberPermissionRequest(listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), onOpenMic)

    VoxBackground {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                // Header.
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(VoxIcons.Microphone, VoxColors.PrimaryGradient, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("VoxCast", style = MaterialTheme.typography.headlineSmall, color = VoxColors.Text)
                        Text("Wireless microphone", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
                    }
                    PremiumChip(premium, onOpenPremium)
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
                }

                Spacer(Modifier.height(26.dp))
                Entrance(0) {
                    Column {
                        Text(
                            "Your voice,\nanywhere in the room.",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            modifier = Modifier.gradientTint(Brush.verticalGradient(listOf(Color.White, Color(0xFFB9C6DD)))),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Talk into one phone, hear it from another. Studio-clean audio over your local Wi-Fi.",
                            style = MaterialTheme.typography.bodyLarge, color = VoxColors.TextDim,
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Entrance(1) { NetworkCard(network, onClick = { showGuide = true }) }

                Spacer(Modifier.height(24.dp))
                SectionLabel("Choose this phone's role")
                Entrance(2) {
                    RoleCard(
                        icon = VoxIcons.Microphone,
                        brush = VoxColors.PrimaryGradient,
                        glow = VoxColors.Cyan,
                        title = "Microphone",
                        subtitle = "Speak into this phone",
                        detail = "Scan the speaker's QR code to connect",
                        onClick = micPermission.launch,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Entrance(3) {
                    RoleCard(
                        icon = VoxIcons.SpeakerBox,
                        brush = Brush.linearGradient(listOf(VoxColors.Violet, Color(0xFF4B3FD1))),
                        glow = VoxColors.Violet,
                        title = "Speaker",
                        subtitle = "Play the voice on this phone",
                        detail = "Shows a QR code for the microphone",
                        onClick = { showModes = true },
                    )
                }

                Spacer(Modifier.height(26.dp))
                SectionLabel("How it works")
                Entrance(4) { HowItWorks() }

                if (!premium) {
                    Spacer(Modifier.height(22.dp))
                    NativeAdCard(enabled = true)
                    Spacer(Modifier.height(16.dp))
                    PremiumUpsell(onOpenPremium)
                }
                Spacer(Modifier.height(28.dp))
            }
            Box(Modifier.navigationBarsPadding()) { BannerAd(enabled = !premium) }
        }
    }

    if (showGuide) NetworkGuideSheet(onDismiss = { showGuide = false })

    if (showModes) {
        ModalBottomSheet(
            onDismissRequest = { showModes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = VoxColors.Surface,
        ) {
            ModePicker(
                onPick = { mode -> showModes = false; onOpenSpeaker(mode) },
                onHelp = { showModes = false; showGuide = true },
            )
        }
    }

    VoxDialog(
        visible = micPermission.showRationale,
        onDismiss = micPermission.dismissRationale,
        icon = VoxIcons.Microphone,
        title = "Allow microphone & camera",
        message = "VoxCast needs the microphone to capture your voice and the camera to scan the speaker's QR code. Audio never leaves your local network.",
        confirmText = "Open settings",
        onConfirm = { micPermission.dismissRationale(); context.openAppSettings() },
        dismissText = "Not now",
    )
}

/** Staggered slide-up entrance for home sections. */
@Composable
private fun Entrance(index: Int, content: @Composable () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(550, delayMillis = 70 * index, easing = FastOutSlowInEasing)) }
    Box(Modifier.graphicsLayer { alpha = anim.value; translationY = (1f - anim.value) * 40f }) { content() }
}

@Composable
private fun PremiumChip(premium: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .bounceClick(onClick = onClick)
            .clip(CircleShape)
            .background(if (premium) VoxColors.GoldGradient else Brush.linearGradient(listOf(Color(0x33F4C46B), Color(0x1AF4C46B))))
            .border(1.dp, VoxColors.Gold.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(VoxIcons.Crown, null, tint = if (premium) Color(0xFF2A1E05) else VoxColors.Gold, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            if (premium) "PRO" else "Go Pro",
            style = MaterialTheme.typography.labelMedium,
            color = if (premium) Color(0xFF2A1E05) else VoxColors.Gold,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun NetworkCard(network: com.voxcast.mic.net.NetworkStatus, onClick: () -> Unit) {
    val ready = network.ready
    val color = if (ready) VoxColors.Success else VoxColors.Warning
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .glass(RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            network.icon(),
            Brush.linearGradient(listOf(color, color.copy(alpha = 0.5f))),
            size = 46.dp,
            glow = color,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(network.headline, style = MaterialTheme.typography.titleMedium, color = VoxColors.Text)
            Text(
                if (ready) network.links.joinToString("  ·  ") { "${linkLabel(it.kind)} ${it.ip}" }
                else "Join Wi-Fi, use a hotspot, or Wi-Fi Direct",
                style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim, maxLines = 1,
            )
        }
        StatusPill(if (ready) "Ready" else "Setup", color, pulsing = ready)
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    brush: Brush,
    glow: Color,
    title: String,
    subtitle: String,
    detail: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .glass(shape, Brush.linearGradient(listOf(glow.copy(alpha = 0.55f), VoxColors.OutlineSoft, VoxColors.OutlineSoft)))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, brush, size = 64.dp, glow = glow)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = VoxColors.Text)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VoxColors.Text.copy(alpha = 0.85f))
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(glow.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = glow, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HowItWorks() {
    GlassCard {
        val steps = listOf(
            Triple(VoxIcons.SpeakerBox, "Open Speaker", "on the phone that plays sound"),
            Triple(VoxIcons.QrScan, "Scan the QR", "with the microphone phone"),
            Triple(VoxIcons.Microphone, "Start talking", "voice plays instantly"),
        )
        steps.forEachIndexed { i, (icon, title, body) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    IconBadge(icon, Brush.linearGradient(listOf(VoxColors.SurfaceHighest, VoxColors.SurfaceHigh)), size = 42.dp, iconTint = VoxColors.Cyan)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}. $title", style = MaterialTheme.typography.titleSmall, color = VoxColors.Text)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
                }
            }
            if (i < steps.lastIndex) {
                Box(Modifier.padding(start = 20.dp).width(2.dp).height(14.dp).background(VoxColors.Outline))
            }
        }
    }
}

@Composable
private fun PremiumUpsell(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2B2210), Color(0xFF16130C))))
            .border(1.dp, Brush.linearGradient(listOf(VoxColors.Gold, VoxColors.GoldDeep.copy(alpha = 0.3f))), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(VoxIcons.Crown, VoxColors.GoldGradient, size = 50.dp, glow = VoxColors.Gold, iconTint = Color(0xFF2A1E05))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("VoxCast Premium", style = MaterialTheme.typography.titleMedium, color = VoxColors.Gold, fontWeight = FontWeight.Bold)
            Text("No ads · Studio 48 kHz · 250% boost", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(PremiumManager.PRICE, style = MaterialTheme.typography.titleLarge, color = VoxColors.Text, fontWeight = FontWeight.ExtraBold)
            Text("/ ${PremiumManager.PERIOD_LABEL}", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = VoxColors.Gold)
    }
}

@Composable
private fun ModePicker(onPick: (HostMode) -> Unit, onHelp: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("How are the phones connected?", style = MaterialTheme.typography.headlineSmall, color = VoxColors.Text)
        Text("Both phones must share one local network. No internet is needed.", style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim)
        Spacer(Modifier.height(4.dp))
        ModeOption(
            icon = VoxIcons.Wifi,
            brush = Brush.linearGradient(listOf(VoxColors.Cyan, VoxColors.CyanDeep)),
            title = "Same Wi-Fi or Hotspot",
            body = "Both phones on one router, or one phone's hotspot joined by the other.",
            badge = "Recommended",
            onClick = { onPick(HostMode.LAN) },
        )
        ModeOption(
            icon = VoxIcons.WifiDirect,
            brush = Brush.linearGradient(listOf(VoxColors.Violet, Color(0xFF4B3FD1))),
            title = "Wi-Fi Direct",
            body = "No router or hotspot. This phone creates a private link the mic joins from the QR code.",
            badge = null,
            onClick = { onPick(HostMode.WIFI_DIRECT) },
        )
        Text(
            "Not sure? See connection guide",
            style = MaterialTheme.typography.labelLarge,
            color = VoxColors.Cyan,
            modifier = Modifier.align(Alignment.CenterHorizontally).bounceClick(onClick = onHelp).padding(10.dp),
        )
    }
}

@Composable
private fun ModeOption(icon: ImageVector, brush: Brush, title: String, body: String, badge: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(VoxColors.SurfaceHigh)
            .border(1.dp, VoxColors.OutlineSoft, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, brush, size = 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoxColors.Text)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        badge, style = MaterialTheme.typography.labelSmall, color = VoxColors.Background,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(VoxColors.Cyan).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = VoxColors.TextDim)
    }
}
