package com.netscope.wifianalyzer.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.AppDialog
import com.netscope.wifianalyzer.ui.components.LocalPremiumActions
import com.netscope.wifianalyzer.ui.components.LocalTour
import com.netscope.wifianalyzer.ui.components.Pill
import com.netscope.wifianalyzer.ui.components.ProBadge
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SignalBars
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.components.formatRemaining
import com.netscope.wifianalyzer.ui.components.pressScale
import com.netscope.wifianalyzer.ui.components.rememberPremiumState
import com.netscope.wifianalyzer.ui.components.signalColor
import com.netscope.wifianalyzer.ui.components.tourTarget
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.navigation.navigateTab
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlinx.coroutines.delay

private val tips = listOf(
    "Place your router high up and in the middle of your home, away from walls and metal objects.",
    "5 GHz is faster but shorter range. Use 2.4 GHz for far rooms and smart home devices.",
    "Restart your router once a month to clear memory and keep speeds steady.",
    "Microwaves, baby monitors and Bluetooth speakers can interfere with 2.4 GHz Wi-Fi.",
    "Use the Channel Analyzer to move your router to the least crowded channel.",
    "Update your router firmware for better speed and security fixes.",
    "Use WPA3 or WPA2 security and a long password to keep strangers off your Wi-Fi.",
)

