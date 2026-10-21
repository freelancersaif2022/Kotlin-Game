package com.netscope.wifianalyzer.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.play.core.review.ReviewManagerFactory
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.ads.AdPlacement
import com.netscope.wifianalyzer.data.SpeedResult
import com.netscope.wifianalyzer.data.SpeedUnit
import com.netscope.wifianalyzer.net.SpeedPhase
import com.netscope.wifianalyzer.net.SpeedProgress
import com.netscope.wifianalyzer.net.SpeedTestEngine
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.LineChart
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.Pill
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.components.SpeedGauge
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.components.pressScale
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import com.netscope.wifianalyzer.widget.WidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SpeedTestViewModel(app: Application) : AndroidViewModel(app) {
    private val _progress = MutableStateFlow(SpeedProgress())
    val progress: StateFlow<SpeedProgress> = _progress.asStateFlow()

    private val _result = MutableStateFlow<SpeedResult?>(null)
    val result: StateFlow<SpeedResult?> = _result.asStateFlow()

    /** A finished result waiting for the post-test interstitial before it is shown. */
    private val _pending = MutableStateFlow<SpeedResult?>(null)
    val pending: StateFlow<SpeedResult?> = _pending.asStateFlow()

    private var job: Job? = null
    val running: Boolean get() = job?.isActive == true

    fun start() {
        if (running) return
        _result.value = null
        val snapBefore = WifiUtils.snapshot(getApplication())
        job = viewModelScope.launch {
            SpeedTestEngine().run().collect { p ->
                _progress.value = p
                if (p.phase == SpeedPhase.DONE) {
                    val r = SpeedResult(
                        id = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis(),
                        downloadMbps = p.downloadMbps ?: 0.0,
                        uploadMbps = p.uploadMbps ?: 0.0,
                        pingMs = p.pingMs ?: 0.0,
                        jitterMs = p.jitterMs ?: 0.0,
                        network = if (snapBefore.wifiConnected) snapBefore.ssid ?: "Wi-Fi" else snapBefore.connectionType,
                        connection = snapBefore.connectionType,
                        isp = p.server?.isp.orEmpty(),
                        server = p.server?.serverLabel.orEmpty(),
                        rssi = snapBefore.rssi.takeIf { snapBefore.hasSignal },
                    )
                    AppGraph.history.add(r)
                    AppGraph.prefs.completedTests = AppGraph.prefs.completedTests + 1
                    WidgetUpdater.updateAll(getApplication())
                    _pending.value = r
                }
            }
        }
    }

    fun reveal(r: SpeedResult) {
        _result.value = r
        _pending.value = null
    }

    fun cancel() {
        job?.cancel()
        job = null
        _progress.value = SpeedProgress()
        _result.value = null
        _pending.value = null
    }

    fun reset() {
        _progress.value = SpeedProgress()
        _result.value = null
    }
}

private val downloadColors = listOf(Palette.Cyan, Palette.Blue, Palette.Violet)
private val uploadColors = listOf(Palette.Lilac, Palette.Violet, Palette.Pink)

