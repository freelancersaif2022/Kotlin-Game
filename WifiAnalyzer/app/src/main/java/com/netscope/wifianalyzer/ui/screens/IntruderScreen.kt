package com.netscope.wifianalyzer.ui.screens

import android.Manifest
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.net.NetworkScanner
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.Pill
import com.netscope.wifianalyzer.ui.components.PremiumGate
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import com.netscope.wifianalyzer.work.IntruderWorker
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

@Composable
fun IntruderScreen(nav: NavHostController) {
    ScreenScaffold(title = "Intruder alert", onBack = { nav.popBackStack() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PremiumGate(
                feature = "Intruder alert",
                description = "Get a notification the moment an unknown phone, laptop or device connects to your Wi-Fi.",
                icon = Icons.Rounded.Security,
            ) { IntruderContent() }
        }
    }
}

@Composable
private fun IntruderContent() {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val enabled by AppGraph.prefs.intruderEnabled.collectAsState()
    val version by AppGraph.knownDevices.version.collectAsState()
    val net by rememberNetState(5_000)
    val key = net.snap.networkKey
    val known = remember(version, key) { key?.let { AppGraph.knownDevices.devices(it) } ?: emptyList() }
    var scanning by remember { mutableStateOf(false) }

    fun enable() {
        AppGraph.prefs.setIntruderEnabled(true)
        IntruderWorker.schedule(context)
        snackbar.show("Intruder alert is on. We'll check your Wi-Fi every 15 minutes.", SnackType.Success)
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) enable() else snackbar.show("Allow notifications so we can alert you about new devices", SnackType.Error, "Settings") {
            openAppSettings(context)
        }
    }

    fun scanNow() {
        if (scanning) return
        if (key == null) {
            snackbar.show("Connect to Wi-Fi first", SnackType.Error)
            return
        }
        scanning = true
        scope.launch {
            val res = runCatching { NetworkScanner(context).scan().last() }.getOrNull()
            scanning = false
            if (res == null || res.error != null) {
                snackbar.show(res?.error ?: "Scan failed. Please try again.", SnackType.Error)
                return@launch
            }
            val fresh = AppGraph.knownDevices.merge(key, res.devices.map { Triple(it.key, it.displayName, it.ip) })
            AppGraph.prefs.lastIntruderCheck = System.currentTimeMillis()
            snackbar.show(
                if (fresh.isEmpty()) "All clear! No new devices found." else "${fresh.size} new device${if (fresh.size > 1) "s" else ""} found. Review the list below.",
                if (fresh.isEmpty()) SnackType.Success else SnackType.Error,
            )
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            val t = rememberInfiniteTransition(label = "shield")
            val pulse by t.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "p")
            AppCard(Modifier.fillMaxWidth(), brush = AppColors.ext.heroGradient, padding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .graphicsLayer {
                                val s = if (enabled) pulse else 1f
                                scaleX = s
                                scaleY = s
                            }
                            .clip(CircleShape)
                            .background(if (enabled) Palette.Mint.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Shield, null, tint = if (enabled) Palette.Mint else Color.White, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (enabled) "Protection is on" else "Protection is off", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        val last = AppGraph.prefs.lastIntruderCheck
                        Text(
                            if (last > 0) "Last check " + DateUtils.getRelativeTimeSpanString(last, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS) else "Checks every 15 min on Wi-Fi",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            if (on) {
                                if (Build.VERSION.SDK_INT >= 33 && !WifiUtils.hasNotificationPermission(context)) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    enable()
                                }
                            } else {
                                AppGraph.prefs.setIntruderEnabled(false)
                                IntruderWorker.cancel(context)
                                snackbar.show("Intruder alert turned off", SnackType.Info)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.Mint),
                    )
                }
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    if (scanning) "Scanning…" else "Check now",
                    onClick = { scanNow() },
                    loading = scanning,
                    icon = Icons.Rounded.Radar,
                    modifier = Modifier.fillMaxWidth(),
                    height = 50.dp,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White, Color(0xFFE0F7FF))),
                )
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)) {
                Text(
                    "Mark the devices you recognize as trusted. Any device that isn't on this list when we scan will trigger an alert. Some phones use a new private address on each network, so a familiar phone may appear as new once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ext.muted,
                )
            }
        }
        item { SectionTitle("Known devices (${known.size})") }
        if (known.isEmpty()) {
            item {
                Text(
                    "No devices yet. Tap \"Check now\" to create the list for this network.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.ext.muted,
                )
            }
        }
        items(known, key = { it.key }) { d ->
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (d.trusted) Icons.Rounded.DevicesOther else Icons.Rounded.Warning,
                        null,
                        tint = if (d.trusted) MaterialTheme.colorScheme.primary else AppColors.ext.warning,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.customName ?: d.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${d.ip} • seen " + DateUtils.getRelativeTimeSpanString(d.lastSeen, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.ext.muted,
                        )
                    }
                    if (!d.trusted) Pill("Unknown", AppColors.ext.warning)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = d.trusted,
                        onCheckedChange = { key?.let { k -> AppGraph.knownDevices.setTrusted(k, d.key, it) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppColors.ext.success),
                    )
                }
            }
        }
    }
}
