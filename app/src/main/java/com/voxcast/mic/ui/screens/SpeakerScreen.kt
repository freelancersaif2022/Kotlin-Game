package com.voxcast.mic.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxcast.mic.ads.BannerAd
import com.voxcast.mic.ads.NativeAdCard
import com.voxcast.mic.data.SettingsRepository
import com.voxcast.mic.net.ConnectionInfo
import com.voxcast.mic.net.HostMode
import com.voxcast.mic.service.LinkStats
import com.voxcast.mic.service.Role
import com.voxcast.mic.service.StreamController
import com.voxcast.mic.service.StreamState
import com.voxcast.mic.ui.components.GhostButton
import com.voxcast.mic.ui.components.GlassCard
import com.voxcast.mic.ui.components.GradientButton
import com.voxcast.mic.ui.components.IconBadge
import com.voxcast.mic.ui.components.RealisticSpeaker
import com.voxcast.mic.ui.components.RoundIconButton
import com.voxcast.mic.ui.components.StatTile
import com.voxcast.mic.ui.components.StatusPill
import com.voxcast.mic.ui.components.StyledQrCode
import com.voxcast.mic.ui.components.VoxBackground
import com.voxcast.mic.ui.components.VoxDialog
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SpeakerScreen(mode: HostMode, onExit: () -> Unit, onOpenPremium: () -> Unit) {
    val context = LocalContext.current
    val app = context.voxApp
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val state by StreamController.state.collectAsStateWithLifecycle()
    val premium = settings.premium.isEntitled
    var confirmExit by rememberSaveable { mutableStateOf(false) }

    KeepScreenOn(settings.keepScreenOn)

    val required = if (mode == HostMode.WIFI_DIRECT) {
        if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.NEARBY_WIFI_DEVICES) else listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else emptyList()
    val optional = if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
    val permission = rememberPermissionRequest(required, optional) { StreamController.startSpeaker(context, mode) }

    LaunchedEffect(Unit) {
        val s = StreamController.state.value
        if (s.role != Role.SPEAKER || s is StreamState.Failed) permission.launch()
    }

    val leave = {
        StreamController.stop(context)
        context.afterSession(onExit)
    }
    BackHandler {
        if (state is StreamState.SpeakerLive) confirmExit = true else leave()
    }

    VoxBackground(accent = VoxColors.Violet, secondary = VoxColors.Cyan) {
        Column(Modifier.fillMaxSize()) {
            VoxTopBar(
                title = if (state is StreamState.SpeakerLive) "Speaker · Live" else "Speaker",
                onBack = { if (state is StreamState.SpeakerLive) confirmExit = true else leave() },
                actions = {
                    if (state is StreamState.SpeakerWaiting) {
                        RoundIconButton(Icons.Rounded.Refresh, "New code", { StreamController.refreshCode() })
                    }
                },
            )
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(tween(350)) + scaleIn(tween(400), initialScale = 0.94f)) togetherWith
                        (fadeOut(tween(200)) + scaleOut(tween(250), targetScale = 1.04f))
                },
                modifier = Modifier.weight(1f),
                label = "speaker",
            ) { s ->
                when (s) {
                    is StreamState.SpeakerWaiting -> WaitingView(s.info, premium)
                    is StreamState.SpeakerLive -> LiveSpeakerView(
                        s, premium, settings.speakerBoost,
                        onVolumeChanged = { v -> app.appScope.launch { app.settings.setSpeakerBoost(v) } },
                        onEnd = { confirmExit = true },
                        onOpenPremium = onOpenPremium,
                    )
                    is StreamState.Preparing -> PreparingView(s.message)
                    else -> PreparingView(if (mode == HostMode.WIFI_DIRECT) "Creating Wi-Fi Direct group" else "Opening speaker")
                }
            }
            if (state !is StreamState.SpeakerLive) {
                Box(Modifier.navigationBarsPadding()) { BannerAd(enabled = !premium) }
            }
        }
    }

    VoxDialog(
        visible = confirmExit,
        onDismiss = { confirmExit = false },
        icon = Icons.Rounded.CallEnd,
        accent = VoxColors.Error,
        title = "End this session?",
        message = "The microphone will be disconnected and audio will stop playing.",
        confirmText = "End session",
        confirmBrush = VoxColors.LiveGradient,
        onConfirm = { confirmExit = false; leave() },
        dismissText = "Keep listening",
    )

    val failed = state as? StreamState.Failed
    VoxDialog(
        visible = failed != null && failed.role == Role.SPEAKER,
        onDismiss = { StreamController.acknowledgeFailure(); onExit() },
        icon = Icons.Rounded.WifiOff,
        accent = VoxColors.Warning,
        title = failed?.title ?: "",
        message = failed?.message ?: "",
        confirmText = "Try again",
        onConfirm = { StreamController.acknowledgeFailure(); permission.launch() },
        dismissText = "Back to home",
    )

    VoxDialog(
        visible = permission.showRationale,
        onDismiss = { permission.dismissRationale(); onExit() },
        icon = VoxIcons.WifiDirect,
        accent = VoxColors.Violet,
        title = "Nearby devices permission",
        message = "Wi-Fi Direct needs the Nearby devices (or Location on older Android) permission to create the private link. Nothing is tracked.",
        confirmText = "Open settings",
        onConfirm = { permission.dismissRationale(); context.openAppSettings() },
        dismissText = "Use Wi-Fi instead",
    )
}

