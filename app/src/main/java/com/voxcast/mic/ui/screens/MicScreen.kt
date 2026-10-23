package com.voxcast.mic.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxcast.mic.data.NoiseMode
import com.voxcast.mic.net.ConnectionInfo
import com.voxcast.mic.net.HostMode
import com.voxcast.mic.service.Role
import com.voxcast.mic.service.StreamController
import com.voxcast.mic.service.StreamState
import com.voxcast.mic.ui.components.ConnectingAnimation
import com.voxcast.mic.ui.components.ConnectionSteps
import com.voxcast.mic.ui.components.GhostButton
import com.voxcast.mic.ui.components.GlassCard
import com.voxcast.mic.ui.components.GradientButton
import com.voxcast.mic.ui.components.LiveWaveform
import com.voxcast.mic.ui.components.Messenger
import com.voxcast.mic.ui.components.RealisticMicrophone
import com.voxcast.mic.ui.components.RoundIconButton
import com.voxcast.mic.ui.components.StatusPill
import com.voxcast.mic.ui.components.VoxBackground
import com.voxcast.mic.ui.components.VoxDialog
import com.voxcast.mic.ui.components.VuMeter
import com.voxcast.mic.ui.components.bounceClick
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.launch

@Composable
fun MicScreen(payload: String, onExit: () -> Unit, onScanAgain: () -> Unit) {
    val context = LocalContext.current
    val app = context.voxApp
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val state by StreamController.state.collectAsStateWithLifecycle()
    val info = remember(payload) { ConnectionInfo.parse(payload) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var wasLive by rememberSaveable { mutableStateOf(false) }

    KeepScreenOn(settings.keepScreenOn)

    LaunchedEffect(payload) {
        val s = StreamController.state.value
        if (s.role != Role.MIC || s is StreamState.Failed) {
            StreamController.startMic(context, payload, startOnAir = !app.settings.settings.value.pushToTalk)
        }
    }
    // Speaker ended the session (or the service stopped): leave the screen.
    LaunchedEffect(state) {
        if (state is StreamState.MicLive) wasLive = true
        if (wasLive && state is StreamState.Idle) onExit()
    }

    val leave = {
        StreamController.stop(context)
        context.afterSession(onExit)
    }
    val isLive = state is StreamState.MicLive || state is StreamState.MicReconnecting
    BackHandler { if (isLive) confirmExit = true else leave() }

    VoxBackground {
        Column(Modifier.fillMaxSize()) {
            VoxTopBar(
                title = when (state) {
                    is StreamState.MicLive, is StreamState.MicReconnecting -> "Microphone · Live"
                    else -> "Connecting"
                },
                onBack = { if (isLive) confirmExit = true else leave() },
            )
            AnimatedContent(
                targetState = state,
                contentKey = { if (it is StreamState.MicLive || it is StreamState.MicReconnecting) "live" else "connecting" },
                transitionSpec = {
                    (fadeIn(tween(400)) + scaleIn(tween(450), initialScale = 0.92f)) togetherWith
                        (fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 1.05f))
                },
                modifier = Modifier.weight(1f),
                label = "mic",
            ) { s ->
                when (s) {
                    is StreamState.MicLive -> LiveMicView(s.speakerName, s.since, s.sampleRate, reconnecting = null, onEnd = { confirmExit = true })
                    is StreamState.MicReconnecting -> LiveMicView(s.speakerName, null, null, reconnecting = s.attempt, onEnd = { confirmExit = true })
                    is StreamState.MicConnecting -> ConnectingView(s, info)
                    else -> ConnectingView(StreamState.MicConnecting(info?.speakerName ?: "Speaker", "Reading secure code", 0), info)
                }
            }
        }
    }

    VoxDialog(
        visible = confirmExit,
        onDismiss = { confirmExit = false },
        icon = Icons.Rounded.CallEnd,
        accent = VoxColors.Error,
        title = "Stop the microphone?",
        message = "Your voice will stop playing on the speaker phone.",
        confirmText = "End session",
        confirmBrush = VoxColors.LiveGradient,
        onConfirm = { confirmExit = false; leave() },
        dismissText = "Keep talking",
    )

    val failed = state as? StreamState.Failed
    VoxDialog(
        visible = failed != null && failed.role == Role.MIC,
        onDismiss = { StreamController.acknowledgeFailure(); onExit() },
        icon = Icons.Rounded.WifiOff,
        accent = VoxColors.Warning,
        title = failed?.title ?: "",
        message = failed?.message ?: "",
        confirmText = "Scan again",
        onConfirm = { StreamController.acknowledgeFailure(); onScanAgain() },
        dismissText = "Back to home",
    )
}

