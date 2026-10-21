package com.novacleaner.app.ui.datausage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.core.Settings
import com.novacleaner.app.data.AppDataUsage
import com.novacleaner.app.data.DailyUsage
import com.novacleaner.app.data.UsagePeriod
import com.novacleaner.app.ui.components.AppIcon
import com.novacleaner.app.ui.components.BytesHeadline
import com.novacleaner.app.ui.components.FilterPill
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.PermissionGate
import com.novacleaner.app.ui.components.PillRow
import com.novacleaner.app.ui.components.ProgressBar
import com.novacleaner.app.ui.components.ShimmerBox
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Nova

@Composable
fun DataUsageScreen(vm: DataUsageViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.limitMb.collectAsStateWithLifecycle(initialValue = Settings())
    val tool = Tool.DATA_USAGE
    var editLimit by remember { mutableStateOf(false) }

    ToolScaffold(title = tool.title, subtitle = "Mobile & Wi-Fi • ${state.period.label}", onBack = navigator::exitTool) {
        PermissionGate(
            AppPermission.USAGE, "Allow usage access",
            "Android only shares per-app data usage with apps that have usage access. Find Nova Cleaner in the list and switch it on.",
            tool.icon, tool.colors,
        ) {
            LaunchedEffect(Unit) { vm.start() }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { PillRow(UsagePeriod.entries, state.period, { it.label }, vm::setPeriod, Modifier.padding(top = 6.dp)) }
                item { SummaryCard(state) }
                item { LimitCard(state.monthMobile, settings.dataLimitMb) { editLimit = true } }
                item { WeekChart(state.daily) }
                item {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Apps", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        UsageSort.entries.forEach { s ->
                            Spacer(Modifier.width(6.dp))
                            FilterPill(s.label, s == state.sort, { vm.setSort(s) })
                        }
                    }
                }
                if (state.loading) {
                    items(5) { ShimmerBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(64.dp)) }
                } else {
                    val apps = state.sortedApps
                    val max = apps.maxOfOrNull {
                        when (state.sort) { UsageSort.TOTAL -> it.total; UsageSort.MOBILE -> it.mobile; UsageSort.WIFI -> it.wifi }
                    }?.coerceAtLeast(1L) ?: 1L
                    if (apps.isEmpty()) {
                        item {
                            Text(
                                "No data used in this period.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                            )
                        }
                    }
                    items(apps, key = { it.uid }) { app -> UsageRow(app, state.sort, max) }
                }
            }
        }
    }

    if (editLimit) {
        LimitDialog(current = settings.dataLimitMb, onPick = { vm.setLimit(it); editLimit = false }, onDismiss = { editLimit = false })
    }
}

@Composable
private fun SummaryCard(state: DataUsageState) {
    val total = state.totals.total
    val mobileFraction by animateFloatAsState(if (total > 0) state.totals.mobile.toFloat() / total else 0f, tween(700), label = "split")
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(Tool.DATA_USAGE.colors))
            .padding(22.dp),
    ) {
        Column {
            Text("Total data used", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            BytesHeadline(total, color = Color.White, numberSize = 40)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.25f))) {
                if (mobileFraction > 0f) Box(Modifier.weight(mobileFraction.coerceAtLeast(0.001f)).fillMaxSize().background(Color(0xFF3A1600).copy(alpha = 0.55f)))
                if (mobileFraction < 1f) Box(Modifier.weight((1f - mobileFraction).coerceAtLeast(0.001f)).fillMaxSize().background(Color.White))
            }
            Spacer(Modifier.height(14.dp))
            Row {
                SplitStat(Icons.Rounded.SignalCellularAlt, "Mobile", Format.bytes(state.totals.mobile), Modifier.weight(1f))
                SplitStat(Icons.Rounded.Wifi, "Wi-Fi", Format.bytes(state.totals.wifi), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SplitStat(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
private fun LimitCard(monthMobile: Long, limitMb: Long, onEdit: () -> Unit) {
    NovaCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(Icons.Rounded.SignalCellularAlt, Tool.DATA_USAGE.colors, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Monthly mobile limit", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (limitMb > 0) "${Format.bytes(monthMobile)} of ${Format.bytes(limitMb shl 20)} this month" else "${Format.bytes(monthMobile)} used this month • tap to set a limit",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        if (limitMb > 0) {
            val fraction = monthMobile.toFloat() / (limitMb shl 20).toFloat()
            Spacer(Modifier.height(14.dp))
            ProgressBar(
                fraction, Modifier.fillMaxWidth(),
                colors = if (fraction > 0.9f) listOf(Nova.colors.danger, Nova.colors.danger) else if (fraction > 0.75f) listOf(Nova.colors.warning, Nova.colors.warning) else Tool.JUNK.colors,
                track = MaterialTheme.colorScheme.surfaceVariant,
                height = 10.dp,
            )
            if (fraction > 0.9f) {
                Spacer(Modifier.height(8.dp))
                Text("You're close to your monthly limit", style = MaterialTheme.typography.labelMedium, color = Nova.colors.danger)
            }
        }
    }
}

@Composable
private fun WeekChart(daily: List<DailyUsage>) {
    val wifiColor = Nova.colors.chartWifi
    val mobileColor = Nova.colors.chartMobile
    val track = MaterialTheme.colorScheme.surfaceVariant
    val grow by animateFloatAsState(if (daily.isEmpty()) 0f else 1f, tween(900), label = "grow")
    NovaCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last 7 days", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Legend(mobileColor, "Mobile")
            Spacer(Modifier.width(12.dp))
            Legend(wifiColor, "Wi-Fi")
        }
        Spacer(Modifier.height(16.dp))
        val max = daily.maxOfOrNull { it.wifi + it.mobile }?.coerceAtLeast(1L) ?: 1L
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            if (daily.isEmpty()) return@Canvas
            val slot = size.width / daily.size
            val barW = slot * 0.46f
            val r = CornerRadius(barW / 2.2f)
            daily.forEachIndexed { i, d ->
                val x = slot * i + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), r)
                val total = (d.wifi + d.mobile).toFloat() / max * size.height * grow
                val mobileH = d.mobile.toFloat() / max * size.height * grow
                if (total > 0f) {
                    drawRoundRect(wifiColor, Offset(x, size.height - total), Size(barW, total), r)
                    if (mobileH > 0f) drawRoundRect(mobileColor, Offset(x, size.height - total), Size(barW, mobileH.coerceAtLeast(barW * 0.3f).coerceAtMost(total)), r)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            daily.forEach { d ->
                Text(
                    Format.dayShort(d.dayStart), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UsageRow(app: AppDataUsage, sort: UsageSort, max: Long) {
    val value = when (sort) { UsageSort.TOTAL -> app.total; UsageSort.MOBILE -> app.mobile; UsageSort.WIFI -> app.wifi }
    NovaCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(Format.bytes(value), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { value.toFloat() / max },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    color = if (sort == UsageSort.WIFI) Nova.colors.chartWifi else Nova.colors.chartMobile,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mobile ${Format.bytes(app.mobile)} • Wi-Fi ${Format.bytes(app.wifi)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LimitDialog(current: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(0L to "Off", 1024L to "1 GB", 2048L to "2 GB", 3072L to "3 GB", 5120L to "5 GB", 10240L to "10 GB", 20480L to "20 GB", 51200L to "50 GB")
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Monthly mobile data limit") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (mb, label) ->
                    Box(Modifier.bounceClick { onPick(mb) }) { FilterPill(label, mb == current, { onPick(mb) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