@Composable
private fun PreparingView(message: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        RealisticSpeaker(level = { 0f }, active = false, modifier = Modifier.width(170.dp))
        Spacer(Modifier.height(26.dp))
        Text(message, style = MaterialTheme.typography.titleLarge, color = VoxColors.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("This takes a second…", style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim)
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier.width(160.dp).clip(RoundedCornerShape(2.dp)),
            color = VoxColors.Violet,
            trackColor = Color.White.copy(alpha = 0.08f),
        )
    }
}

@Composable
private fun WaitingView(info: ConnectionInfo, premium: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        StatusPill("Waiting for microphone", VoxColors.Cyan, pulsing = true)
        Spacer(Modifier.height(14.dp))
        Text("Scan to connect", style = MaterialTheme.typography.headlineLarge, color = VoxColors.Text)
        Spacer(Modifier.height(6.dp))
        Text(
            "On the other phone open VoxCast, tap Microphone and point the camera at this code.",
            style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        Box(Modifier.fillMaxWidth(0.82f)) { StyledQrCode(info.toQrPayload()) }
        Spacer(Modifier.height(26.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    if (info.mode == HostMode.WIFI_DIRECT) VoxIcons.WifiDirect else VoxIcons.Wifi,
                    if (info.mode == HostMode.WIFI_DIRECT) Brush.linearGradient(listOf(VoxColors.Violet, Color(0xFF4B3FD1)))
                    else Brush.linearGradient(listOf(VoxColors.Cyan, VoxColors.CyanDeep)),
                    size = 44.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(info.speakerName, style = MaterialTheme.typography.titleMedium, color = VoxColors.Text, maxLines = 1)
                    Text(
                        if (info.mode == HostMode.WIFI_DIRECT) "Wi-Fi Direct · private link" else "Same Wi-Fi / Hotspot",
                        style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim,
                    )
                }
                StatusPill("Unique code", VoxColors.Success, icon = VoxIcons.NoiseShield)
            }
            if (info.mode == HostMode.WIFI_DIRECT && info.ssid != null) {
                Spacer(Modifier.height(14.dp))
                CredentialRow("Network", info.ssid)
                Spacer(Modifier.height(6.dp))
                CredentialRow("Password", info.passphrase ?: "")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Android 10+ joins automatically. Older phones: join this network in Wi-Fi settings first.",
                    style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Address: " + info.hosts.joinToString("  ·  ") + "  :${info.port}",
                style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint, fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.height(16.dp))
        GhostButton("Generate new code", onClick = { StreamController.refreshCode() }, icon = Icons.Rounded.Refresh, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        NativeAdCard(enabled = !premium)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim, modifier = Modifier.width(76.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = VoxColors.Text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LiveSpeakerView(
    s: StreamState.SpeakerLive,
    premium: Boolean,
    savedVolume: Float,
    onVolumeChanged: (Float) -> Unit,
    onEnd: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    val level = StreamController.level.collectAsStateWithLifecycle()
    val stats by StreamController.stats.collectAsStateWithLifecycle()
    val maxVolume = if (premium) SettingsRepository.MAX_BOOST_PREMIUM else SettingsRepository.MAX_BOOST_FREE
    var volume by remember { mutableFloatStateOf(savedVolume.coerceIn(0f, maxVolume)) }
    LaunchedEffect(volume) { StreamController.speakerVolume = volume }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("LIVE", VoxColors.Live, pulsing = true)
            ElapsedText(s.since)
        }
        Spacer(Modifier.height(8.dp))
        Text(s.micName, style = MaterialTheme.typography.headlineMedium, color = VoxColors.Text, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        AnimatedContent(targetState = s.talking, label = "talk") { talking ->
            if (talking) {
                Text("Playing live voice", style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MicOff, null, tint = VoxColors.Warning, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Microphone is muted", style = MaterialTheme.typography.bodyMedium, color = VoxColors.Warning)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        RealisticSpeaker(level = { level.value }, active = true, modifier = Modifier.fillMaxWidth(0.72f))
        Spacer(Modifier.height(22.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeUp, null, tint = VoxColors.Cyan)
                Spacer(Modifier.width(10.dp))
                Text("Volume", style = MaterialTheme.typography.titleMedium, color = VoxColors.Text, modifier = Modifier.weight(1f))
                Text(
                    "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (volume > 1f) VoxColors.Gold else VoxColors.Cyan,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = volume,
                onValueChange = { volume = it },
                onValueChangeFinished = { onVolumeChanged(volume) },
                valueRange = 0f..maxVolume,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = if (volume > 1f) VoxColors.Gold else VoxColors.Cyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                ),
            )
            if (!premium) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(VoxIcons.Crown, null, tint = VoxColors.Gold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Boost up to 250% with Premium",
                        style = MaterialTheme.typography.bodySmall, color = VoxColors.Gold,
                        modifier = Modifier.weight(1f),
                    )
                    RoundIconButton(VoxIcons.Crown, "Premium", onOpenPremium, size = 32.dp, tint = VoxColors.Gold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        StatsRow(stats)
        Spacer(Modifier.height(20.dp))
        GradientButton("End session", onClick = onEnd, brush = VoxColors.LiveGradient, contentColor = Color.White, icon = Icons.Rounded.CallEnd)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun StatsRow(stats: LinkStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val latency = if (stats.rttMs >= 0) stats.rttMs / 2 + stats.bufferMs + 20 else -1
        StatTile("Latency", if (latency >= 0) "$latency ms" else "—", Modifier.weight(1f))
        StatTile(
            "Packet loss", String.format(java.util.Locale.US, "%.1f%%", stats.lossPercent), Modifier.weight(1f),
            accent = if (stats.lossPercent < 2f) VoxColors.Success else VoxColors.Warning,
        )
        StatTile("Bitrate", if (stats.kbps > 0) "${stats.kbps} kb/s" else "—", Modifier.weight(1f), accent = VoxColors.Violet)
    }
}

@Composable
fun ElapsedText(since: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val secs = ((now - since) / 1000).coerceAtLeast(0)
    Text(
        String.format(java.util.Locale.US, "%02d:%02d", secs / 60, secs % 60),
        style = MaterialTheme.typography.titleSmall,
        color = VoxColors.TextDim,
        fontFamily = FontFamily.Monospace,
    )
}
