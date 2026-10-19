package com.eyescroll.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyescroll.app.data.ScrollDistance
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.data.TrackingMode
import com.eyescroll.app.service.EyeTrackingService
import com.eyescroll.app.tracking.GazeZone
import com.eyescroll.app.tracking.ScrollDirection
import com.eyescroll.app.tracking.TrackingBus
import com.eyescroll.app.tracking.TrackingPhase
import com.eyescroll.app.tracking.TrackingStatus
import com.eyescroll.app.ui.components.AnimatedEye
import com.eyescroll.app.ui.components.BulletRow
import com.eyescroll.app.ui.components.GazeMeter
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.GradientButton
import com.eyescroll.app.ui.components.PowerOrb
import com.eyescroll.app.ui.components.SectionLabel
import com.eyescroll.app.ui.components.StatusPill
import com.eyescroll.app.ui.theme.EsColors

@Composable
fun HomeScreen(onCalibrate: () -> Unit, onSetup: () -> Unit, onPractice: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val settings by store.settings.collectAsStateWithLifecycle()
    val status by TrackingBus.status.collectAsStateWithLifecycle()
    val setup = rememberSetupState().state.value
    val ready = setup.camera && setup.calibrated

    val toggle: () -> Unit = {
        when {
            status.isActive -> EyeTrackingService.stop(context)
            ready -> EyeTrackingService.start(context)
            !setup.camera -> onSetup()
            else -> onCalibrate()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp).enterAnimation(0), verticalAlignment = Alignment.CenterVertically) {
            AnimatedEye(Modifier.size(width = 44.dp, height = 30.dp), lookY = 0f, glowColor = Color.Transparent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("EyeScroll", style = MaterialTheme.typography.titleLarge, color = EsColors.Text)
                Text("Hands-free scrolling", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
            }
            val (label, color) = phaseLabel(status)
            StatusPill(label, color, pulsing = status.isActive)
        }

        // Hero card with the power orb
        GlassCard(Modifier.fillMaxWidth().padding(top = 10.dp).enterAnimation(1), highlighted = status.isActive, padding = 22.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PowerOrb(active = status.isActive, enabled = true, onClick = toggle)
                Spacer(Modifier.height(14.dp))
                Text(
                    when {
                        status.isActive -> "Eye scrolling is ON"
                        !ready -> "Finish setup to start"
                        else -> "Tap to start eye scrolling"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = EsColors.Text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle(status, settings.autoOffSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EsColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
                val msg = status.message
                AnimatedVisibility(msg != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Row(
                        Modifier.padding(top = 14.dp).clip(RoundedCornerShape(14.dp))
                            .background(EsColors.Warning.copy(alpha = 0.10f)).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Info, null, tint = EsColors.Warning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(msg ?: "", style = MaterialTheme.typography.bodySmall, color = EsColors.Text)
                    }
                }
            }
        }

        // Live tracking panel
        AnimatedVisibility(status.isActive, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            LivePanel(status)
        }

        // Setup warning
        AnimatedVisibility(!setup.requiredDone, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            GlassCard(Modifier.fillMaxWidth().padding(top = 14.dp), onClick = onSetup, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = EsColors.Warning)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Setup not finished", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
                        val missing = buildList {
                            if (!setup.camera) add("camera")
                            if (!setup.accessibility) add("accessibility")
                            if (!setup.calibrated) add("calibration")
                        }.joinToString(", ")
                        Text("Still needed: $missing", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
                    }
                    Text("Open", style = MaterialTheme.typography.labelLarge, color = EsColors.Primary)
                }
            }
        }

        // Quick actions
        Row(Modifier.fillMaxWidth().padding(top = 14.dp).enterAnimation(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Filled.List, "Practice", Modifier.weight(1f), onPractice)
            QuickAction(Icons.Filled.Refresh, "Recalibrate", Modifier.weight(1f)) { if (setup.camera) onCalibrate() else onSetup() }
            QuickAction(Icons.Filled.Build, "Setup", Modifier.weight(1f), onSetup)
        }

        // Settings
        Spacer(Modifier.height(22.dp))
        SectionLabel("Tracking")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(3)) {
            Text("Mode", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = TrackingMode.entries.map { it.label },
                selected = settings.mode.ordinal,
            ) { i -> store.update { it.copy(mode = TrackingMode.entries[i]) } }
            Text(
                settings.mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = EsColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
            if (setup.calibrated.not() && store.calibration.value != null) {
                Text(
                    "This mode needs a new calibration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EsColors.Warning,
                )
            }
            Divider()
            LabeledSlider(
                title = "Sensitivity",
                valueText = when {
                    settings.sensitivity < 0.34f -> "Calm"
                    settings.sensitivity < 0.67f -> "Balanced"
                    else -> "Quick"
                },
                hint = "Higher reacts to a smaller glance toward the edge",
                value = settings.sensitivity,
                range = 0f..1f,
            ) { v -> store.update { it.copy(sensitivity = v) } }
            Divider()
            LabeledSlider(
                title = "Look-and-hold time",
                valueText = "${settings.dwellMs} ms",
                hint = "How long to look at the edge before it scrolls",
                value = settings.dwellMs.toFloat(),
                range = 200f..1200f,
                steps = 19,
            ) { v -> store.update { it.copy(dwellMs = v.toInt()) } }
            Divider()
            Text("Camera speed", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Text("Checks per second · higher feels more instant", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            val fpsOptions = listOf(15, 24, 30)
            Segmented(
                options = listOf("15 /s · saver", "24 /s", "30 /s · fast"),
                selected = fpsOptions.indexOf(settings.targetFps).coerceAtLeast(0),
            ) { i -> store.update { it.copy(targetFps = fpsOptions[i]) } }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Scrolling")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(4)) {
            Text("Scroll distance", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = ScrollDistance.entries.map { it.label },
                selected = settings.scrollDistance.ordinal,
            ) { i -> store.update { it.copy(scrollDistance = ScrollDistance.entries[i]) } }
            Spacer(Modifier.height(6.dp))
            Divider()
            LabeledSlider(
                title = "Repeat speed",
                valueText = String.format("%.1f s", settings.repeatMs / 1000f),
                hint = "Keep looking at the edge to keep scrolling, once every…",
                value = settings.repeatMs.toFloat(),
                range = 400f..2000f,
                steps = 15,
            ) { v -> store.update { it.copy(repeatMs = v.toInt()) } }
            Divider()
            ToggleRow("Vibrate on scroll", "A tiny tick each time it scrolls", settings.vibrate) { v ->
                store.update { it.copy(vibrate = v) }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Camera on demand")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(5)) {
            Text("Auto-off when idle", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Text("Camera turns off after this long without scrolling", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            val autoOptions = listOf(30, 60, 120, 300, 0)
            Segmented(
                options = listOf("30s", "1m", "2m", "5m", "Never"),
                selected = autoOptions.indexOf(settings.autoOffSeconds).coerceAtLeast(0),
            ) { i -> store.update { it.copy(autoOffSeconds = autoOptions[i]) } }
            Spacer(Modifier.height(6.dp))
            Divider()
            ToggleRow(
                "Floating on/off dot",
                if (setup.accessibility) "Tap to toggle · drag to move · long-press opens app" else "Needs the accessibility service",
                settings.showBubble,
            ) { v -> store.update { it.copy(showBubble = v) } }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("How to use")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(6)) {
            BulletRow(Icons.Filled.Face, "Turn on", "Tap the floating dot, the \"Eye scroll\" quick-settings tile, or the big button above.")
            BulletRow(Icons.Filled.KeyboardArrowDown, "Scroll down", "Look at the bottom edge of the screen for a moment.", EsColors.Secondary)
            BulletRow(Icons.Filled.KeyboardArrowUp, "Scroll up", "Look at the top edge of the screen for a moment.")
            BulletRow(Icons.Filled.Star, "Best results", "Good light, phone at reading distance, recalibrate if you change position (e.g. sitting → lying).", EsColors.Warning)
        }

        Spacer(Modifier.height(18.dp))
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 28.dp), padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, tint = EsColors.Success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Private: camera frames are processed on-device and discarded instantly. Nothing is recorded or uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EsColors.TextMuted,
                )
            }
        }
    }
}

private fun phaseLabel(s: TrackingStatus): Pair<String, Color> = when (s.phase) {
    TrackingPhase.OFF -> "Off" to EsColors.TextMuted
    TrackingPhase.STARTING -> "Starting" to EsColors.Warning
    TrackingPhase.TRACKING -> "Tracking" to EsColors.Success
    TrackingPhase.NO_FACE -> "No face" to Color(0xFFFF8A3D)
    TrackingPhase.ERROR -> "Error" to EsColors.Danger
}

private fun subtitle(s: TrackingStatus, autoOff: Int): String = when {
    s.phase == TrackingPhase.STARTING -> "Starting the camera…"
    s.isActive -> buildString {
        append("${s.fps} checks/s")
        if (s.autoOffIn >= 0) append(" · auto-off in ${s.autoOffIn}s")
        append("\nGo to any app and look at the top or bottom edge")
    }
    autoOff > 0 -> "The camera turns on only now and switches off after ${if (autoOff >= 60) "${autoOff / 60} min" else "$autoOff s"} without scrolling."
    else -> "The camera turns on only when you start it."
}

@Composable
private fun LivePanel(status: TrackingStatus) {
    GlassCard(Modifier.fillMaxWidth().padding(top = 14.dp), padding = 18.dp) {
        Row(Modifier.height(150.dp), verticalAlignment = Alignment.CenterVertically) {
            GazeMeter(status.gaze, Modifier.width(16.dp).fillMaxHeight())
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Live", style = MaterialTheme.typography.labelSmall, color = EsColors.TextMuted)
                val (zoneText, zoneColor) = when {
                    !status.gaze.faceFound -> "Looking for your face" to Color(0xFFFF8A3D)
                    status.gaze.blinking -> "Blink ignored" to EsColors.TextMuted
                    status.gaze.zone == GazeZone.UP -> "Scroll-up zone" to EsColors.Primary
                    status.gaze.zone == GazeZone.DOWN -> "Scroll-down zone" to EsColors.Secondary
                    else -> "Reading" to EsColors.Success
                }
                val c by animateColorAsState(zoneColor, label = "zc")
                Text(zoneText, style = MaterialTheme.typography.titleLarge, color = c)
                Spacer(Modifier.height(10.dp))
                ProgressLine(status.gaze.dwellProgress, c)
                Spacer(Modifier.height(12.dp))
                Row {
                    Metric("Scrolls", "${status.scrollCount}", Modifier.weight(1f))
                    Metric(
                        "Last",
                        when (status.lastScroll) {
                            ScrollDirection.UP -> "↑ Up"
                            ScrollDirection.DOWN -> "↓ Down"
                            null -> "—"
                        },
                        Modifier.weight(1f),
                    )
                    Metric("Speed", "${status.fps}/s", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(value: Float, color: Color) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.07f))) {
        val w by animateDpAsState(maxWidth * value.coerceIn(0f, 1f), tween(90), label = "pl")
        Box(Modifier.width(w).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
        Text(label, style = MaterialTheme.typography.labelSmall, color = EsColors.TextMuted)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier, padding = 14.dp, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(EsColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = EsColors.Primary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = EsColors.Text)
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, tween(260), label = "seg")
        Box(
            Modifier.offset(x = x).width(segment).fillMaxHeight().clip(RoundedCornerShape(11.dp))
                .background(EsColors.Accent),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp)).clickableNoRipple { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (i == selected) Color(0xFF041019) else EsColors.TextMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    title: String,
    valueText: String,
    hint: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = EsColors.Text, modifier = Modifier.weight(1f))
        Text(valueText, style = MaterialTheme.typography.labelLarge, color = EsColors.Primary)
    }
    Text(hint, style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        valueRange = range,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = EsColors.Primary,
            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EsColors.Primary,
                uncheckedThumbColor = EsColors.TextMuted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.12f),
            ),
        )
    }
}
