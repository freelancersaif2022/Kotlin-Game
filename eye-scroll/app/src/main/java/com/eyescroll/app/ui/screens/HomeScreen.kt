package com.eyescroll.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
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
import com.eyescroll.app.data.AppSettings
import com.eyescroll.app.data.ScrollDistance
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.gesture.GestureMode
import com.eyescroll.app.gesture.ScrollDirection
import com.eyescroll.app.gesture.TrackingBus
import com.eyescroll.app.gesture.TrackingPhase
import com.eyescroll.app.gesture.TrackingStatus
import com.eyescroll.app.service.GestureTrackingService
import com.eyescroll.app.ui.components.AirGestureGlyph
import com.eyescroll.app.ui.components.BulletRow
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.PowerOrb
import com.eyescroll.app.ui.components.SectionLabel
import com.eyescroll.app.ui.components.StatusPill
import com.eyescroll.app.ui.theme.EsColors

@Composable
fun HomeScreen(onSetup: () -> Unit, onPractice: () -> Unit, onTest: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val settings by store.settings.collectAsStateWithLifecycle()
    val status by TrackingBus.status.collectAsStateWithLifecycle()
    val setup = rememberSetupState().state.value

    val toggle: () -> Unit = {
        when {
            status.isActive -> GestureTrackingService.stop(context)
            setup.camera -> GestureTrackingService.start(context)
            else -> onSetup()
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
            AirGestureGlyph(Modifier.size(width = 34.dp, height = 44.dp), offset = 0f, glow = false)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("AirScroll", style = MaterialTheme.typography.titleLarge, color = EsColors.Text)
                Text("Scroll without touching", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
            }
            val (label, color) = phaseLabel(status)
            StatusPill(label, color, pulsing = status.isActive)
        }

        // Hero
        GlassCard(Modifier.fillMaxWidth().padding(top = 10.dp).enterAnimation(1), highlighted = status.isActive, padding = 22.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PowerOrb(active = status.isActive, enabled = true, onClick = toggle)
                Spacer(Modifier.height(14.dp))
                Text(
                    when {
                        status.isActive -> "Air scrolling is ON"
                        !setup.camera -> "Finish setup to start"
                        else -> "Tap to start air scrolling"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = EsColors.Text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle(status, settings),
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
                AnimatedVisibility(status.isActive, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Row(Modifier.padding(top = 16.dp)) {
                        Metric("Hand", if (status.hand.handFound) "Seen ✓" else "—", Modifier.weight(1f))
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
                        }.joinToString(", ")
                        Text("Still needed: $missing", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
                    }
                    Text("Open", style = MaterialTheme.typography.labelLarge, color = EsColors.Primary)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp).enterAnimation(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Filled.Face, "Test", Modifier.weight(1f)) { if (setup.camera) onTest() else onSetup() }
            QuickAction(Icons.Filled.List, "Practice", Modifier.weight(1f), onPractice)
            QuickAction(Icons.Filled.Build, "Setup", Modifier.weight(1f), onSetup)
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Gesture")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(3)) {
            Text("Mode", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(listOf("Swipe", "Point & hold"), settings.mode.ordinal) { i ->
                store.update { it.copy(mode = GestureMode.entries[i]) }
            }
            Text(
                if (settings.mode == GestureMode.SWIPE) "Flick your finger up or down in the air — one scroll per flick."
                else "Raise or lower your finger and hold it — keeps scrolling until you bring it back.",
                style = MaterialTheme.typography.bodySmall,
                color = EsColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
            Divider()
            Text("Direction", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(listOf("Natural", "Reversed"), if (settings.natural) 0 else 1) { i ->
                store.update { it.copy(natural = i == 0) }
            }
            Text(
                when {
                    settings.mode == GestureMode.SWIPE && settings.natural -> "Flick up = page moves up (like swiping the screen)."
                    settings.mode == GestureMode.SWIPE -> "Flick up = go back up the page."
                    settings.natural -> "Finger up = scroll up, finger down = scroll down."
                    else -> "Finger up = scroll down, finger down = scroll up."
                },
                style = MaterialTheme.typography.bodySmall,
                color = EsColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
            Divider()
            LabeledSlider(
                title = "Sensitivity",
                valueText = when {
                    settings.sensitivity < 0.34f -> "Big moves"
                    settings.sensitivity < 0.67f -> "Balanced"
                    else -> "Tiny moves"
                },
                hint = "Higher reacts to a smaller finger movement",
                value = settings.sensitivity,
                range = 0f..1f,
            ) { v -> store.update { it.copy(sensitivity = v) } }
            if (settings.mode == GestureMode.HOLD) {
                Divider()
                LabeledSlider(
                    title = "Hold repeat speed",
                    valueText = String.format("%.1f s", settings.repeatMs / 1000f),
                    hint = "While you hold, scroll once every…",
                    value = settings.repeatMs.toFloat(),
                    range = 300f..1500f,
                    steps = 11,
                ) { v -> store.update { it.copy(repeatMs = v.toInt()) } }
            }
            Divider()
            Text("Scroll distance", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(ScrollDistance.entries.map { it.label }, settings.scrollDistance.ordinal) { i ->
                store.update { it.copy(scrollDistance = ScrollDistance.entries[i]) }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Camera on demand")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(4)) {
            Text("Auto-off when idle", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Text("Camera turns off after this long without scrolling", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            val autoOptions = listOf(30, 60, 120, 300, 0)
            Segmented(listOf("30s", "1m", "2m", "5m", "Never"), autoOptions.indexOf(settings.autoOffSeconds).coerceAtLeast(0)) { i ->
                store.update { it.copy(autoOffSeconds = autoOptions[i]) }
            }
            Divider()
            Text("Camera speed", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Text("Checks per second · higher feels more instant", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            val fpsOptions = listOf(15, 24, 30)
            Segmented(listOf("15 /s · saver", "24 /s", "30 /s · fast"), fpsOptions.indexOf(settings.targetFps).coerceAtLeast(0)) { i ->
                store.update { it.copy(targetFps = fpsOptions[i]) }
            }
            Divider()
            ToggleRow(
                "Floating on/off dot",
                if (setup.accessibility) "Tap to toggle camera · drag to move · long-press opens app" else "Needs the accessibility service",
                settings.showBubble,
            ) { v -> store.update { it.copy(showBubble = v) } }
            Divider()
            ToggleRow("Vibrate on scroll", "A tiny tick each time it scrolls", settings.vibrate) { v ->
                store.update { it.copy(vibrate = v) }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("How to use")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(5)) {
            BulletRow(Icons.Filled.Face, "Turn on", "Tap the floating dot, the \"Air scroll\" quick-settings tile, or the big button above.")
            BulletRow(Icons.Filled.KeyboardArrowUp, "Swipe mode", "Point your index finger at the phone and flick it up or down, 30–150 cm from the camera.")
            BulletRow(Icons.Filled.KeyboardArrowDown, "Point & hold mode", "Hold your finger still for half a second, then raise or lower it and keep it there.", EsColors.Secondary)
            BulletRow(Icons.Filled.Star, "Best results", "Good light, whole hand visible to the front camera, phone resting on a stand or table.", EsColors.Warning)
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
    TrackingPhase.TRACKING -> "Hand seen" to EsColors.Success
    TrackingPhase.NO_HAND -> "Show hand" to Color(0xFFFF8A3D)
    TrackingPhase.ERROR -> "Error" to EsColors.Danger
}

private fun subtitle(s: TrackingStatus, settings: AppSettings): String = when {
    s.phase == TrackingPhase.STARTING -> "Starting the camera…"
    s.isActive -> buildString {
        append(if (settings.mode == GestureMode.SWIPE) "Flick your finger up or down in the air" else "Raise or lower your finger and hold")
        if (s.autoOffIn >= 0) append("\nAuto-off in ${s.autoOffIn}s")
    }
    settings.autoOffSeconds > 0 -> "The camera turns on only now and switches off after ${if (settings.autoOffSeconds >= 60) "${settings.autoOffSeconds / 60} min" else "${settings.autoOffSeconds} s"} without scrolling."
    else -> "The camera turns on only when you start it."
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