@Composable
fun SpeedTestScreen(nav: NavHostController, autoStart: Boolean) {
    val vm: SpeedTestViewModel = viewModel()
    val p by vm.progress.collectAsState()
    val result by vm.result.collectAsState()
    val unit by AppGraph.prefs.unit.collectAsState()
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    var autoStarted by rememberSaveable { mutableStateOf(false) }
    val net by rememberNetState(4_000)

    fun startTest() {
        if (!net.snap.hasInternet && net.snap.connectionType == "Offline") {
            snackbar.show("You're offline. Connect to Wi-Fi or mobile data first.", SnackType.Error)
            return
        }
        vm.start()
    }

    LaunchedEffect(autoStart) {
        if (autoStart && !autoStarted && !vm.running && result == null) {
            autoStarted = true
            startTest()
        }
    }
    val pending by vm.pending.collectAsState()
    LaunchedEffect(pending) {
        pending?.let { r ->
            val activity = context.findActivity()
            AppGraph.ads.showInterstitial(activity, AdPlacement.SPEED_TEST_DONE) {
                vm.reveal(r)
                if (AppGraph.prefs.completedTests == 3 && !AppGraph.prefs.reviewRequested && activity != null) {
                    AppGraph.prefs.reviewRequested = true
                    val manager = ReviewManagerFactory.create(activity)
                    manager.requestReviewFlow().addOnCompleteListener { task ->
                        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
                    }
                }
            }
        }
    }

    val running = p.phase in listOf(SpeedPhase.CONNECTING, SpeedPhase.PING, SpeedPhase.DOWNLOAD, SpeedPhase.UPLOAD) ||
        (p.phase == SpeedPhase.DONE && result == null)

    ScreenScaffold(
        title = "Speed test",
        onBack = null,
        topLevel = true,
        actions = {
            IconButton(onClick = { nav.navigate(Routes.HISTORY) }) { Icon(Icons.Rounded.History, "History") }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ServerRow(p, net)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth().height(310.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = p.phase == SpeedPhase.IDLE || p.phase == SpeedPhase.ERROR,
                    transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.85f)) togetherWith fadeOut(tween(200)) },
                    label = "gaugeSwap",
                ) { idle ->
                    if (idle) {
                        StartButton { startTest() }
                    } else {
                        val live = when (p.phase) {
                            SpeedPhase.DOWNLOAD, SpeedPhase.UPLOAD -> p.liveMbps
                            SpeedPhase.DONE -> result?.downloadMbps ?: p.downloadMbps ?: 0.0
                            else -> 0.0
                        }
                        val colors = if (p.phase == SpeedPhase.UPLOAD) uploadColors else downloadColors
                        SpeedGauge(live.toFloat(), colors, Modifier.size(300.dp)) {
                            GaugeCenter(p, live, unit, result != null)
                        }
                    }
                }
            }

            AnimatedVisibility(running) {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { p.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(12.dp))
                    if (p.phase == SpeedPhase.DOWNLOAD || p.phase == SpeedPhase.UPLOAD) {
                        LineChart(
                            values = p.samples,
                            color = if (p.phase == SpeedPhase.UPLOAD) AppColors.ext.upload else AppColors.ext.download,
                            modifier = Modifier.fillMaxWidth().height(70.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    Icons.Rounded.Timer, "Ping", p.pingMs?.let { "${it.toInt()}" } ?: "--", "ms",
                    AppColors.ext.ping, p.phase == SpeedPhase.PING, Modifier.weight(1f),
                )
                MetricCard(
                    Icons.Rounded.GraphicEq, "Jitter", p.jitterMs?.let { "${it.toInt()}" } ?: "--", "ms",
                    Palette.Orange, p.phase == SpeedPhase.PING, Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    Icons.Rounded.ArrowDownward, "Download",
                    p.downloadMbps?.let { WifiUtils.formatSpeed(it, unit).first }
                        ?: if (p.phase == SpeedPhase.DOWNLOAD) WifiUtils.formatSpeed(p.liveMbps, unit).first else "--",
                    unit.label, AppColors.ext.download, p.phase == SpeedPhase.DOWNLOAD, Modifier.weight(1f),
                )
                MetricCard(
                    Icons.Rounded.ArrowUpward, "Upload",
                    p.uploadMbps?.let { WifiUtils.formatSpeed(it, unit).first }
                        ?: if (p.phase == SpeedPhase.UPLOAD) WifiUtils.formatSpeed(p.liveMbps, unit).first else "--",
                    unit.label, AppColors.ext.upload, p.phase == SpeedPhase.UPLOAD, Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(18.dp))

            if (p.phase == SpeedPhase.ERROR) {
                AppCard(Modifier.fillMaxWidth(), color = AppColors.ext.danger.copy(alpha = 0.08f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = AppColors.ext.danger)
                        Spacer(Modifier.width(12.dp))
                        Text(p.error ?: "Test failed", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            when {
                running -> SoftButton(
                    "Cancel test",
                    onClick = { vm.cancel() },
                    icon = Icons.Rounded.Close,
                    tint = AppColors.ext.danger,
                    modifier = Modifier.fillMaxWidth(),
                )
                result != null -> {
                    val r = result!!
                    ResultInsights(r, unit)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SoftButton(
                            "Share",
                            onClick = {
                                val (d, du) = WifiUtils.formatSpeed(r.downloadMbps, unit)
                                val (u, uu) = WifiUtils.formatSpeed(r.uploadMbps, unit)
                                shareText(
                                    context,
                                    "My internet speed: ⬇ $d $du  ⬆ $u $uu  • Ping ${r.pingMs.toInt()} ms\nTested with WiFi Analyzer: https://play.google.com/store/apps/details?id=${context.packageName}",
                                    "Share result",
                                )
                            },
                            icon = Icons.Rounded.Share,
                            modifier = Modifier.weight(1f),
                        )
                        GradientButton("Test again", onClick = { startTest() }, icon = Icons.Rounded.Refresh, modifier = Modifier.weight(1.4f), height = 52.dp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ServerRow(p: SpeedProgress, net: NetState) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Pill(
            p.server?.serverLabel ?: "Cloudflare global network",
            MaterialTheme.colorScheme.primary,
            icon = Icons.Rounded.Public,
        )
        val provider = p.server?.isp?.takeIf { it.isNotBlank() }
        Pill(
            provider ?: (if (net.snap.wifiConnected) net.snap.ssid ?: "Wi-Fi" else net.snap.connectionType),
            MaterialTheme.colorScheme.secondary,
            icon = Icons.Rounded.Language,
        )
    }
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "start")
    val r1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200)), label = "r1")
    val r2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, delayMillis = 700)), label = "r2")
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
        listOf(r1, r2).forEach { r ->
            Box(
                Modifier
                    .size(230.dp)
                    .graphicsLayer {
                        scaleX = 0.8f + r * 0.45f
                        scaleY = 0.8f + r * 0.45f
                        alpha = (1f - r) * 0.8f
                    }
                    .clip(CircleShape)
                    .border(2.dp, Palette.Cyan, CircleShape),
            )
        }
        Box(
            Modifier
                .size(196.dp)
                .pressScale(interaction)
                .clip(CircleShape)
                .background(Brush.linearGradient(downloadColors))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(176.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "GO",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text("Tap to start", style = MaterialTheme.typography.labelMedium, color = AppColors.ext.muted)
                }
            }
        }
    }
}

