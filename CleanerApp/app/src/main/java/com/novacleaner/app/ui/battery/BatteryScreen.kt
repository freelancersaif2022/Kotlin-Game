package com.novacleaner.app.ui.battery

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Amber
import com.novacleaner.app.ui.theme.Coral
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

data class BatteryState(
    val level: Int = 0,
    val charging: Boolean = false,
    val status: String = "—",
    val health: String = "—",
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val technology: String = "—",
    val plugged: String = "—",
    val currentMa: Int? = null,
    val capacityMah: Int? = null,
    val timeToFullMin: Long? = null,
)

class BatteryViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _state.value = read(getApplication())
                delay(2000)
            }
        }
    }

    private fun read(context: Context): BatteryState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return BatteryState()
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val statusCode = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = statusCode == BatteryManager.BATTERY_STATUS_CHARGING || statusCode == BatteryManager.BATTERY_STATUS_FULL
        val status = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }
        val plugged = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }
        val currentRaw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (currentRaw == Int.MIN_VALUE || currentRaw == 0) null else {
            // Devices report µA or mA; normalise to mA.
            if (abs(currentRaw) > 20_000) currentRaw / 1000 else currentRaw
        }
        val counter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val capacity = if (counter > 0 && level > 0) (counter / 1000f / level * 100).toInt().takeIf { it in 500..20_000 } else null
        val ttf = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && charging) {
            bm.computeChargeTimeRemaining().takeIf { it > 0 }?.let { it / 60_000 }
        } else null
        return BatteryState(
            level = level,
            charging = charging,
            status = status,
            health = health,
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—",
            plugged = plugged,
            currentMa = currentMa,
            capacityMah = capacity,
            timeToFullMin = ttf,
        )
    }
}

@Composable
fun BatteryScreen(vm: BatteryViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by vm.state.collectAsStateWithLifecycle()
    val tool = Tool.BATTERY
    ToolScaffold(title = tool.title, subtitle = state.status, onBack = navigator::exitTool) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            NovaCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BatteryIllustration(state.level, state.charging, Modifier.width(110.dp).height(190.dp))
                    Spacer(Modifier.width(24.dp))
                    Column {
                        Text("${state.level}%", style = MaterialTheme.typography.displayMedium)
                        Text(state.status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        val ttf = state.timeToFullMin
                        Text(
                            when {
                                ttf != null -> "Full in about ${ttf / 60}h ${ttf % 60}m"
                                state.charging -> "Charging via ${state.plugged}"
                                else -> "Running on battery"
                            },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val tiles = listOf(
                Triple(Icons.Rounded.Favorite, "Health", state.health),
                Triple(Icons.Rounded.Thermostat, "Temperature", String.format(Locale.US, "%.1f °C", state.temperature)),
                Triple(Icons.Rounded.ElectricalServices, "Voltage", String.format(Locale.US, "%.2f V", state.voltage)),
                Triple(Icons.Rounded.Science, "Technology", state.technology),
                Triple(Icons.Rounded.Power, "Power source", state.plugged),
                Triple(Icons.Rounded.Speed, "Current", state.currentMa?.let { "$it mA" } ?: "—"),
                Triple(Icons.Rounded.Memory, "Est. capacity", state.capacityMah?.let { "$it mAh" } ?: "—"),
                Triple(Icons.Rounded.Timer, "Time to full", state.timeToFullMin?.let { "${it / 60}h ${it % 60}m" } ?: "—"),
            )
            val palettes = listOf(Tool.SCREENSHOTS.colors, Tool.DATA_USAGE.colors, Tool.LARGE_FILES.colors, Tool.DUPLICATES.colors, Tool.BATTERY.colors, Tool.SPEED.colors, Tool.APPS.colors, Tool.JUNK.colors)
            tiles.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { col, (icon, label, value) ->
                        InfoTile(icon, label, value, palettes[row * 2 + col], Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun InfoTile(icon: ImageVector, label: String, value: String, colors: List<Color>, modifier: Modifier) {
    NovaCard(modifier) {
        GlossyIcon(icon, colors, size = 38.dp)
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun BatteryIllustration(level: Int, charging: Boolean, modifier: Modifier) {
    val fill by animateFloatAsState(level / 100f, tween(1000), label = "fill")
    val pulse by rememberInfiniteTransition(label = "bolt").animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    val colors = when {
        level <= 15 -> listOf(Coral, Color(0xFFFF9A8A))
        level <= 35 -> listOf(Amber, Color(0xFFFFD58A))
        else -> listOf(Color(0xFF00A57C), Color(0xFF7DF2CC))
    }
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val inner = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val capW = size.width * 0.36f
            val capH = size.height * 0.06f
            drawRoundRect(outline, Offset((size.width - capW) / 2, 0f), Size(capW, capH + 6f), CornerRadius(8f))
            val bodyTop = capH
            val bodyH = size.height - bodyTop
            val stroke = 5.dp.toPx()
            drawRoundRect(inner, Offset(0f, bodyTop), Size(size.width, bodyH), CornerRadius(28.dp.toPx()))
            drawRoundRect(outline, Offset(0f, bodyTop), Size(size.width, bodyH), CornerRadius(28.dp.toPx()), style = Stroke(stroke))
            val pad = stroke * 1.8f
            val maxH = bodyH - pad * 2
            val h = maxH * fill
            drawRoundRect(
                Brush.verticalGradient(colors.reversed(), startY = bodyTop + pad + maxH - h, endY = bodyTop + pad + maxH),
                Offset(pad, bodyTop + pad + maxH - h), Size(size.width - pad * 2, h), CornerRadius(20.dp.toPx()),
            )
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), startX = pad, endX = size.width * 0.5f),
                Offset(pad, bodyTop + pad + maxH - h), Size((size.width - pad * 2) * 0.45f, h), CornerRadius(20.dp.toPx()),
            )
            if (charging) {
                val cx = size.width / 2
                val cy = bodyTop + bodyH / 2
                val w = size.width * 0.42f
                val bh = w * 1.5f
                scale(pulse, Offset(cx, cy)) {
                    val left = cx - w / 2
                    val top = cy - bh / 2
                    val bolt = Path().apply {
                        moveTo(left + w * 0.62f, top)
                        lineTo(left + w * 0.08f, top + bh * 0.56f)
                        lineTo(left + w * 0.46f, top + bh * 0.56f)
                        lineTo(left + w * 0.34f, top + bh)
                        lineTo(left + w * 0.92f, top + bh * 0.4f)
                        lineTo(left + w * 0.54f, top + bh * 0.4f)
                        close()
                    }
                    drawPath(bolt, Color.White)
                }
            }
        }
    }
}
