package com.netscope.wifianalyzer.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.data.AppUsage
import com.netscope.wifianalyzer.data.DataUsageRepository
import com.netscope.wifianalyzer.data.UsagePeriod
import com.netscope.wifianalyzer.data.UsageReport
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.BarChart
import com.netscope.wifianalyzer.ui.components.EmptyState
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.PremiumGate
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SegmentedTabs
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DataUsageScreen(nav: NavHostController) {
    ScreenScaffold(title = "Data usage", onBack = { nav.popBackStack() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PremiumGate(
                feature = "Data usage monitor",
                description = "See how much Wi-Fi and mobile data you use each day and month, and which apps use the most.",
                icon = Icons.Rounded.DataUsage,
                preview = {
                    Column(Modifier.padding(20.dp)) {
                        AppCard(Modifier.fillMaxWidth()) {
                            Text("24.6 GB", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(12.dp))
                            BarChart(listOf(3f, 5f, 2f, 6f, 4f, 7f, 5f), Palette.Cyan, Modifier.fillMaxWidth().height(120.dp))
                        }
                    }
                },
            ) { DataUsageContent() }
        }
    }
}

@Composable
private fun DataUsageContent() {
    val context = LocalContext.current
    val repo = remember { DataUsageRepository(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var hasAccess by remember { mutableStateOf(repo.hasAccess()) }
    var tab by remember { mutableIntStateOf(0) }
    var period by remember { mutableStateOf(UsagePeriod.MONTH) }
    var report by remember { mutableStateOf<UsageReport?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { hasAccess = repo.hasAccess() }
    }
    LaunchedEffect(hasAccess, tab, period) {
        if (!hasAccess) return@LaunchedEffect
        loading = true
        report = withContext(Dispatchers.IO) { repo.load(wifi = tab == 0, period = period) }
        loading = false
    }

    if (!hasAccess) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            EmptyState(
                Icons.Rounded.Insights,
                "Allow usage access",
                "Android needs you to turn on \"Usage access\" for WiFi Analyzer so it can read data usage per app. This data stays on your phone.",
                action = {
                    GradientButton(
                        "Open settings",
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                    )
                },
            )
        }
        return
    }

    val r = report
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SegmentedTabs(listOf("Wi-Fi", "Mobile data"), tab, onSelect = { tab = it }) }
        item {
            SegmentedTabs(UsagePeriod.entries.map { it.label }, period.ordinal, onSelect = { period = UsagePeriod.entries[it] })
        }
        item {
            AppCard(Modifier.fillMaxWidth(), brush = AppColors.ext.heroGradient, padding = PaddingValues(20.dp)) {
                Text("${period.label} on ${if (tab == 0) "Wi-Fi" else "mobile data"}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                if (loading && r == null) {
                    CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                } else {
                    Text(WifiUtils.formatBytes(r?.total ?: 0), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        UsageStat(Icons.Rounded.ArrowDownward, "Received", WifiUtils.formatBytes(r?.rxBytes ?: 0))
                        UsageStat(Icons.Rounded.ArrowUpward, "Sent", WifiUtils.formatBytes(r?.txBytes ?: 0))
                    }
                }
            }
        }
        if (r != null) {
            item {
                SectionTitle("Last 7 days")
                AppCard(Modifier.fillMaxWidth()) {
                    BarChart(r.daily.map { it.bytes.toFloat() }, if (tab == 0) Palette.Cyan else Palette.Lilac, Modifier.fillMaxWidth().height(120.dp))
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        r.daily.forEach { Text(it.dayLabel.take(2), style = MaterialTheme.typography.labelSmall, color = AppColors.ext.muted) }
                    }
                }
            }
            item { SectionTitle("Top apps") }
            if (r.apps.isEmpty()) {
                item { Text("No app usage recorded for this period.", style = MaterialTheme.typography.bodyMedium, color = AppColors.ext.muted) }
            }
            val max = r.apps.maxOfOrNull { it.total }?.coerceAtLeast(1L) ?: 1L
            items(r.apps, key = { it.uid }) { app -> AppUsageRow(app, max) }
        }
    }
}

@Composable
private fun UsageStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage, max: Long) {
    AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bmp = app.icon
            if (bmp != null) {
                Image(bmp.asImageBitmap(), null, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
            } else {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Android, null, tint = AppColors.ext.muted) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(app.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(WifiUtils.formatBytes(app.total), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { app.total.toFloat() / max },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
    }
}
