package com.netscope.wifianalyzer.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tablet
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.ads.AdPlacement
import com.netscope.wifianalyzer.net.DeviceType
import com.netscope.wifianalyzer.net.NetDevice
import com.netscope.wifianalyzer.net.NetworkScanner
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.AppDialog
import com.netscope.wifianalyzer.ui.components.EmptyState
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.IconBadge
import com.netscope.wifianalyzer.ui.components.InfoRow
import com.netscope.wifianalyzer.ui.components.LocalPremiumActions
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.Pill
import com.netscope.wifianalyzer.ui.components.ProBadge
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.components.rememberPremiumState
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class DevicesUi(
    val scanning: Boolean = false,
    val progress: Float = 0f,
    val devices: List<NetDevice> = emptyList(),
    val newKeys: Set<String> = emptySet(),
    val error: String? = null,
    val scannedOnce: Boolean = false,
    val awaitingReveal: Boolean = false,
    val networkKey: String? = null,
)

class DevicesViewModel(app: Application) : AndroidViewModel(app) {
    private val _ui = MutableStateFlow(DevicesUi())
    val ui: StateFlow<DevicesUi> = _ui.asStateFlow()
    private var job: Job? = null

    fun scan() {
        if (job?.isActive == true) return
        val key = WifiUtils.snapshot(getApplication()).networkKey
        _ui.value = _ui.value.copy(scanning = true, progress = 0f, error = null, networkKey = key, devices = emptyList())
        job = viewModelScope.launch {
            NetworkScanner(getApplication()).scan().collect { p ->
                if (p.error != null) {
                    _ui.value = _ui.value.copy(scanning = false, error = p.error, scannedOnce = true)
                    return@collect
                }
                _ui.value = _ui.value.copy(progress = p.progress, devices = p.devices)
                if (p.done) {
                    val fresh = if (key != null) {
                        AppGraph.knownDevices.merge(key, p.devices.map { Triple(it.key, it.displayName, it.ip) })
                    } else {
                        emptySet()
                    }
                    AppGraph.prefs.completedScans = AppGraph.prefs.completedScans + 1
                    _ui.value = _ui.value.copy(
                        scanning = false, progress = 1f, newKeys = fresh, scannedOnce = true, awaitingReveal = true,
                    )
                }
            }
        }
    }

    fun revealed() {
        _ui.value = _ui.value.copy(awaitingReveal = false)
    }
}

fun deviceIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.ROUTER -> Icons.Rounded.Router
    DeviceType.PHONE -> Icons.Rounded.PhoneAndroid
    DeviceType.TABLET -> Icons.Rounded.Tablet
    DeviceType.COMPUTER -> Icons.Rounded.Computer
    DeviceType.TV -> Icons.Rounded.Tv
    DeviceType.SPEAKER -> Icons.Rounded.Speaker
    DeviceType.PRINTER -> Icons.Rounded.Print
    DeviceType.CAMERA -> Icons.Rounded.Videocam
    DeviceType.CONSOLE -> Icons.Rounded.SportsEsports
    DeviceType.SMART_HOME -> Icons.Rounded.Lightbulb
    DeviceType.UNKNOWN -> Icons.Rounded.DevicesOther
}

fun deviceColor(type: DeviceType): Color = when (type) {
    DeviceType.ROUTER -> Palette.Violet
    DeviceType.PHONE, DeviceType.TABLET -> Palette.Cyan
    DeviceType.COMPUTER -> Palette.Blue
    DeviceType.TV -> Palette.Pink
    DeviceType.SPEAKER -> Palette.Mint
    DeviceType.PRINTER -> Palette.Orange
    DeviceType.CAMERA -> Palette.Rose
    DeviceType.CONSOLE -> Palette.Lilac
    DeviceType.SMART_HOME -> Palette.Gold
    DeviceType.UNKNOWN -> Color(0xFF8A94AD)
}

