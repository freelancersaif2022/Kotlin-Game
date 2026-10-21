package com.novacleaner.app.ui.traffic

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.core.Permissions
import com.novacleaner.app.core.Settings
import com.novacleaner.app.service.NetSpeedService
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.components.rememberPermissionRequester
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Nova
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

enum class Connection(val label: String) { WIFI("Wi-Fi"), MOBILE("Mobile data"), ETHERNET("Ethernet"), OFFLINE("Offline") }

data class TrafficState(
    val down: Long = 0,
    val up: Long = 0,
    val peakDown: Long = 0,
    val peakUp: Long = 0,
    val history: List<Pair<Long, Long>> = emptyList(),
    val sessionDown: Long = 0,
    val sessionUp: Long = 0,
    val bootDown: Long = 0,
    val bootUp: Long = 0,
    val connection: Connection = Connection.OFFLINE,
)

class TrafficViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(TrafficState())
    val state: StateFlow<TrafficState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            var lastRx = TrafficStats.getTotalRxBytes()
            var lastTx = TrafficStats.getTotalTxBytes()
            val startRx = lastRx
            val startTx = lastTx
            while (isActive) {
                delay(1000)
                val rx = TrafficStats.getTotalRxBytes()
                val tx = TrafficStats.getTotalTxBytes()
                val down = (rx - lastRx).coerceAtLeast(0)
                val up = (tx - lastTx).coerceAtLeast(0)
                lastRx = rx
                lastTx = tx
                val s = _state.value
                _state.value = s.copy(
                    down = down,
                    up = up,
                    peakDown = maxOf(s.peakDown, down),
                    peakUp = maxOf(s.peakUp, up),
                    history = (s.history + (down to up)).takeLast(60),
                    sessionDown = (rx - startRx).coerceAtLeast(0),
                    sessionUp = (tx - startTx).coerceAtLeast(0),
                    bootDown = rx.coerceAtLeast(0),
                    bootUp = tx.coerceAtLeast(0),
                    connection = connection(getApplication()),
                )
            }
        }
    }

    private fun connection(context: Context): Connection {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return Connection.OFFLINE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Connection.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Connection.MOBILE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Connection.ETHERNET
            else -> Connection.OFFLINE
        }
    }
}

/** Log scale so 10 KB/s and 50 MB/s are both readable on one gauge. */
private fun gaugeFraction(bytesPerSec: Long): Float {
    val kb = bytesPerSec / 1024.0
    return (ln(1 + kb) / ln(1 + 102_400.0)).toFloat().coerceIn(0f, 1f)
}

