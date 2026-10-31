package com.spectral.ghostdetector.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectral.ghostdetector.R
import com.spectral.ghostdetector.audio.Loop
import com.spectral.ghostdetector.audio.Sfx
import com.spectral.ghostdetector.ads.BannerAd
import com.spectral.ghostdetector.ads.NativeAdCard
import com.spectral.ghostdetector.ui.components.Badge
import com.spectral.ghostdetector.ui.components.BlinkingDot
import com.spectral.ghostdetector.ui.components.DisclaimerText
import com.spectral.ghostdetector.ui.components.GhostBackground
import com.spectral.ghostdetector.ui.components.GlassIconButton
import com.spectral.ghostdetector.ui.components.GlassPanel
import com.spectral.ghostdetector.ui.components.NeonButton
import com.spectral.ghostdetector.ui.components.PulseRings
import com.spectral.ghostdetector.ui.components.RadarScope
import com.spectral.ghostdetector.ui.components.SectionLabel
import com.spectral.ghostdetector.ui.components.StatTile
import com.spectral.ghostdetector.ui.components.soundClick
import com.spectral.ghostdetector.ui.findActivity
import com.spectral.ghostdetector.ui.rememberApp
import com.spectral.ghostdetector.ui.scanner.rememberMagnetometer
import com.spectral.ghostdetector.ui.theme.GColors
import com.spectral.ghostdetector.ui.theme.GFonts
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
fun HomeScreen(
    onFindGhost: () -> Unit,
    onHistory: () -> Unit,
    onPremium: () -> Unit,
    onSettings: () -> Unit,
) {
    val app = rememberApp()
    val context = LocalContext.current
    val premium by app.premium.collectAsStateWithLifecycle()
    val total by app.prefs.totalDetections.collectAsStateWithLifecycle(0)
    val mag = rememberMagnetometer()

    var showExit by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf("") }
    var emf by remember { mutableFloatStateOf(0.4f) }
    var temp by remember { mutableFloatStateOf(21.4f) }
    var evp by remember { mutableFloatStateOf(-62f) }

    DisposableEffect(Unit) {
        app.sounds.startLoop(Loop.DRONE, 0.32f)
        onDispose { app.sounds.stopLoop(Loop.DRONE) }
    }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            clock = fmt.format(Date())
            val target = 0.25f + mag.deviationMg.coerceAtMost(40f) * 0.35f + Random.nextFloat() * 0.5f
            emf += (target - emf) * 0.35f
            temp = (temp + (Random.nextFloat() - 0.5f) * 0.12f).coerceIn(20.6f, 22.3f)
            evp = (evp + (Random.nextFloat() - 0.5f) * 3f).coerceIn(-70f, -52f)
            delay(250)
        }
    }
    BackHandler { showExit = true }

    GhostBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                // ---------- top bar ----------
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(46.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GHOST DETECTOR", style = MaterialTheme.typography.headlineSmall, color = GColors.TextPrimary)
                            if (premium) {
                                Spacer(Modifier.width(6.dp))
                                Badge("PRO", GColors.Amber)
                            }
                        }
                        Text("PARANORMAL SCANNER", color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    if (!premium) {
                        GlassIconButton(Icons.Rounded.WorkspacePremium, "Premium", tint = GColors.Amber, active = true, onClick = onPremium)
                        Spacer(Modifier.width(8.dp))
                    }
                    GlassIconButton(Icons.Rounded.Settings, "Settings", onClick = onSettings)
                }
                Spacer(Modifier.height(14.dp))
                // ---------- status strip ----------
                GlassPanel(Modifier.fillMaxWidth(), corner = 12.dp, padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BlinkingDot()
                        Spacer(Modifier.width(8.dp))
                        Text("SYSTEM ONLINE", color = GColors.Neon, fontFamily = GFonts.Mono, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text("SENSORS 4/4", color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text(clock, color = GColors.TextPrimary, fontFamily = GFonts.Mono, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                // ---------- main radar button ----------
                FindGhostButton(onClick = {
                    app.sounds.play(Sfx.SCAN_START)
                    onFindGhost()
                })
                Spacer(Modifier.height(6.dp))
                Text(
                    "TAP TO BEGIN PARANORMAL SCAN",
                    color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 11.sp, letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(20.dp))
                // ---------- live sensors ----------
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("EMF", "%.2f".format(emf), "mG", Modifier.weight(1f))
                    StatTile("TEMP", "%.1f".format(temp), "°C", Modifier.weight(1f))
                    StatTile("EVP", "%.0f".format(evp), "dB", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureTile(Icons.Rounded.History, "GHOST LOG", "$total found", onHistory)
                    if (premium) {
                        FeatureTile(Icons.Rounded.WorkspacePremium, "PRO ACTIVE", "No ads", onPremium, GColors.Amber)
                    } else {
                        FeatureTile(Icons.Rounded.WorkspacePremium, "REMOVE ADS", "Go premium", onPremium, GColors.Amber)
                    }
                    FeatureTile(Icons.Rounded.HelpOutline, "HOW IT WORKS", "Read first", { showHelp = true })
                }
                Spacer(Modifier.height(12.dp))
                NativeAdCard(Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                DisclaimerText(Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
            BannerAd()
        }
    }

    if (showExit) {
        GhostDialog(onDismiss = { showExit = false }) {
            Text("LEAVE THE SPECTRAL REALM?", style = MaterialTheme.typography.headlineSmall, color = GColors.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("The spirits will still be here when you come back…", color = GColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton("Stay", Modifier.weight(1f), icon = Icons.Rounded.Radar) { showExit = false }
                NeonButton("Exit", Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.ExitToApp, color = GColors.Danger, filled = false) {
                    context.findActivity()?.finish()
                }
            }
        }
    }
    if (showHelp) {
        GhostDialog(onDismiss = { showHelp = false }) {
            Text("HOW IT WORKS", style = MaterialTheme.typography.headlineSmall, color = GColors.Neon)
            Spacer(Modifier.height(10.dp))
            listOf(
                "1. Tap FIND GHOST and point your back camera around the room.",
                "2. The radar calibrates, then entities appear inside green tracking frames.",
                "3. Tap a frame to view the entity's name, age, type and threat level.",
                "4. Use EVP to 'listen', the torch for dark rooms and NV for night vision.",
                "5. The EMF needle also reacts to real magnets & electronics near your phone.",
            ).forEach {
                Text(it, color = GColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "This is a PRANK / entertainment app. Ghost detections, ages and names are randomly generated — it cannot detect real ghosts.",
                color = GColors.Amber, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            NeonButton("Got it", Modifier.fillMaxWidth()) { showHelp = false }
        }
    }
}

@Composable
private fun FindGhostButton(onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "btn")
    val glow by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "glow")
    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        PulseRings(Modifier.fillMaxSize(), periodMs = 2800)
        RadarScope(Modifier.size(250.dp))
        Box(
            Modifier
                .size(132.dp)
                .scale(0.97f + glow * 0.03f)
                .drawBehind {
                    drawCircle(GColors.Neon.copy(alpha = 0.18f * glow), radius = size.minDimension * 0.72f)
                    drawCircle(GColors.Neon.copy(alpha = 0.12f * glow), radius = size.minDimension * 0.62f)
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF1B5E3B), Color(0xFF07160F))))
                .border(2.dp, GColors.Neon.copy(alpha = 0.6f + 0.4f * glow), CircleShape)
                .soundClick(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Radar, null, tint = GColors.Neon, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(4.dp))
                Text("FIND", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp), color = GColors.TextPrimary)
                Text("GHOST", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp), color = GColors.Neon)
            }
        }
    }
}

@Composable
private fun RowScope.FeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    color: Color = GColors.Neon,
) {
    GlassPanel(Modifier.weight(1f).soundClick(onClick = onClick), corner = 14.dp, padding = 10.dp) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, color = GColors.TextPrimary, fontFamily = GFonts.Orbitron, fontSize = 10.sp, letterSpacing = 1.sp, maxLines = 1)
        SectionLabel(subtitle)
    }
}

@Composable
fun GhostDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(Modifier.fillMaxWidth(), corner = 22.dp, borderColor = GColors.Neon.copy(alpha = 0.5f), padding = 20.dp) {
            Box(Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Rounded.Close, "Close", tint = GColors.TextSecondary,
                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp).soundClick(onClick = onDismiss),
                )
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
    }
}