@Composable
private fun ConnectingView(s: StreamState.MicConnecting, info: ConnectionInfo?) {
    val context = LocalContext.current
    val p2p = info?.mode == HostMode.WIFI_DIRECT
    val steps = listOf(
        "Checking network",
        if (p2p) "Joining Wi-Fi Direct" else "Local network ready",
        "Reaching ${s.speakerName}",
        "Securing audio link",
    )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        ConnectingAnimation()
        Spacer(Modifier.height(30.dp))
        Text("Connecting to", style = MaterialTheme.typography.bodyLarge, color = VoxColors.TextDim)
        Text(s.speakerName, style = MaterialTheme.typography.headlineLarge, color = VoxColors.Text, textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = s.stage,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "stage",
        ) { stage ->
            Text("$stage…", style = MaterialTheme.typography.titleMedium, color = VoxColors.Cyan)
        }
        Spacer(Modifier.height(28.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            ConnectionSteps(steps, current = s.step.coerceAtLeast(1))
        }
        val ssid = s.ssidToJoinManually
        if (ssid != null) {
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth(), border = Brush.linearGradient(listOf(VoxColors.Violet, VoxColors.OutlineSoft))) {
                Text("Join this Wi-Fi network", style = MaterialTheme.typography.titleMedium, color = VoxColors.Text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your Android version can't join Wi-Fi Direct automatically. Open Wi-Fi settings, join this network, then come back. VoxCast connects as soon as you're on it.",
                    style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim,
                )
                Spacer(Modifier.height(12.dp))
                CopyRow(context, "Network", ssid)
                Spacer(Modifier.height(8.dp))
                CopyRow(context, "Password", s.passphrase.orEmpty())
                Spacer(Modifier.height(12.dp))
                GradientButton("Open Wi-Fi settings", onClick = { context.openWifiSettings() }, icon = Icons.Rounded.Router, height = 50.dp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Both phones must be on the same Wi-Fi, one phone's hotspot, or Wi-Fi Direct.",
            style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CopyRow(context: Context, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .bounceClick {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(label, value))
                Messenger.success("$label copied")
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim, modifier = Modifier.width(76.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = VoxColors.Text, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ContentCopy, "Copy", tint = VoxColors.Cyan, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LiveMicView(speakerName: String, since: Long?, sampleRate: Int?, reconnecting: Int?, onEnd: () -> Unit) {
    val context = LocalContext.current
    val app = context.voxApp
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val onAir by StreamController.onAir.collectAsStateWithLifecycle()
    val level = StreamController.level.collectAsStateWithLifecycle()
    val stats by StreamController.stats.collectAsStateWithLifecycle()
    val haptic = rememberHaptic()
    val ptt = settings.pushToTalk
    val dim by animateFloatAsState(if (reconnecting != null) 0.45f else 1f, tween(300), label = "dim")

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedContent(targetState = onAir, label = "air") { air ->
                    if (air) StatusPill("ON AIR", VoxColors.Live, pulsing = true)
                    else StatusPill("MUTED", VoxColors.TextDim, icon = Icons.Rounded.MicOff)
                }
                if (since != null) ElapsedText(since)
            }
            Spacer(Modifier.height(8.dp))
            Text("Streaming to $speakerName", style = MaterialTheme.typography.titleMedium, color = VoxColors.TextDim, maxLines = 1)
            Spacer(Modifier.height(10.dp))

            // The microphone itself is the main control.
            Box(
                Modifier
                    .fillMaxWidth(0.52f)
                    .alpha(dim)
                    .pointerInput(ptt) {
                        if (ptt) {
                            detectTapGestures(onPress = {
                                haptic(HapticFeedbackType.LongPress)
                                StreamController.setOnAir(true)
                                tryAwaitRelease()
                                StreamController.setOnAir(false)
                                haptic(HapticFeedbackType.TextHandleMove)
                            })
                        } else {
                            detectTapGestures(onTap = {
                                haptic(HapticFeedbackType.LongPress)
                                StreamController.setOnAir(!StreamController.onAir.value)
                            })
                        }
                    },
            ) {
                RealisticMicrophone(level = { level.value }, live = onAir, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    ptt && !onAir -> "Hold the microphone to talk"
                    ptt -> "Release to mute"
                    onAir -> "Tap the microphone to mute"
                    else -> "Tap the microphone to go live"
                },
                style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim,
            )
            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                LiveWaveform(level = { if (onAir) level.value else 0f }, height = 56.dp)
                Spacer(Modifier.height(10.dp))
                VuMeter(level = { level.value })
            }
            Spacer(Modifier.height(14.dp))
            NoiseControls(settings.noiseMode, settings.micGainDb, settings.voiceEnhance)
            Spacer(Modifier.height(14.dp))
            StatsRow(stats)
            if (sampleRate != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${sampleRate / 1000} kHz · 16-bit · ${settings.noiseMode.label}",
                    style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    if (onAir) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    if (onAir) "Mute" else "Unmute",
                    onClick = { haptic(HapticFeedbackType.LongPress); StreamController.setOnAir(!onAir) },
                    size = 56.dp,
                    active = onAir,
                )
                GradientButton(
                    "End session", onClick = onEnd, brush = VoxColors.LiveGradient, contentColor = Color.White,
                    icon = Icons.Rounded.CallEnd, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        AnimatedVisibility(
            visible = reconnecting != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2312))
                    .border(1.dp, VoxColors.Warning.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = VoxColors.Warning, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("Reconnecting… attempt ${reconnecting ?: 1}", style = MaterialTheme.typography.labelLarge, color = VoxColors.Warning)
            }
        }
    }
}

@Composable
private fun NoiseControls(mode: NoiseMode, gainDb: Float, enhance: Boolean) {
    val app = LocalContext.current.voxApp
    var gain by remember(gainDb) { mutableFloatStateOf(gainDb) }
    GlassCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(VoxIcons.NoiseShield, null, tint = VoxColors.Success, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Noise reduction", style = MaterialTheme.typography.titleSmall, color = VoxColors.Text)
        }
        Spacer(Modifier.height(10.dp))
        SegmentedNoise(mode) { m -> app.appScope.launch { app.settings.setNoiseMode(m) } }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mic gain", style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim, modifier = Modifier.weight(1f))
            Text(
                (if (gain >= 0) "+" else "") + String.format(java.util.Locale.US, "%.0f dB", gain),
                style = MaterialTheme.typography.labelLarge, color = VoxColors.Cyan,
            )
        }
        Slider(
            value = gain,
            onValueChange = { gain = it },
            onValueChangeFinished = { app.appScope.launch { app.settings.setMicGainDb(gain) } },
            valueRange = -6f..12f,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VoxColors.Cyan, inactiveTrackColor = Color.White.copy(alpha = 0.1f)),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Voice clarity", style = MaterialTheme.typography.bodyMedium, color = VoxColors.Text)
                Text("Presence boost for crisp speech", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint)
            }
            Switch(
                checked = enhance,
                onCheckedChange = { v -> app.appScope.launch { app.settings.setVoiceEnhance(v) } },
                colors = SwitchDefaults.colors(checkedTrackColor = VoxColors.Cyan, checkedThumbColor = Color.White),
            )
        }
    }
}

@Composable
fun SegmentedNoise(selected: NoiseMode, onSelect: (NoiseMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NoiseMode.entries.forEach { m ->
            val active = m == selected
            val bg by animateFloatAsState(if (active) 1f else 0f, tween(250), label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(VoxColors.Cyan.copy(alpha = 0.9f * bg), VoxColors.Violet.copy(alpha = 0.9f * bg))))
                    .bounceClick { onSelect(m) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    m.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) VoxColors.Background else VoxColors.TextDim,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                )
            }
        }
    }
}