@Composable
fun TrafficScreen(vm: TrafficViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by AppGraph.prefs.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val scope = rememberCoroutineScope()
    val tool = Tool.SPEED

    fun setMonitor(enabled: Boolean) {
        scope.launch { AppGraph.prefs.setSpeedMonitorEnabled(enabled) }
        if (enabled) NetSpeedService.start(context) else NetSpeedService.stop(context)
    }
    val requestNotifications = rememberPermissionRequester(AppPermission.NOTIFICATIONS) { granted -> if (granted) setMonitor(true) }

    ToolScaffold(title = tool.title, subtitle = state.connection.label, onBack = navigator::exitTool) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            NovaCard(Modifier.fillMaxWidth()) {
                SpeedGauge(state.down, Modifier.fillMaxWidth().height(230.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpeedTile(Icons.Rounded.ArrowDownward, "Download", state.down, state.peakDown, Tool.JUNK.colors, Modifier.weight(1f))
                    SpeedTile(Icons.Rounded.ArrowUpward, "Upload", state.up, state.peakUp, tool.colors, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            NovaCard(Modifier.fillMaxWidth()) {
                Text("Live traffic • last 60 s", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                LiveChart(state.history, Modifier.fillMaxWidth().height(130.dp))
            }
            Spacer(Modifier.height(12.dp))
            NovaCard(Modifier.fillMaxWidth()) {
                InfoRow(
                    when (state.connection) {
                        Connection.WIFI -> Icons.Rounded.Wifi
                        Connection.MOBILE -> Icons.Rounded.SignalCellularAlt
                        Connection.ETHERNET -> Icons.Rounded.Wifi
                        Connection.OFFLINE -> Icons.Rounded.CloudOff
                    },
                    "Connection", state.connection.label,
                )
                InfoRow(Icons.Rounded.ArrowDownward, "This session", "↓ ${Format.bytes(state.sessionDown)}   ↑ ${Format.bytes(state.sessionUp)}")
                InfoRow(Icons.Rounded.ArrowUpward, "Since device boot", "↓ ${Format.bytes(state.bootDown)}   ↑ ${Format.bytes(state.bootUp)}")
            }
            Spacer(Modifier.height(12.dp))
            NovaCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlossyIcon(Icons.Rounded.NotificationsActive, tool.colors, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Speed meter in status bar", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Shows your live speed as a status bar icon.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.speedMonitorEnabled,
                        onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Permissions.hasNotifications(context)) {
                                requestNotifications()
                            } else setMonitor(on)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpeedGauge(bytesPerSec: Long, modifier: Modifier) {
    val fraction by animateFloatAsState(gaugeFraction(bytesPerSec), spring(dampingRatio = 0.7f, stiffness = 60f), label = "needle")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val needleColor = MaterialTheme.colorScheme.onSurface
    val colors = listOf(Color(0xFF34E3A8), Color(0xFFB6F25C), Color(0xFFFFC14D), Color(0xFFFF8A3D), Color(0xFFE548C5))
    val (value, unit) = Format.bytesParts(bytesPerSec)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val diameter = minOf(size.width * 0.8f, size.height * 1.25f)
            val topLeft = Offset((size.width - diameter) / 2, stroke)
            val arcSize = Size(diameter, diameter)
            val center = Offset(topLeft.x + diameter / 2, topLeft.y + diameter / 2)
            val start = 150f
            val sweep = 240f
            drawArc(track, start, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(0f to colors[4], 0.42f to colors[0], 0.58f to colors[1], 0.75f to colors[2], 0.9f to colors[3], 1f to colors[4], center = center),
                start, sweep * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val r = diameter / 2
            for (i in 0..10) {
                val a = ((start + sweep * i / 10f) * PI / 180).toFloat()
                val inner = r - stroke * 1.4f
                val outer = r - stroke * (if (i % 5 == 0) 2.3f else 1.9f)
                drawLine(tickColor.copy(alpha = 0.5f), Offset(center.x + cos(a) * inner, center.y + sin(a) * inner), Offset(center.x + cos(a) * outer, center.y + sin(a) * outer), 2.dp.toPx(), StrokeCap.Round)
            }
            val na = ((start + sweep * fraction) * PI / 180).toFloat()
            val needleLen = r - stroke * 2.6f
            drawLine(needleColor, center, Offset(center.x + cos(na) * needleLen, center.y + sin(na) * needleLen), 4.dp.toPx(), StrokeCap.Round)
            drawCircle(needleColor, 9.dp.toPx(), center)
            drawCircle(track, 4.dp.toPx(), center)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(4.dp))
                Text("$unit/s", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text("Download speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpeedTile(icon: ImageVector, label: String, speed: Long, peak: Long, colors: List<Color>, modifier: Modifier) {
    Row(modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        GlossyIcon(icon, colors, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Format.speed(speed), style = MaterialTheme.typography.titleSmall)
            Text("Peak ${Format.speed(peak)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LiveChart(history: List<Pair<Long, Long>>, modifier: Modifier) {
    val down = Nova.colors.chartWifi
    val up = Tool.SPEED.colors.last()
    val grid = MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (history.size < 2) return@Canvas
        val max = history.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(10 * 1024L).toFloat()
        val step = size.width / 59f
        val offset = (60 - history.size) * step
        fun path(select: (Pair<Long, Long>) -> Long, close: Boolean): Path = Path().apply {
            history.forEachIndexed { i, p ->
                val x = offset + i * step
                val y = size.height - select(p) / max * size.height * 0.92f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            if (close) {
                lineTo(offset + (history.size - 1) * step, size.height)
                lineTo(offset, size.height)
                close()
            }
        }
        drawPath(path({ it.first }, true), Brush.verticalGradient(listOf(down.copy(alpha = 0.35f), down.copy(alpha = 0f))))
        drawPath(path({ it.first }, false), down, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path({ it.second }, false), up, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}