@Composable
fun DevicesScreen(nav: NavHostController) {
    val vm: DevicesViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val net by rememberNetState(5_000)
    val premium by rememberPremiumState()
    val actions = LocalPremiumActions.current
    val context = LocalContext.current
    val knownVersion by AppGraph.knownDevices.version.collectAsState()
    var selected by remember { mutableStateOf<NetDevice?>(null) }

    LaunchedEffect(Unit) {
        if (!ui.scannedOnce && !ui.scanning && net.snap.wifiConnected) vm.scan()
    }
    LaunchedEffect(ui.awaitingReveal) {
        if (ui.awaitingReveal) {
            AppGraph.ads.showInterstitial(context.findActivity(), AdPlacement.DEVICE_SCAN_DONE) { vm.revealed() }
        }
    }

    val known = remember(knownVersion, ui.networkKey) {
        ui.networkKey?.let { k -> AppGraph.knownDevices.devices(k).associateBy { it.key } } ?: emptyMap()
    }
    val visible = if (premium.isPremium) ui.devices else ui.devices.take(AppConfig.FREE_DEVICE_LIMIT)
    val hiddenCount = ui.devices.size - visible.size
    val showList = !ui.awaitingReveal

    ScreenScaffold(title = "Who's on my Wi-Fi", onBack = null, topLevel = true) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScanHeader(ui, net, onScan = { vm.scan() })
            }
            if (!net.snap.wifiConnected && !ui.scanning) {
                item {
                    EmptyState(
                        Icons.Rounded.WifiOff,
                        "Not connected to Wi-Fi",
                        "Connect to a Wi-Fi network to see which devices are using it.",
                        action = { SoftButton("Open Wi-Fi settings", onClick = { openWifiSettings(context) }) },
                    )
                }
            }
            ui.error?.let { err ->
                item {
                    AppCard(Modifier.fillMaxWidth(), color = AppColors.ext.danger.copy(alpha = 0.08f)) {
                        Text(err, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (showList && ui.devices.isNotEmpty()) {
                if (!AppGraph.prefs.intruderEnabled.value) {
                    item {
                        AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.INTRUDER) }, color = Palette.Rose.copy(alpha = 0.07f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(Icons.Rounded.NotificationsActive, Palette.Rose, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Intruder alert", style = MaterialTheme.typography.titleSmall)
                                        Spacer(Modifier.width(6.dp))
                                        ProBadge()
                                    }
                                    Text("Get notified when an unknown device joins", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                                }
                            }
                        }
                    }
                }
                items(visible, key = { it.ip }) { d ->
                    val k = known[d.key]
                    DeviceRow(
                        device = d,
                        name = k?.customName ?: d.displayName,
                        isNew = d.key in ui.newKeys,
                        trusted = k?.trusted == true,
                        modifier = Modifier.animateItem(),
                    ) { selected = d }
                }
                if (hiddenCount > 0) {
                    item {
                        LockedDevicesCard(hiddenCount, onUnlock = actions.openRewardUnlock, onPaywall = actions.openPaywall)
                    }
                }
                item {
                    Text(
                        "Some devices hide from scans or use private (random) addresses. Android does not let apps read MAC addresses on newer versions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.ext.subtle,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }

    selected?.let { d ->
        DeviceSheet(d, ui.networkKey, onDismiss = { selected = null })
    }
}

@Composable
private fun ScanHeader(ui: DevicesUi, net: NetState, onScan: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().animateContentSize(), brush = AppColors.ext.heroGradient, padding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    net.snap.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(net.snap.gateway?.let { "Router $it" }, net.snap.ip?.let { "You $it" }).joinToString("  •  "),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    val count by animateFloatAsState(ui.devices.size.toFloat(), tween(400), label = "count")
                    Text("${count.toInt()}", color = Color.White, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (ui.scanning) "found so far" else if (ui.devices.size == 1) "device" else "devices",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            Radar(scanning = ui.scanning, progress = ui.progress, modifier = Modifier.size(108.dp))
        }
        Spacer(Modifier.height(16.dp))
        if (ui.scanning) {
            Text(
                "Scanning your network… ${(ui.progress * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
            )
        } else {
            GradientButton(
                if (ui.scannedOnce) "Scan again" else "Scan network",
                onClick = onScan,
                icon = if (ui.scannedOnce) Icons.Rounded.Refresh else Icons.Rounded.Radar,
                enabled = net.snap.wifiConnected,
                modifier = Modifier.fillMaxWidth(),
                height = 50.dp,
                brush = Brush.horizontalGradient(listOf(Color.White, Color(0xFFE0F7FF))),
            )
        }
    }
}

@Composable
private fun Radar(scanning: Boolean, progress: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "radar")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "sweep")
    Canvas(modifier) {
        val r = size.minDimension / 2
        for (i in 1..3) drawCircle(Color.White.copy(alpha = 0.14f), r * i / 3f, style = Stroke(1.dp.toPx()))
        if (scanning) {
            val a = sweep * PI / 180
            val end = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Palette.Cyan), center, end), center, end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Palette.Cyan.copy(alpha = 0.18f), sweep - 50f, 50f, true)
        }
        drawArc(
            Palette.Cyan,
            -90f,
            360f * progress,
            false,
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(Palette.Cyan, 5.dp.toPx())
    }
}

