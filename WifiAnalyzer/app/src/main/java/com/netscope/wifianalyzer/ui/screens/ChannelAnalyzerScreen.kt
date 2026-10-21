package com.netscope.wifianalyzer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.ads.AdPlacement
import com.netscope.wifianalyzer.net.AccessPoint
import com.netscope.wifianalyzer.net.WifiScanner
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.EmptyState
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.Pill
import com.netscope.wifianalyzer.ui.components.PremiumGate
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SegmentedTabs
import com.netscope.wifianalyzer.ui.components.SignalBars
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.components.signalColor
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val bands = listOf("2.4 GHz", "5 GHz", "6 GHz")
private val apColors = listOf(Palette.Cyan, Palette.Lilac, Palette.Mint, Palette.Gold, Palette.Pink, Palette.Orange, Palette.Sky, Palette.Rose, Palette.Blue)

@Composable
fun ChannelAnalyzerScreen(nav: NavHostController) {
    val context = LocalContext.current
    val leave = {
        AppGraph.ads.showInterstitial(context.findActivity(), AdPlacement.LEAVE_CHANNELS) { nav.popBackStack() }
        Unit
    }
    BackHandler(onBack = leave)
    ScreenScaffold(title = "Channel analyzer", onBack = leave) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PremiumGate(
                feature = "Channel analyzer",
                description = "See every nearby network on a live graph and find the least crowded channel for faster, more stable Wi-Fi.",
                icon = Icons.Rounded.Tune,
                preview = { ChannelPreview() },
            ) { ChannelContent() }
        }
    }
}

@Composable
private fun ChannelPreview() {
    val fake = remember {
        listOf(1 to -45, 1 to -70, 6 to -55, 6 to -62, 6 to -80, 11 to -50, 11 to -75, 3 to -85).mapIndexed { i, (ch, rssi) ->
            AccessPoint("Network $i", "$i", rssi, 2407 + ch * 5, ch, ch, 20, "2.4 GHz", "WPA2", null, i == 0)
        }
    }
    Column(Modifier.padding(20.dp)) {
        ChannelGraph(fake, "2.4 GHz", Modifier.fillMaxWidth().height(260.dp))
    }
}

@Composable
private fun ChannelContent() {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scanner = remember { WifiScanner(context) }
    val net by rememberNetState(5_000)
    var aps by remember { mutableStateOf(scanner.results()) }
    var band by remember { mutableIntStateOf(0) }
    var scanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun rescan(userInitiated: Boolean) {
        if (scanning) return
        scanning = true
        scope.launch {
            val fresh = scanner.scan()
            aps = withContext(Dispatchers.IO) { scanner.results() }
            scanning = false
            if (userInitiated && !fresh) {
                snackbar.show("Android limits Wi-Fi scans to 4 every 2 minutes. Showing the latest results.", SnackType.Info)
            }
        }
    }

    LaunchedEffect(net.hasLocation) {
        if (net.hasLocation) {
            rescan(false)
            while (true) {
                delay(30_000)
                rescan(false)
            }
        }
    }
    LaunchedEffect(aps) {
        val connected = aps.firstOrNull { it.isConnected }
        if (connected != null) band = bands.indexOf(connected.band).coerceAtLeast(0)
    }

    val bandName = bands[band]
    val inBand = aps.filter { it.band == bandName }
    val advice = remember(aps, bandName) { WifiScanner.advise(aps, bandName) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (net.needsLocation || !net.hasLocation) {
            item { LocationNeededCard(net) { rescan(true) } }
        }
        item {
            SegmentedTabs(bands, band, onSelect = { band = it })
        }
        item {
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${inBand.size} networks on $bandName", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (scanning) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { rescan(true) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Refresh, "Rescan") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (inBand.isEmpty()) {
                    EmptyState(Icons.Rounded.WifiFind, "No networks found", "Nothing detected on $bandName yet. Try rescanning in a moment.")
                } else {
                    ChannelGraph(inBand, bandName, Modifier.fillMaxWidth().height(240.dp))
                }
            }
        }
        if (inBand.isNotEmpty() && bandName != "6 GHz") {
            item {
                AppCard(Modifier.fillMaxWidth(), color = AppColors.ext.success.copy(alpha = 0.08f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = AppColors.ext.success)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Best channel: ${advice.bestChannel}", style = MaterialTheme.typography.titleMedium)
                            val cur = advice.currentChannel
                            Text(
                                when {
                                    cur == null -> "Least crowded channel nearby. Change it in your router's Wi-Fi settings."
                                    cur == advice.bestChannel -> "Great! Your router is already on the best channel."
                                    else -> "You're on channel $cur. Switching your router to ${advice.bestChannel} may reduce interference."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.ext.muted,
                            )
                        }
                    }
                }
            }
        }
        item { SectionTitle("Nearby networks") }
        items(inBand, key = { it.bssid + it.frequency }) { ap ->
            ApRow(ap, apColors[(inBand.indexOf(ap)) % apColors.size], Modifier.animateItem())
        }
    }
}