@Composable
private fun GaugeCenter(p: SpeedProgress, live: Double, unit: SpeedUnit, revealed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
        val label = when (p.phase) {
            SpeedPhase.CONNECTING -> "Connecting…"
            SpeedPhase.PING -> "Measuring ping"
            SpeedPhase.DOWNLOAD -> "Download"
            SpeedPhase.UPLOAD -> "Upload"
            SpeedPhase.DONE -> if (revealed) "Download" else "Finishing…"
            else -> ""
        }
        val icon: ImageVector? = when (p.phase) {
            SpeedPhase.DOWNLOAD -> Icons.Rounded.ArrowDownward
            SpeedPhase.UPLOAD -> Icons.Rounded.ArrowUpward
            SpeedPhase.DONE -> Icons.Rounded.CheckCircle
            else -> null
        }
        if (p.phase == SpeedPhase.PING || p.phase == SpeedPhase.CONNECTING) {
            Text(
                p.pingMs?.let { "${it.toInt()}" } ?: "…",
                style = MaterialTheme.typography.displayLarge,
            )
            Text("ms", style = MaterialTheme.typography.titleSmall, color = AppColors.ext.muted)
        } else {
            val (value, u) = WifiUtils.formatSpeed(live, unit)
            Text(value, style = MaterialTheme.typography.displayLarge)
            Text(u, style = MaterialTheme.typography.titleSmall, color = AppColors.ext.muted)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon, null,
                    tint = if (p.phase == SpeedPhase.UPLOAD) AppColors.ext.upload else AppColors.ext.download,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = AppColors.ext.muted)
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = rememberInfiniteTransition(label = "metric")
    val glow by t.animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (active) color.copy(alpha = glow) else AppColors.ext.cardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.ext.muted)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = AppColors.ext.muted, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

private data class UseCase(val icon: ImageVector, val name: String, val ok: Boolean)

@Composable
private fun ResultInsights(r: SpeedResult, unit: SpeedUnit) {
    val grade = when {
        r.downloadMbps >= 200 && r.pingMs < 30 -> "A+" to "Blazing fast"
        r.downloadMbps >= 100 -> "A" to "Excellent"
        r.downloadMbps >= 50 -> "B" to "Very good"
        r.downloadMbps >= 25 -> "C" to "Good"
        r.downloadMbps >= 10 -> "D" to "Fair"
        else -> "E" to "Slow"
    }
    val gradeColor = when (grade.first) {
        "A+", "A" -> AppColors.ext.success
        "B", "C" -> MaterialTheme.colorScheme.primary
        "D" -> AppColors.ext.warning
        else -> AppColors.ext.danger
    }
    val cases = listOf(
        UseCase(Icons.Rounded.Language, "Web browsing", r.downloadMbps >= 3),
        UseCase(Icons.Rounded.VideoCall, "HD video calls", r.downloadMbps >= 4 && r.uploadMbps >= 3 && r.pingMs < 150),
        UseCase(Icons.Rounded.Videocam, "4K streaming", r.downloadMbps >= 25),
        UseCase(Icons.Rounded.Games, "Online gaming", r.downloadMbps >= 10 && r.pingMs < 60 && r.jitterMs < 30),
        UseCase(Icons.Rounded.CloudDownload, "Large downloads", r.downloadMbps >= 100),
    )
    SectionTitle("Your result")
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(gradeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(grade.first, style = MaterialTheme.typography.headlineMedium, color = gradeColor, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(grade.second, style = MaterialTheme.typography.titleMedium)
                val (d, du) = WifiUtils.formatSpeed(r.downloadMbps, unit)
                Text(
                    String.format(Locale.US, "%s %s down • %d ms ping", d, du, r.pingMs.toInt()) +
                        if (r.isp.isNotBlank()) " • ${r.isp}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ext.muted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        cases.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(c.icon, null, tint = AppColors.ext.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(c.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (c.ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    null,
                    tint = if (c.ok) AppColors.ext.success else AppColors.ext.subtle,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
