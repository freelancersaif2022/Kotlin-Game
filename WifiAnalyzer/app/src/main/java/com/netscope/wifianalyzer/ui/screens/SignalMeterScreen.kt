package com.netscope.wifianalyzer.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.LineChart
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.StatTile
import com.netscope.wifianalyzer.ui.components.signalColor
import com.netscope.wifianalyzer.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun SignalMeterScreen(nav: NavHostController) {
    val net by rememberNetState(600)
    val snap = net.snap
    val sound by AppGraph.prefs.soundGuide.collectAsState()
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        while (true) {
            val s = net.snap
            if (s.hasSignal) {
                history += s.rssi.toFloat()
                while (history.size > 90) history.removeAt(0)
            }
            delay(700)
        }
    }

    // Optional "geiger" style beeps: faster beeps mean a stronger signal.
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(sound, snap.hasSignal) {
        while (sound && snap.hasSignal) {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
            val pct = WifiUtils.rssiToPercent(net.snap.rssi)
            delay((1300 - pct * 11).toLong().coerceIn(150, 1300))
        }
    }

    ScreenScaffold(title = "Signal meter", onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (net.needsLocation || !net.wifiOn) {
                LocationNeededCard(net)
                Spacer(Modifier.height(12.dp))
            }
            SignalDial(if (snap.hasSignal) snap.rssi else -100, snap.hasSignal)
            Spacer(Modifier.height(4.dp))
            Text(
                snap.ssid ?: if (snap.wifiConnected) "Wi-Fi network" else "Not connected",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                listOfNotNull(snap.band.ifBlank { null }, snap.channel.takeIf { it > 0 }?.let { "Channel $it" })
                    .joinToString(" • ").ifBlank { "Connect to Wi-Fi to measure" },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.ext.muted,
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Best", history.maxOrNull()?.toInt()?.toString() ?: "--", "dBm", Icons.Rounded.ArrowUpward, AppColors.ext.success, Modifier.weight(1f))
                StatTile("Average", if (history.isEmpty()) "--" else history.average().toInt().toString(), "dBm", Icons.Rounded.ShowChart, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatTile("Worst", history.minOrNull()?.toInt()?.toString() ?: "--", "dBm", Icons.Rounded.ArrowDownward, AppColors.ext.danger, Modifier.weight(1f))
            }
            SectionTitle("Live signal")
            AppCard(Modifier.fillMaxWidth()) {
                LineChart(
                    values = history.map { it + 100f },
                    color = signalColor(snap.rssi),
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxValue = 75f,
                )
                Spacer(Modifier.height(6.dp))
                Text("Updates live • higher is better", style = MaterialTheme.typography.labelSmall, color = AppColors.ext.subtle)
            }
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sound guide", style = MaterialTheme.typography.titleSmall)
                        Text("Beeps get faster as the signal gets stronger", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                    }
                    Switch(checked = sound, onCheckedChange = { AppGraph.prefs.setSoundGuide(it) })
                }
            }
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f)) {
                Row {
                    Icon(Icons.Rounded.Lightbulb, null, tint = AppColors.ext.gold)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Walk slowly around your home holding the phone. Above -60 dBm is great for streaming; below -75 dBm you may see slow speeds or drops. A mesh system or extender helps in weak spots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.ext.muted,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SignalDial(rssi: Int, active: Boolean) {
    val pct by animateFloatAsState(if (active) WifiUtils.rssiToPercent(rssi) / 100f else 0f, spring(stiffness = 120f), label = "pct")
    val color by animateColorAsState(signalColor(rssi), label = "sigColor")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val t = rememberInfiniteTransition(label = "dial")
    val pulse by t.animateFloat(0.92f, 1.04f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = if (active) pulse else 1f; scaleY = if (active) pulse else 1f }) {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f))), radius = size.minDimension / 2)
        }
        Canvas(Modifier.size(230.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 140f, 260f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(color.copy(alpha = 0.6f), color, color.copy(alpha = 0.6f))),
                140f, 260f * pct, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (active) "$rssi" else "--", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Text("dBm", style = MaterialTheme.typography.titleSmall, color = AppColors.ext.muted)
            Spacer(Modifier.height(6.dp))
            Text(
                if (active) "${WifiUtils.qualityLabel(rssi)} • ${(pct * 100).toInt()}%" else "No signal",
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}