@Composable
private fun ApRow(ap: AccessPoint, color: Color, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth(), padding = PaddingValues(14.dp), color = if (ap.isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ap.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (ap.isConnected) {
                        Spacer(Modifier.width(6.dp))
                        Pill("Connected", MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    "Ch ${ap.channel} • ${ap.widthMhz} MHz • ${ap.standard ?: ap.band}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ext.muted,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ap.security.startsWith("Open")) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        ap.security,
                        tint = if (ap.security.startsWith("Open")) AppColors.ext.warning else AppColors.ext.subtle,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    SignalBars(WifiUtils.signalLevel(ap.rssi), Modifier.size(18.dp), signalColor(ap.rssi))
                }
                Text("${ap.rssi} dBm", style = MaterialTheme.typography.labelMedium, color = signalColor(ap.rssi))
            }
        }
    }
}

/** Classic channel graph: each network is a hill centred on its channel, as tall as its signal. */
@Composable
fun ChannelGraph(aps: List<AccessPoint>, band: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val grid = AppColors.ext.cardBorder
    val labelColor = AppColors.ext.muted
    val anim = remember { Animatable(0f) }
    LaunchedEffect(aps) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(700))
    }
    val (minCh, maxCh, ticks) = when (band) {
        "2.4 GHz" -> Triple(-1f, 15f, (1..13).toList())
        "5 GHz" -> Triple(30f, 170f, listOf(36, 52, 64, 100, 116, 132, 149, 165))
        else -> Triple(0f, 235f, listOf(1, 37, 69, 101, 133, 165, 197, 229))
    }
    Canvas(modifier) {
        val bottomPad = 22.dp.toPx()
        val leftPad = 28.dp.toPx()
        val h = size.height - bottomPad
        val w = size.width - leftPad
        fun x(ch: Float) = leftPad + (ch - minCh) / (maxCh - minCh) * w
        fun y(rssi: Int) = h - ((rssi + 100).coerceIn(0, 75) / 75f) * h

        listOf(-30, -50, -70, -90).forEach { level ->
            val yy = y(level)
            drawLine(grid, Offset(leftPad, yy), Offset(size.width, yy), 1f)
            val l = measurer.measure("$level", TextStyle(fontSize = 9.sp, color = labelColor))
            drawText(l, topLeft = Offset(0f, yy - l.size.height / 2f))
        }
        ticks.forEach { ch ->
            val l = measurer.measure("$ch", TextStyle(fontSize = 10.sp, color = labelColor))
            drawText(l, topLeft = Offset(x(ch.toFloat()) - l.size.width / 2f, h + 4.dp.toPx()))
        }
        aps.forEachIndexed { i, ap ->
            val c = apColors[i % apColors.size]
            val halfWidth = if (band == "2.4 GHz") 2f * (ap.widthMhz / 20f) else ap.widthMhz / 10f
            val left = x(ap.centerChannel - halfWidth)
            val right = x(ap.centerChannel + halfWidth)
            val top = h - (h - y(ap.rssi)) * anim.value
            val path = Path().apply {
                moveTo(left, h)
                cubicTo(left + (right - left) * 0.15f, top, right - (right - left) * 0.15f, top, right, h)
            }
            drawPath(path, c.copy(alpha = if (ap.isConnected) 0.35f else 0.16f))
            drawPath(path, c, style = Stroke(if (ap.isConnected) 3.dp.toPx() else 1.8.dp.toPx()))
            if (ap.ssid.isNotBlank() && i < 12) {
                val l = measurer.measure(ap.ssid.take(14), TextStyle(fontSize = 9.sp, color = c))
                drawText(l, topLeft = Offset(((left + right) / 2 - l.size.width / 2f).coerceIn(leftPad, size.width - l.size.width), (top + (h - top) * 0.25f - l.size.height).coerceAtLeast(0f)))
            }
        }
    }
}