@Composable
private fun DeviceRow(device: NetDevice, name: String, isNew: Boolean, trusted: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 3 }) {
        AppCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(deviceIcon(device.type), deviceColor(device.type), size = 46.dp, iconSize = 24.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(device.ip, device.vendor?.takeIf { !device.isSelf }, device.type.label.takeIf { device.type != DeviceType.UNKNOWN })
                            .distinct().joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when {
                    device.isSelf -> Pill("You", MaterialTheme.colorScheme.primary)
                    device.isGateway -> Pill("Router", Palette.Violet)
                    isNew -> Pill("New", Palette.Rose)
                    trusted -> Icon(Icons.Rounded.VerifiedUser, "Trusted", tint = AppColors.ext.success, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun LockedDevicesCard(count: Int, onUnlock: () -> Unit, onPaywall: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), color = AppColors.ext.gold.copy(alpha = 0.07f)) {
        repeat(minOf(count, 3)) {
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.width(14.dp))
                Column {
                    Box(Modifier.width(140.dp).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(90.dp).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Lock, null, tint = AppColors.ext.gold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("+$count more device${if (count > 1) "s" else ""} on your network", style = MaterialTheme.typography.titleSmall)
        Text(
            "The free version shows ${AppConfig.FREE_DEVICE_LIMIT} devices. Unlock to see every device with its name, brand and details.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.ext.muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton("12h free", onClick = onUnlock, modifier = Modifier.weight(1f), height = 44.dp)
            SoftButton("Go Premium", onClick = onPaywall, tint = AppColors.ext.gold, modifier = Modifier.weight(1f), height = 44.dp)
        }
    }
}

@Composable
private fun DeviceSheet(d: NetDevice, networkKey: String?, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val version by AppGraph.knownDevices.version.collectAsState()
    val known = remember(version) { networkKey?.let { AppGraph.knownDevices.device(it, d.key) } }
    var renaming by remember { mutableStateOf(false) }
    val webPort = d.openPorts.firstOrNull { it == 80 || it == 443 || it == 8080 }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(deviceIcon(d.type), deviceColor(d.type), size = 58.dp, iconSize = 30.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(known?.customName ?: d.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Text(d.type.label, style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                }
                if (networkKey != null && !d.isSelf) {
                    SoftButton("Rename", onClick = { renaming = true }, icon = Icons.Rounded.Edit, height = 38.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            InfoRow("IP address", d.ip, copyable = true)
            InfoRow("MAC address", d.mac ?: "Hidden by Android", copyable = d.mac != null)
            InfoRow("Hostname", d.hostname ?: d.mdnsName ?: "Not shared")
            InfoRow("Brand", d.vendor ?: "Unknown")
            d.latencyMs?.let { InfoRow("Response time", "$it ms") }
            known?.let {
                InfoRow(
                    "First seen",
                    android.text.format.DateUtils.getRelativeTimeSpanString(it.firstSeen, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString(),
                )
            }
            if (d.openPorts.isNotEmpty() || d.services.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Services", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.openPorts.forEach { Pill(NetworkScanner.portName(it), MaterialTheme.colorScheme.primary) }
                    d.services.forEach { Pill(it.trimStart('_'), MaterialTheme.colorScheme.secondary) }
                }
            }
            if (networkKey != null && !d.isSelf && !d.isGateway) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = AppColors.ext.success)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Trusted device", style = MaterialTheme.typography.titleSmall)
                        Text("Mark devices you recognize", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                    }
                    Switch(
                        checked = known?.trusted == true,
                        onCheckedChange = {
                            AppGraph.knownDevices.setTrusted(networkKey, d.key, it)
                            snackbar.show(if (it) "Marked as trusted" else "Removed from trusted devices", SnackType.Success)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppColors.ext.success),
                    )
                }
            }
            if (webPort != null) {
                Spacer(Modifier.height(12.dp))
                SoftButton(
                    "Open device web page",
                    onClick = { openUrl(context, (if (webPort == 443) "https://" else "http://") + d.ip + if (webPort == 8080) ":8080" else "") },
                    icon = Icons.Rounded.OpenInNew,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (renaming && networkKey != null) {
        var text by remember { mutableStateOf(known?.customName ?: d.displayName) }
        AppDialog(
            onDismiss = { renaming = false },
            icon = Icons.Rounded.Edit,
            title = "Rename device",
            confirmText = "Save",
            onConfirm = {
                AppGraph.knownDevices.rename(networkKey, d.key, text.trim().ifBlank { null })
                renaming = false
                snackbar.show("Device renamed", SnackType.Success)
            },
            dismissText = "Cancel",
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(40) },
                singleLine = true,
                label = { Text("Device name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

