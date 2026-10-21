package com.netscope.wifianalyzer.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.ads.AdPlacement
import com.netscope.wifianalyzer.data.SpeedResult
import com.netscope.wifianalyzer.data.SpeedUnit
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.AppDialog
import com.netscope.wifianalyzer.ui.components.EmptyState
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.InfoRow
import com.netscope.wifianalyzer.ui.components.LineChart
import com.netscope.wifianalyzer.ui.components.LocalPremiumActions
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.LockedFeatureCard
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.StatTile
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.components.rememberPremiumState
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.navigation.navigateTab
import com.netscope.wifianalyzer.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(nav: NavHostController) {
    val context = LocalContext.current
    val results by AppGraph.history.results.collectAsState()
    val unit by AppGraph.prefs.unit.collectAsState()
    val premium by rememberPremiumState()
    val actions = LocalPremiumActions.current
    val snackbar = LocalSnackbar.current
    var confirmClear by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<SpeedResult?>(null) }

    val leave = {
        AppGraph.ads.showInterstitial(context.findActivity(), AdPlacement.LEAVE_HISTORY) { nav.popBackStack() }
        Unit
    }
    BackHandler(onBack = leave)

    val visible = if (premium.isPremium) results else results.take(AppConfig.FREE_HISTORY_LIMIT)
    val hidden = results.size - visible.size

    ScreenScaffold(
        title = "Speed history",
        onBack = leave,
        actions = {
            if (results.isNotEmpty()) {
                IconButton(onClick = {
                    if (!premium.isPremium) {
                        actions.openRewardUnlock()
                    } else {
                        runCatching {
                            val file = AppGraph.history.exportCsv()
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("text/csv")
                                .putExtra(Intent.EXTRA_STREAM, uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(Intent.createChooser(send, "Export speed history"))
                        }.onFailure { snackbar.show("Export failed. Please try again.", SnackType.Error) }
                    }
                }) { Icon(Icons.Rounded.FileDownload, "Export CSV") }
                IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, "Clear history") }
            }
        },
    ) { padding ->
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    Icons.Rounded.History,
                    "No speed tests yet",
                    "Your results will appear here so you can track your internet over time.",
                    action = {
                        GradientButton("Run a speed test", onClick = { nav.navigateTab(Routes.speed(true)) })
                    },
                )
            }
            return@ScreenScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val dl = results.map { it.downloadMbps }.average()
                val ul = results.map { it.uploadMbps }.average()
                val ping = results.map { it.pingMs }.average()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Avg down", WifiUtils.formatSpeed(dl, unit).first, unit.label, Icons.Rounded.ArrowDownward, AppColors.ext.download, Modifier.weight(1f))
                    StatTile("Avg up", WifiUtils.formatSpeed(ul, unit).first, unit.label, Icons.Rounded.ArrowUpward, AppColors.ext.upload, Modifier.weight(1f))
                    StatTile("Avg ping", ping.toInt().toString(), "ms", Icons.Rounded.Timer, AppColors.ext.ping, Modifier.weight(1f))
                }
            }
            item {
                SectionTitle("Trend")
                if (premium.isPremium) {
                    AppCard(Modifier.fillMaxWidth()) {
                        val series = results.take(30).reversed()
                        LineChart(
                            values = series.map { it.downloadMbps.toFloat() },
                            color = AppColors.ext.download,
                            secondValues = series.map { it.uploadMbps.toFloat() },
                            secondColor = AppColors.ext.upload,
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Legend("Download", AppColors.ext.download)
                            Spacer(Modifier.width(16.dp))
                            Legend("Upload", AppColors.ext.upload)
                        }
                    }
                } else {
                    LockedFeatureCard(
                        "Charts & CSV export",
                        "See how your speed changes over time and export every result.",
                        Icons.Rounded.Insights,
                    )
                }
            }
            item { SectionTitle("All tests") }
            items(visible, key = { it.id }) { r -> HistoryRow(r, unit, Modifier.animateItem()) { detail = r } }
            if (hidden > 0) {
                item {
                    AppCard(Modifier.fillMaxWidth(), onClick = actions.openRewardUnlock, color = AppColors.ext.gold.copy(alpha = 0.07f)) {
                        Text("+$hidden older results", style = MaterialTheme.typography.titleSmall)
                        Text("Unlock Premium to see your full history", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AppDialog(
            onDismiss = { confirmClear = false },
            icon = Icons.Rounded.DeleteSweep,
            iconTint = AppColors.ext.danger,
            title = "Clear all history?",
            message = "This removes all ${results.size} saved speed tests. This can't be undone.",
            confirmText = "Clear history",
            onConfirm = {
                AppGraph.history.clear()
                confirmClear = false
                snackbar.show("History cleared", SnackType.Success)
            },
            dismissText = "Cancel",
        )
    }

    detail?.let { r ->
        AppDialog(
            onDismiss = { detail = null },
            title = formatDate(r.timestamp),
            message = r.network,
            confirmText = "Done",
            onConfirm = { detail = null },
            dismissText = "Delete this result",
            onDismissClick = {
                AppGraph.history.delete(r.id)
                detail = null
                snackbar.show("Result deleted", SnackType.Success)
            },
        ) {
            val (d, du) = WifiUtils.formatSpeed(r.downloadMbps, unit)
            val (u, uu) = WifiUtils.formatSpeed(r.uploadMbps, unit)
            InfoRow("Download", "$d $du")
            InfoRow("Upload", "$u $uu")
            InfoRow("Ping", "${r.pingMs.toInt()} ms")
            InfoRow("Jitter", "${r.jitterMs.toInt()} ms")
            r.rssi?.let { InfoRow("Signal", "$it dBm") }
            InfoRow("Connection", r.connection)
            if (r.isp.isNotBlank()) InfoRow("Provider", r.isp)
            if (r.server.isNotBlank()) InfoRow("Server", r.server)
        }
    }
}

private fun formatDate(ts: Long): String = SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).padding(1.dp)) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCircle(color) }
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.ext.muted)
    }
}

@Composable
private fun HistoryRow(r: SpeedResult, unit: SpeedUnit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatDate(r.timestamp), style = MaterialTheme.typography.labelMedium, color = AppColors.ext.muted)
                Text(r.network, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Value(Icons.Rounded.ArrowDownward, WifiUtils.formatSpeed(r.downloadMbps, unit).first, unit.label, AppColors.ext.download)
            Value(Icons.Rounded.ArrowUpward, WifiUtils.formatSpeed(r.uploadMbps, unit).first, unit.label, AppColors.ext.upload)
            Value(Icons.Rounded.Timer, r.pingMs.toInt().toString(), "ms", AppColors.ext.ping)
        }
    }
}

@Composable
private fun Value(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, unit: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(3.dp))
        Text(unit, style = MaterialTheme.typography.labelSmall, color = AppColors.ext.muted)
    }
}
