package com.netscope.wifianalyzer.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.netscope.wifianalyzer.ui.components.SegmentedTabs
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.data.HeatPoint
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.AppDialog
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.PremiumGate
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.StatTile
import com.netscope.wifianalyzer.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

private const val ROWS = 8
private const val COLS = 6

private fun heatColor(rssi: Float): Color {
    val stops = listOf(-90f to Color(0xFFF43F5E), -78f to Color(0xFFFB923C), -68f to Color(0xFFFBBF24), -58f to Color(0xFF22D3EE), -48f to Color(0xFF34D399))
    if (rssi <= stops.first().first) return stops.first().second
    if (rssi >= stops.last().first) return stops.last().second
    val i = stops.indexOfFirst { it.first > rssi }
    val (a, ca) = stops[i - 1]
    val (b, cb) = stops[i]
    return lerp(ca, cb, (rssi - a) / (b - a))
}

private fun cellName(row: Int, col: Int) = "${'A' + col}${row + 1}"

@Composable
fun HeatmapScreen(nav: NavHostController) {
    var confirmReset by remember { mutableStateOf(false) }
    val net by rememberNetState()
    val key = net.snap.networkKey
    val points = remember(key) { mutableStateListOf<HeatPoint>().apply { if (key != null) addAll(AppGraph.heatmap.load(key)) } }
    val premiumGate: @Composable (@Composable () -> Unit) -> Unit = { body ->
        PremiumGate(
            feature = "Signal heatmap",
            description = "Scan rooms with your live camera to see where Wi-Fi is strong or weak, and map your home to find the best spot for your router or extender.",
            icon = Icons.Rounded.GridOn,
            preview = {
                val demo = remember {
                    List(ROWS * COLS) { i -> HeatPoint(i / COLS, i % COLS, -45 - ((i / COLS) * 5 + (i % COLS) * 3)) }.filterIndexed { i, _ -> i % 3 == 0 }
                }
                Column(Modifier.padding(20.dp)) { HeatGrid(demo, null, 0f) { _, _ -> } }
            },
        ) { body() }
    }
    ScreenScaffold(
        title = "Signal heatmap",
        onBack = { nav.popBackStack() },
        actions = {
            if (points.isNotEmpty() && AppGraph.premium.isPremium) {
                IconButton(onClick = { confirmReset = true }) { Icon(Icons.Rounded.RestartAlt, "Reset map") }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            premiumGate {
                var mode by rememberSaveable { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize()) {
                    SegmentedTabs(
                        listOf("Live camera", "Floor map"),
                        mode,
                        onSelect = { mode = it },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    )
                    if (mode == 0) {
                        if (net.needsLocation || !net.wifiOn) {
                            LocationNeededCard(net, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp))
                        }
                        CameraSignalView(key, Modifier.fillMaxWidth().weight(1f).padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
                    } else {
                        HeatmapContent(net, key, points)
                    }
                }
            }
        }
    }
    if (confirmReset) {
        AppDialog(
            onDismiss = { confirmReset = false },
            icon = Icons.Rounded.RestartAlt,
            title = "Start a new map?",
            message = "All measured spots for this network will be cleared.",
            confirmText = "Reset map",
            onConfirm = {
                points.clear()
                key?.let { AppGraph.heatmap.save(it, emptyList()) }
                confirmReset = false
            },
            dismissText = "Cancel",
        )
    }
}

@Composable
private fun HeatmapContent(net: NetState, key: String?, points: MutableList<HeatPoint>) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var measuring by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var measureProgress by remember { mutableStateOf(0f) }

    fun measure(row: Int, col: Int) {
        if (measuring != null) return
        if (!net.snap.hasSignal || key == null) {
            snackbar.show("Connect to Wi-Fi to measure signal strength", SnackType.Error)
            return
        }
        measuring = row to col
        scope.launch {
            val samples = mutableListOf<Int>()
            repeat(6) { i ->
                val s = WifiUtils.snapshot(context)
                if (s.hasSignal) samples += s.rssi
                measureProgress = (i + 1) / 6f
                delay(500)
            }
            val avg = if (samples.isEmpty()) -100 else samples.average().toInt()
            points.removeAll { it.row == row && it.col == col }
            points += HeatPoint(row, col, avg)
            AppGraph.heatmap.save(key, points.toList())
            measuring = null
            measureProgress = 0f
            snackbar.show("${cellName(row, col)}: $avg dBm • ${WifiUtils.qualityLabel(avg)}", SnackType.Success)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        if (net.needsLocation || !net.wifiOn) {
            LocationNeededCard(net)
            Spacer(Modifier.height(12.dp))
        }
        AppCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TouchApp, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Imagine the grid is your home's floor plan. Stand in a spot, tap the matching square and hold still for 3 seconds. Repeat in each room.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ext.muted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        HeatGrid(points, measuring, measureProgress) { r, c -> measure(r, c) }
        Spacer(Modifier.height(12.dp))
        Legend()
        Spacer(Modifier.height(14.dp))
        val best = points.maxByOrNull { it.rssi }
        val worst = points.minByOrNull { it.rssi }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Spots", "${points.size}", "of ${ROWS * COLS}", Icons.Rounded.GridOn, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatTile("Best", best?.let { cellName(it.row, it.col) } ?: "--", best?.let { "${it.rssi}" } ?: "", Icons.Rounded.ArrowUpward, AppColors.ext.success, Modifier.weight(1f))
            StatTile("Weakest", worst?.let { cellName(it.row, it.col) } ?: "--", worst?.let { "${it.rssi}" } ?: "", Icons.Rounded.ArrowDownward, AppColors.ext.danger, Modifier.weight(1f))
        }
        if (worst != null && worst.rssi < -75) {
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth(), color = AppColors.ext.warning.copy(alpha = 0.08f)) {
                Text(
                    "Spot ${cellName(worst.row, worst.col)} is weak (${worst.rssi} dBm). Try moving the router closer, removing obstacles, or adding a mesh node or extender between ${best?.let { cellName(it.row, it.col) } ?: "the router"} and this area.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeatGrid(points: List<HeatPoint>, measuring: Pair<Int, Int>?, progress: Float, onTap: (Int, Int) -> Unit) {
    val measurer = rememberTextMeasurer()
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val border = MaterialTheme.colorScheme.primary
    val labelColor = AppColors.ext.subtle
    val currentOnTap by rememberUpdatedState(onTap)
    val t = rememberInfiniteTransition(label = "heat")
    val pulse by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(COLS / ROWS.toFloat())
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(points) {
                detectTapGestures { o ->
                    val col = (o.x / (size.width / COLS.toFloat())).toInt().coerceIn(0, COLS - 1)
                    val row = (o.y / (size.height / ROWS.toFloat())).toInt().coerceIn(0, ROWS - 1)
                    currentOnTap(row, col)
                }
            },
    ) {
        val gap = 4.dp.toPx()
        val cw = size.width / COLS
        val ch = size.height / ROWS
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val measured = points.firstOrNull { it.row == r && it.col == c }
            val color = when {
                measured != null -> heatColor(measured.rssi.toFloat())
                points.isNotEmpty() -> {
                    var num = 0.0
                    var den = 0.0
                    points.forEach { p ->
                        val d2 = ((p.row - r) * (p.row - r) + (p.col - c) * (p.col - c)).toDouble()
                        val wgt = 1.0 / d2.pow(1.2)
                        num += wgt * p.rssi
                        den += wgt
                    }
                    heatColor((num / den).toFloat()).copy(alpha = 0.38f)
                }
                else -> empty
            }
            val tl = Offset(c * cw + gap / 2, r * ch + gap / 2)
            val sz = Size(cw - gap, ch - gap)
            drawRoundRect(color, tl, sz, CornerRadius(10.dp.toPx()))
            if (measuring == (r to c)) {
                drawRoundRect(border.copy(alpha = pulse), tl, sz, CornerRadius(10.dp.toPx()), style = Stroke(3.dp.toPx()))
                drawRoundRect(border.copy(alpha = 0.5f), tl, Size(sz.width * progress, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
            }
            val label = measured?.let { "${it.rssi}" } ?: cellName(r, c)
            val style = TextStyle(
                fontSize = if (measured != null) 12.sp else 9.sp,
                fontWeight = if (measured != null) FontWeight.Bold else FontWeight.Normal,
                color = if (measured != null) Color(0xFF060A17) else labelColor,
            )
            val l = measurer.measure(label, style)
            drawText(l, topLeft = Offset(tl.x + (sz.width - l.size.width) / 2, tl.y + (sz.height - l.size.height) / 2))
        }
    }
}

@Composable
private fun Legend() {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFFFB923C), Color(0xFFFBBF24), Color(0xFF22D3EE), Color(0xFF34D399)))),
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weak  -90 dBm", style = MaterialTheme.typography.labelSmall, color = AppColors.ext.muted)
            Text("-48 dBm  Strong", style = MaterialTheme.typography.labelSmall, color = AppColors.ext.muted)
        }
    }
}
