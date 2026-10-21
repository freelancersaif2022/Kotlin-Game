package com.netscope.wifianalyzer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocation
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.data.SignalSpot
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppDialog
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.LineChart
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.signalColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val glass = Color(0x99070B18)
private val roomSuggestions = listOf("Living room", "Bedroom", "Kitchen", "Office", "Hallway", "Bathroom", "Balcony", "Garage")

/**
 * Live camera signal finder: the camera view is tinted by the current Wi-Fi
 * strength (green = strong, red = weak), with a scanner reticle, a trend
 * indicator while walking and named spots you can mark.
 */
@Composable
fun CameraSignalView(networkKey: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val haptic = LocalHapticFeedback.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasCamera = ok
        if (!ok) snackbar.show("Camera access lets you scan rooms live. Signal readings still work without it.", SnackType.Info)
    }
    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }

    // Live signal samples.
    val samples = remember { mutableStateListOf<Int>() }
    var rssi by remember { mutableIntStateOf(-127) }
    var connected by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        var lastLevel = -1
        while (true) {
            val snap = withContext(Dispatchers.IO) { WifiUtils.snapshot(context) }
            connected = snap.hasSignal
            if (snap.hasSignal) {
                rssi = snap.rssi
                samples += snap.rssi
                if (samples.size > 60) samples.removeAt(0)
                val level = WifiUtils.signalLevel(snap.rssi)
                if (lastLevel >= 0 && level != lastLevel) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastLevel = level
            }
            delay(500)
        }
    }

    val spots = remember(networkKey) {
        mutableStateListOf<SignalSpot>().apply { if (networkKey != null) addAll(AppGraph.heatmap.loadSpots(networkKey)) }
    }
    var naming by remember { mutableStateOf(false) }

    val color by animateColorAsState(if (connected) signalColor(rssi) else Color(0xFF6B7899), tween(500), label = "camColor")
    val pct by animateFloatAsState(if (connected) WifiUtils.rssiToPercent(rssi) / 100f else 0f, spring(stiffness = 120f), label = "camPct")
    val trend = remember(samples.size, rssi) {
        if (samples.size < 8) 0 else {
            val recent = samples.takeLast(4).average()
            val before = samples.dropLast(4).takeLast(6).average()
            when {
                recent - before >= 2.5 -> 1
                before - recent >= 2.5 -> -1
                else -> 0
            }
        }
    }

    Box(modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFF05070F))) {
        if (hasCamera) {
            CameraPreview(Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E1640), Color(0xFF05070F)))),
            )
        }
        ScannerOverlay(color, pct, Modifier.fillMaxSize())

        // Live readout
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(glass)
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(if (connected) "$rssi" else "--", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(" dBm", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(
                        if (connected) "${WifiUtils.qualityLabel(rssi)} signal • ${(pct * 100).toInt()}%" else "Not connected to Wi-Fi",
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                val (icon, label) = when (trend) {
                    1 -> Icons.Rounded.ArrowUpward to "Getting stronger"
                    -1 -> Icons.Rounded.ArrowDownward to "Getting weaker"
                    else -> Icons.Rounded.Remove to "Steady"
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = if (trend == 1) Color(0xFF34D399) else if (trend == -1) Color(0xFFF43F5E) else Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (!hasCamera) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(glass)
                        .clickable { launcher.launch(Manifest.permission.CAMERA) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Turn on camera view", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Hint under the reticle
        Text(
            when {
                !connected -> "Connect to Wi-Fi to start scanning"
                rssi >= -55 -> "Great spot for streaming and gaming"
                rssi >= -67 -> "Good signal here"
                rssi >= -75 -> "Fair: walk toward the router"
                else -> "Weak zone: consider a mesh or extender"
            },
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 210.dp)
                .clip(RoundedCornerShape(50))
                .background(glass)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )

        // Bottom panel
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(glass)
                .padding(14.dp),
        ) {
            LineChart(
                values = samples.map { (it + 100).toFloat() },
                color = color,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                maxValue = 75f,
                gridColor = Color.White.copy(alpha = 0.08f),
            )
            if (spots.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val best = spots.maxByOrNull { it.rssi }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(spots.sortedByDescending { it.rssi }) { s ->
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(signalColor(s.rssi).copy(alpha = 0.22f))
                                .border(1.dp, signalColor(s.rssi).copy(alpha = 0.6f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (s == best) {
                                Icon(Icons.Rounded.Star, "Best", tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text("${s.name} ${s.rssi}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            GradientButton(
                "Mark this spot",
                onClick = {
                    if (!connected || networkKey == null) {
                        snackbar.show("Connect to Wi-Fi to mark spots", SnackType.Error)
                    } else {
                        naming = true
                    }
                },
                icon = Icons.Rounded.AddLocation,
                modifier = Modifier.fillMaxWidth(),
                height = 50.dp,
            )
        }
    }

    if (naming && networkKey != null) {
        val reading = if (samples.size >= 4) samples.takeLast(4).average().toInt() else rssi
        var name by remember { mutableStateOf("Spot ${spots.size + 1}") }
        AppDialog(
            onDismiss = { naming = false },
            icon = Icons.Rounded.AddLocation,
            iconTint = signalColor(reading),
            title = "Name this spot",
            message = "$reading dBm • ${WifiUtils.qualityLabel(reading)}",
            confirmText = "Save spot",
            onConfirm = {
                val clean = name.trim().ifBlank { "Spot ${spots.size + 1}" }
                spots.removeAll { it.name.equals(clean, ignoreCase = true) }
                spots += SignalSpot(clean, reading, System.currentTimeMillis())
                AppGraph.heatmap.saveSpots(networkKey, spots.toList())
                naming = false
                snackbar.show("$clean saved: $reading dBm", SnackType.Success)
            },
            dismissText = "Cancel",
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                singleLine = true,
                label = { Text("Room or spot") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                roomSuggestions.forEach { r ->
                    Text(
                        r,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { name = r }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply { cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA }
    }
    DisposableEffect(owner) {
        runCatching { controller.bindToLifecycle(owner) }
        onDispose { runCatching { controller.unbind() } }
    }
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ScannerOverlay(color: Color, pct: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "scanner")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "ring")
    val scan by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "scan")
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height * 0.46f)
        // Signal-tinted vignette: the whole view glows with the signal colour.
        drawRect(
            Brush.radialGradient(
                0.0f to Color.Transparent,
                0.45f to Color.Transparent,
                1.0f to color.copy(alpha = 0.55f),
                center = c,
                radius = size.maxDimension * 0.75f,
            ),
        )
        // Expanding scan rings.
        for (k in 0 until 3) {
            val p = (ring + k / 3f) % 1f
            drawCircle(color.copy(alpha = (1f - p) * 0.6f), radius = 40.dp.toPx() + p * size.minDimension * 0.45f, center = c, style = Stroke(2.dp.toPx()))
        }
        // Reticle corners.
        val half = 92.dp.toPx()
        val arm = 28.dp.toPx()
        val sw = 4.dp.toPx()
        val l = c.x - half
        val r = c.x + half
        val top = c.y - half
        val b = c.y + half
        listOf(
            Triple(Offset(l, top), Offset(l + arm, top), Offset(l, top + arm)),
            Triple(Offset(r, top), Offset(r - arm, top), Offset(r, top + arm)),
            Triple(Offset(l, b), Offset(l + arm, b), Offset(l, b - arm)),
            Triple(Offset(r, b), Offset(r - arm, b), Offset(r, b - arm)),
        ).forEach { (corner, h, v) ->
            drawLine(Color.White, corner, h, sw, StrokeCap.Round)
            drawLine(Color.White, corner, v, sw, StrokeCap.Round)
        }
        // Sweeping scan line inside the reticle.
        val y = top + (b - top) * scan
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent), l, r),
            Offset(l + 8.dp.toPx(), y),
            Offset(r - 8.dp.toPx(), y),
            strokeWidth = 3.dp.toPx(),
        )
        // Strength arc around the centre point.
        val ar = 34.dp.toPx()
        drawArc(Color.White.copy(alpha = 0.2f), 135f, 270f, false, Offset(c.x - ar, c.y - ar), androidx.compose.ui.geometry.Size(ar * 2, ar * 2), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawArc(color, 135f, 270f * pct, false, Offset(c.x - ar, c.y - ar), androidx.compose.ui.geometry.Size(ar * 2, ar * 2), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color.White, 5.dp.toPx(), c)
    }
}