@Composable
fun HomeScreen(nav: NavHostController) {
    val context = LocalContext.current
    val net by rememberNetState()
    val premium by rememberPremiumState()
    val history by AppGraph.history.results.collectAsState()
    val unit by AppGraph.prefs.unit.collectAsState()
    val tour = LocalTour.current
    val actions = LocalPremiumActions.current
    var showExit by remember { mutableStateOf(false) }
    var tipIndex by remember { mutableIntStateOf((System.currentTimeMillis() / 86_400_000L % tips.size).toInt()) }

    BackHandler { showExit = true }
    LaunchedEffect(Unit) {
        while (true) {
            delay(9_000)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WiFi Analyzer", style = MaterialTheme.typography.headlineMedium)
                Text("Speed test & network tools", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
            }
            when {
                premium.subscribed -> ProBadge(text = "PREMIUM")
                premium.rewardActive -> Pill("PRO ${formatRemaining(premium.rewardRemainingMs).substringBefore(" ")}", AppColors.ext.gold, icon = Icons.Rounded.Timer)
                else -> Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppColors.ext.premiumGradient)
                        .clickable { actions.openPaywall() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Go PRO", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = { nav.navigate(Routes.HELP) }) {
                Icon(Icons.Rounded.HelpOutline, "Help", tint = AppColors.ext.muted)
            }
        }

        HeroCard(net, nav = nav, modifier = Modifier.tourTarget(tour, "hero"))

        if (net.needsLocation || !net.wifiOn) {
            Spacer(Modifier.height(12.dp))
            LocationNeededCard(net)
        }

        Spacer(Modifier.height(16.dp))
        Box(Modifier.tourTarget(tour, "unlock")) {
            PremiumStatusCard(premium.subscribed, premium.rewardActive, premium.rewardRemainingMs, premium.rewardAdsWatched) {
                actions.openRewardUnlock()
            }
        }

        // Last result
        SectionTitle("Last speed test", action = if (history.isNotEmpty()) "History" else null, onAction = { nav.navigate(Routes.HISTORY) })
        val last = history.firstOrNull()
        AppCard(Modifier.fillMaxWidth(), onClick = { if (last == null) nav.navigateTab(Routes.speed(true)) else nav.navigate(Routes.HISTORY) }) {
            if (last == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("No tests yet", style = MaterialTheme.typography.titleSmall)
                        Text("Tap to run your first speed test", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat(Icons.Rounded.ArrowDownward, "Download", WifiUtils.formatSpeed(last.downloadMbps, unit).first, unit.label, AppColors.ext.download)
                    MiniStat(Icons.Rounded.ArrowUpward, "Upload", WifiUtils.formatSpeed(last.uploadMbps, unit).first, unit.label, AppColors.ext.upload)
                    MiniStat(Icons.Rounded.Timer, "Ping", last.pingMs.toInt().toString(), "ms", AppColors.ext.ping)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${last.network} • " + DateUtils.getRelativeTimeSpanString(last.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.ext.subtle,
                )
            }
        }

        // Tools
        SectionTitle("Network tools", action = "See all", onAction = { nav.navigateTab(Routes.TOOLS) })
        val featured = homeTools
        featured.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tool ->
                    ToolTile(tool, locked = tool.pro && !premium.isPremium, modifier = Modifier.weight(1f)) { openTool(nav, tool) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // Tip of the moment
        Spacer(Modifier.height(4.dp))
        AppCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Lightbulb, null, tint = AppColors.ext.gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Wi-Fi tip", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.animation.AnimatedContent(tipIndex, label = "tip") { i ->
                        Text(tips[i], style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showExit) {
        AppDialog(
            onDismiss = { showExit = false },
            icon = Icons.Rounded.Wifi,
            title = "Leaving so soon?",
            message = "Enjoying WiFi Analyzer? A quick rating helps us a lot.",
            confirmText = "Rate us",
            onConfirm = {
                showExit = false
                openPlayStore(context)
            },
            dismissText = "Exit app",
            onDismissClick = {
                showExit = false
                context.findActivity()?.finish()
            },
        )
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, unit: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.ext.muted)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(3.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = AppColors.ext.muted, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun HeroCard(net: NetState, nav: NavHostController, modifier: Modifier = Modifier) {
    val snap = net.snap
    val tour = LocalTour.current
    val onHero = Color.White
    AppCard(
        modifier.fillMaxWidth(),
        brush = AppColors.ext.heroGradient,
        padding = PaddingValues(20.dp),
        onClick = { nav.navigate(Routes.WIFI_INFO) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (snap.wifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, null, tint = onHero)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        !snap.wifiConnected -> "Not on Wi-Fi"
                        snap.ssid != null -> snap.ssid
                        net.needsLocation -> "Wi-Fi connected"
                        else -> "Wi-Fi network"
                    },
                    color = onHero,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (snap.wifiConnected) {
                        listOfNotNull(
                            "Connected",
                            snap.band.ifBlank { null },
                            snap.channel.takeIf { it > 0 }?.let { "Ch $it" },
                        ).joinToString(" • ")
                    } else {
                        "Using ${snap.connectionType.lowercase()}"
                    },
                    color = onHero.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (snap.hasSignal) SignalBars(WifiUtils.signalLevel(snap.rssi), Modifier.size(26.dp), color = signalColor(snap.rssi))
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (snap.hasSignal) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${snap.rssi}", color = onHero, style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(4.dp))
                        Text("dBm", color = onHero.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(
                        "${WifiUtils.qualityLabel(snap.rssi)} signal • ${snap.signalPercent}%",
                        color = signalColor(snap.rssi),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { snap.signalPercent / 100f },
                        modifier = Modifier.fillMaxWidth(0.85f).height(6.dp).clip(CircleShape),
                        color = signalColor(snap.rssi),
                        trackColor = Color.White.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        listOfNotNull(
                            snap.linkSpeed.takeIf { it > 0 }?.let { "Link $it Mbps" },
                            snap.ip?.let { "IP $it" },
                        ).joinToString("   "),
                        color = onHero.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else {
                    Text(
                        if (snap.hasInternet) "You can still run a speed test on ${snap.connectionType.lowercase()}." else "No internet connection",
                        color = onHero.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            GoButton(Modifier.tourTarget(tour, "go")) { nav.navigateTab(Routes.speed(true)) }
        }
    }
}

@Composable
private fun GoButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "go")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800)), label = "ring")
    val breathe by t.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "breathe")
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(104.dp)
                .graphicsLayer {
                    scaleX = 0.75f + ring * 0.3f
                    scaleY = 0.75f + ring * 0.3f
                    alpha = 1f - ring
                }
                .clip(CircleShape)
                .border(3.dp, Palette.Cyan, CircleShape),
        )
        Box(
            Modifier
                .size(84.dp)
                .graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                }
                .pressScale(interaction)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Palette.Cyan, Palette.Blue, Palette.Violet)))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("GO", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PremiumStatusCard(subscribed: Boolean, rewardActive: Boolean, remainingMs: Long, watched: Int, onUnlock: () -> Unit) {
    when {
        subscribed -> AppCard(Modifier.fillMaxWidth(), color = AppColors.ext.gold.copy(alpha = 0.08f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(AppColors.ext.premiumGradient),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Premium active", style = MaterialTheme.typography.titleSmall)
                    Text("All Pro features unlocked. No ads. Thank you!", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                }
            }
        }
        rewardActive -> AppCard(Modifier.fillMaxWidth(), color = AppColors.ext.gold.copy(alpha = 0.08f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = AppColors.ext.gold)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pro unlocked • ${formatRemaining(remainingMs)} left", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (remainingMs.toFloat() / AppConfig.REWARD_DURATION_MS).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = AppColors.ext.gold,
                        trackColor = AppColors.ext.gold.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }
        else -> AppCard(Modifier.fillMaxWidth(), onClick = onUnlock) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(AppColors.ext.premiumGradient),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayCircle, null, tint = Color.White) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Unlock all Pro tools free", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Watch ${AppConfig.REWARD_ADS_REQUIRED} short ads for 12 hours of Premium",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.ext.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(AppConfig.REWARD_ADS_REQUIRED) { i ->
                            Box(
                                Modifier
                                    .width(28.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(if (i < watched) Palette.Mint else MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                    }
                }
                SoftButton("Watch", onClick = onUnlock, tint = AppColors.ext.gold, height = 40.dp)
            }
        }
    }
}
