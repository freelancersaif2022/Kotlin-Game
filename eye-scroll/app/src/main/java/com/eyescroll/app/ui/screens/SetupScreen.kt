package com.eyescroll.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyescroll.app.ui.components.GhostButton
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.GradientButton
import com.eyescroll.app.ui.components.PadGlyph
import com.eyescroll.app.ui.components.StepBadge
import com.eyescroll.app.ui.theme.EsColors

@Composable
fun SetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val state = rememberSetupState().state.value
    var showDisclosure by remember { mutableStateOf(false) }
    var showGuide by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text("Quick setup", style = MaterialTheme.typography.headlineMedium, color = EsColors.Text, modifier = Modifier.enterAnimation(0))
            Spacer(Modifier.height(6.dp))
            Text(
                "Only one permission is needed — no camera, no internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = EsColors.TextMuted,
                modifier = Modifier.enterAnimation(1),
            )
            Spacer(Modifier.height(18.dp))

            GlassCard(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).animateContentSize().enterAnimation(2),
                highlighted = !state.accessibility,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepBadge(1, state.accessibility)
                    Spacer(Modifier.width(14.dp))
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, null, tint = EsColors.Primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Accessibility service", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
                    }
                    if (state.accessibility) Text("Done", style = MaterialTheme.typography.labelMedium, color = EsColors.Success)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Lets FingerScroll show the floating pad and scroll the app you are using. It does not read or store your messages, typing or passwords.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EsColors.TextMuted,
                )
                AnimatedVisibility(
                    visible = showGuide && !state.accessibility,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) { AccessibilityGuide(onOpenAppInfo = { openAppDetails(context) }) }
                if (!state.accessibility) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("Turn on in Settings", { showDisclosure = true }, Modifier.weight(1f))
                        GhostButton(if (showGuide) "Hide guide" else "Show me how", { showGuide = !showGuide })
                    }
                }
            }

            AnimatedVisibility(state.accessibility, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), padding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PadGlyph(Modifier.size(width = 56.dp, height = 110.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("The pad is on your screen", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Look at the right edge. Move your thumb slightly up or down on it and release — or keep holding to keep scrolling.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EsColors.TextMuted,
                            )
                        }
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp).enterAnimation(3), padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = EsColors.Success, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Privacy: FingerScroll has no camera and no internet permission and collects nothing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EsColors.TextMuted,
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            GradientButton(
                text = if (state.requiredDone) "All set — let's go" else "Turn on the service above",
                onClick = onDone,
                enabled = state.requiredDone,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!state.requiredDone) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "I'll do it later",
                    style = MaterialTheme.typography.labelLarge,
                    color = EsColors.TextMuted,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(10.dp))
                        .clickableNoRipple(onDone)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            containerColor = EsColors.SurfaceHigh,
            icon = { Icon(Icons.Filled.Info, null, tint = EsColors.Primary) },
            title = { Text("Accessibility permission", color = EsColors.Text) },
            text = {
                Text(
                    "FingerScroll uses Android's Accessibility API to:\n\n" +
                        "• show the floating scroll pad above other apps\n" +
                        "• scroll the app you are using when you move your finger on the pad\n\n" +
                        "It does NOT read, store or send your messages, typing, passwords or other screen content, and it has no internet access.\n\n" +
                        "On the next screen tap \"FingerScroll\" and switch it on.",
                    color = EsColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    openAccessibilitySettings(context)
                }) { Text("Agree & open settings", color = EsColors.Primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text("Not now", color = EsColors.TextMuted) }
            },
        )
    }
}

@Composable
private fun AccessibilityGuide(onOpenAppInfo: () -> Unit) {
    Column(Modifier.padding(top = 14.dp)) {
        val steps = listOf(
            "Tap \"Turn on in Settings\" below",
            "Find \"Installed apps\" or \"Downloaded apps\"",
            "Tap \"FingerScroll – one-finger scrolling\"",
            "Switch \"Use FingerScroll\" ON and tap Allow",
            "Come back here — this step turns green",
        )
        steps.forEachIndexed { i, s ->
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(EsColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = EsColors.Primary) }
                Spacer(Modifier.width(12.dp))
                Text(s, style = MaterialTheme.typography.bodyMedium, color = EsColors.Text)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(EsColors.Warning.copy(alpha = 0.08f)).padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = EsColors.Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Says \"Restricted setting\"?", style = MaterialTheme.typography.titleMedium, color = EsColors.Warning)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Android 13+ protects apps installed from an APK file. Open App info → tap ⋮ (top-right) → \"Allow restricted settings\", then switch FingerScroll on again.",
                style = MaterialTheme.typography.bodySmall,
                color = EsColors.TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            GhostButton("Open App info", onOpenAppInfo, Modifier.fillMaxWidth())
        }
    }
}
