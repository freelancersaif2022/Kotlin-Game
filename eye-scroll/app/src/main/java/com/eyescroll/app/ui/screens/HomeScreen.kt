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
import com.eyescroll.app.data.PadSize
import com.eyescroll.app.data.ScrollSpeed
import com.eyescroll.app.data.ScrollStyle
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.scroll.PadBus
import com.eyescroll.app.scroll.ScrollDirection
import com.eyescroll.app.ui.components.BulletRow
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.PadGlyph
import com.eyescroll.app.ui.components.PowerOrb
import com.eyescroll.app.ui.components.SectionLabel
import com.eyescroll.app.ui.components.StatusPill
import com.eyescroll.app.ui.theme.EsColors

@Composable
fun HomeScreen(onSetup: () -> Unit, onPractice: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val settings by store.settings.collectAsStateWithLifecycle()
    val status by PadBus.status.collectAsStateWithLifecycle()
    val setup = rememberSetupState().state.value
    val padOn = settings.padEnabled && setup.accessibility

    val toggle: () -> Unit = {
        if (!setup.accessibility) onSetup() else store.update { it.copy(padEnabled = !it.padEnabled) }
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
            PadGlyph(Modifier.size(width = 26.dp, height = 44.dp), thumb = 0f, showFinger = false)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("FingerScroll", style = MaterialTheme.typography.titleLarge, color = EsColors.Text)
                Text("One-finger scrolling", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
            }
            when {
                !setup.accessibility -> StatusPill("Setup needed", EsColors.Warning)
                padOn -> StatusPill("Pad on", EsColors.Success, pulsing = true)
                else -> StatusPill("Pad off", EsColors.TextMuted)
            }
        }

        // Hero
        GlassCard(Modifier.fillMaxWidth().padding(top = 10.dp).enterAnimation(1), highlighted = padOn, padding = 22.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PowerOrb(active = padOn, enabled = true, onClick = toggle)
                Spacer(Modifier.height(14.dp))
                Text(
                    when {
                        !setup.accessibility -> "Finish setup to start"
                        padOn -> "Scroll pad is ON"
                        else -> "Tap to show the scroll pad"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = EsColors.Text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        !setup.accessibility -> "Turn on the FingerScroll accessibility service (30 seconds)."
                        padOn -> "Open any app and move your thumb slightly up or down on the pad."
                        else -> "Also available from the \"Scroll pad\" quick-settings tile."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = EsColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
                AnimatedVisibility(padOn && status.scrollCount > 0, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Row(Modifier.padding(top = 16.dp)) {
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
                        Metric("Mode", if (status.holding) "Holding" else settings.style.label, Modifier.weight(1f))
                    }
                }
            }
        }

        AnimatedVisibility(!setup.accessibility, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            GlassCard(Modifier.fillMaxWidth().padding(top = 14.dp), onClick = onSetup, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = EsColors.Warning)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Accessibility service is off", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
                        Text("Needed to show the pad and scroll other apps", style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted)
                    }
                    Text("Open", style = MaterialTheme.typography.labelLarge, color = EsColors.Primary)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp).enterAnimation(2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Filled.List, "Practice", Modifier.weight(1f), onPractice)
            QuickAction(Icons.Filled.Refresh, "Reset pad", Modifier.weight(1f)) {
                store.update { it.copy(padX = -1, padY = -1, padEnabled = true) }
            }
            QuickAction(Icons.Filled.Build, "Setup", Modifier.weight(1f), onSetup)
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Scrolling")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(3)) {
            Text("Direction", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(ScrollStyle.entries.map { it.label }, settings.style.ordinal) { i ->
                store.update { it.copy(style = ScrollStyle.entries[i]) }
            }
            Text(settings.style.description, style = MaterialTheme.typography.bodySmall, color = EsColors.TextMuted, modifier = Modifier.padding(top = 8.dp))
            Divider()
            LabeledSlider(
                title = "Sensitivity",
                valueText = "${settings.triggerDp} dp",
                hint = "How slight a movement starts a scroll · lower = more sensitive",
                value = settings.triggerDp.toFloat(),
                range = 3f..24f,
                steps = 20,
            ) { v -> store.update { it.copy(triggerDp = v.toInt()) } }
            Divider()
            LabeledSlider(
                title = "Scroll distance",
                valueText = "${(settings.maxDistance * 100).toInt()}% of screen",
                hint = "How far a full swipe on the pad scrolls",
                value = settings.maxDistance,
                range = 0.3f..0.85f,
            ) { v -> store.update { it.copy(maxDistance = v) } }
            Divider()
            Text("Scroll speed", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(ScrollSpeed.entries.map { it.label }, settings.speed.ordinal) { i ->
                store.update { it.copy(speed = ScrollSpeed.entries[i]) }
            }
            Divider()
            LabeledSlider(
                title = "Hold-to-scroll speed",
                valueText = when {
                    settings.holdSpeed < 0.34f -> "Relaxed"
                    settings.holdSpeed < 0.67f -> "Normal"
                    else -> "Rapid"
                },
                hint = "How fast it keeps scrolling while you hold",
                value = settings.holdSpeed,
                range = 0f..1f,
            ) { v -> store.update { it.copy(holdSpeed = v) } }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Pad")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(4)) {
            Text("Size", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Spacer(Modifier.height(10.dp))
            Segmented(PadSize.entries.map { it.label }, settings.padSize.ordinal) { i ->
                store.update { it.copy(padSize = PadSize.entries[i]) }
            }
            Divider()
            LabeledSlider(
                title = "Opacity when idle",
                valueText = "${(settings.padOpacity * 100).toInt()}%",
                hint = "The pad becomes fully visible while you touch it",
                value = settings.padOpacity,
                range = 0.3f..1f,
            ) { v -> store.update { it.copy(padOpacity = v) } }
            Divider()
            ToggleRow("Haptic feedback", "A light tick when a scroll starts", settings.haptics) { v ->
                store.update { it.copy(haptics = v) }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("How to use")
        GlassCard(Modifier.fillMaxWidth().enterAnimation(5)) {
            BulletRow(Icons.Filled.KeyboardArrowUp, "Swipe & release", "Move your thumb slightly up or down on the pad and let go — one smooth scroll. Move further for a longer scroll.")
            BulletRow(Icons.Filled.KeyboardArrowDown, "Swipe & hold", "Keep holding after the move and it keeps scrolling. Pull further to go faster. Let go to stop.", EsColors.Secondary)
            BulletRow(Icons.Filled.ThumbUp, "Move the pad", "Long-press the pad without moving, then drag it anywhere. It snaps to the screen edge.", EsColors.Success)
            BulletRow(Icons.Filled.Star, "Quick toggle", "Add the \"Scroll pad\" tile to quick settings to show or hide the pad in one tap.", EsColors.Warning)
        }

        Spacer(Modifier.height(18.dp))
        GlassCard(Modifier.fillMaxWidth().padding(bottom = 28.dp), padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, tint = EsColors.Success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Private: no camera, no internet, nothing on your screen is stored or sent anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EsColors.TextMuted,
                )
            }
        }
    }
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
