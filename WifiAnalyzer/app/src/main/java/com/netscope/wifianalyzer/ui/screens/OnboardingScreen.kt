package com.netscope.wifianalyzer.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netscope.wifianalyzer.ui.components.AppIcons
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.IconBadge
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.components.SpeedGauge
import com.netscope.wifianalyzer.ui.components.rememberPopIn
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private data class OnboardPage(val title: String, val text: String, val accent: Color)

private val pages = listOf(
    OnboardPage("Test your internet speed", "Accurate download, upload, ping and jitter results in seconds, with history to track your provider.", Palette.Cyan),
    OnboardPage("See who's on your Wi-Fi", "Find every phone, TV and smart device on your network, and get alerts when someone new connects.", Palette.Lilac),
    OnboardPage("Boost your signal", "Live signal meter, channel analyzer and room heatmap show you exactly where and how to get faster Wi-Fi.", Palette.Mint),
    OnboardPage("Pro tools, your way", "Unlock every Pro feature free for 12 hours by watching 3 short ads, or go Premium to remove ads.", Palette.Gold),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val bg = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(pages[pager.currentPage].accent.copy(alpha = 0.18f), bg, bg),
                ),
            ),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("WiFi Analyzer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (pager.currentPage < pages.lastIndex) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppColors.ext.muted,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onFinish() }.padding(10.dp),
                    )
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = lerp(1f, 0.3f, offset.coerceIn(0f, 1f))
                            val s = lerp(1f, 0.88f, offset.coerceIn(0f, 1f))
                            scaleX = s
                            scaleY = s
                        }
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                        when (page) {
                            0 -> SpeedIllustration()
                            1 -> RadarIllustration()
                            2 -> SignalIllustration()
                            else -> PremiumIllustration()
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(pages[page].title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        pages[page].text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.ext.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
                pages.indices.forEach { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 28.dp else 8.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) pages[i].accent else MaterialTheme.colorScheme.outline),
                    )
                }
            }
            GradientButton(
                text = if (pager.currentPage == pages.lastIndex) "Get started" else "Next",
                onClick = {
                    if (pager.currentPage == pages.lastIndex) onFinish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                icon = Icons.Rounded.ArrowForward,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun SpeedIllustration() {
    val t = rememberInfiniteTransition(label = "speedIll")
    val v by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "v",
    )
    val mbps = lerp(5f, 480f, v * v)
    SpeedGauge(mbps, listOf(Palette.Cyan, Palette.Blue, Palette.Violet), Modifier.size(260.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${mbps.toInt()}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text("Mbps", style = MaterialTheme.typography.titleSmall, color = AppColors.ext.muted)
        }
    }
}

@Composable
private fun RadarIllustration() {
    val t = rememberInfiniteTransition(label = "radar")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "sweep")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "ring")
    val accent = Palette.Lilac
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            for (i in 1..3) drawCircle(accent.copy(alpha = 0.18f), r * i / 3f, style = Stroke(1.5.dp.toPx()))
            drawCircle(accent.copy(alpha = (1f - ring) * 0.5f), r * ring, style = Stroke(3.dp.toPx()))
            val a = sweep * PI / 180.0
            drawLine(
                Brush.linearGradient(listOf(accent.copy(alpha = 0f), accent), center, Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())),
                center,
                Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val icons: List<Pair<ImageVector, Offset>> = listOf(
            Icons.Rounded.PhoneAndroid to Offset(-70f, -60f),
            Icons.Rounded.Tv to Offset(76f, -34f),
            Icons.Rounded.Computer to Offset(-58f, 66f),
            Icons.Rounded.Print to Offset(62f, 74f),
            Icons.Rounded.Videocam to Offset(4f, -104f),
        )
        icons.forEachIndexed { i, (icon, off) ->
            val pop = rememberPopIn(200 + i * 220)
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = off.x * density
                        translationY = off.y * density
                        scaleX = pop
                        scaleY = pop
                    }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
        }
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Palette.Violet, Palette.Cyan))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Router, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
    }
}

@Composable
private fun SignalIllustration() {
    val t = rememberInfiniteTransition(label = "signal")
    val phase by t.animateFloat(0f, 4f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "phase")
    val bars by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bars")
    val accent = Palette.Mint
    Canvas(Modifier.size(260.dp)) {
        val c = Offset(size.width / 2, size.height * 0.62f)
        val stroke = 16.dp.toPx()
        for (i in 0 until 3) {
            val r = size.minDimension * (0.16f + 0.14f * i)
            val active = phase > i + 1
            drawArc(
                color = if (active) accent else accent.copy(alpha = 0.18f),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        drawCircle(accent, 12.dp.toPx(), c)
        val bw = 14.dp.toPx()
        val heights = listOf(0.35f, 0.55f, 0.45f, 0.75f, 0.6f, 0.9f)
        heights.forEachIndexed { i, h ->
            val hh = size.height * 0.18f * (h * (0.6f + 0.4f * ((bars + i * 0.17f) % 1f)))
            val x = size.width * 0.14f + i * (bw + 8.dp.toPx())
            drawRoundRect(
                Palette.Cyan.copy(alpha = 0.7f),
                Offset(x, size.height * 0.98f - hh),
                androidx.compose.ui.geometry.Size(bw, hh),
                androidx.compose.ui.geometry.CornerRadius(bw / 3),
            )
        }
    }
}

@Composable
private fun PremiumIllustration() {
    val t = rememberInfiniteTransition(label = "premium")
    val float by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "float")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rot")
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rot }) {
            val r = size.minDimension / 2.3f
            for (i in 0 until 8) {
                val a = i * PI / 4
                drawCircle(
                    if (i % 2 == 0) Palette.Gold else Palette.Pink,
                    (if (i % 2 == 0) 5 else 3).dp.toPx(),
                    Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat()),
                )
            }
        }
        Box(
            Modifier
                .graphicsLayer { translationY = float * density }
                .size(150.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(AppColors.ext.premiumGradient),
            contentAlignment = Alignment.Center,
        ) { Icon(AppIcons.Crown, null, tint = Color.White, modifier = Modifier.size(84.dp)) }
    }
}

// ---------------------------------------------------------------- Permissions

@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    var locationGranted by remember { mutableStateOf(WifiUtils.hasLocationPermission(context)) }
    var notifGranted by remember { mutableStateOf(WifiUtils.hasNotificationPermission(context)) }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        locationGranted = res[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (locationGranted) snackbar.show("Location access granted", SnackType.Success)
        else if (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            snackbar.show("Choose \"Precise\" location so Android can show your Wi-Fi name", SnackType.Info)
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notifGranted = ok
        if (ok) snackbar.show("Notifications enabled", SnackType.Success)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        val pop = rememberPopIn()
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                }
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.ext.primaryGradient),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(22.dp))
        Text("A couple of quick permissions", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Android protects Wi-Fi details behind these permissions. We only use them on your device to analyze your network.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.ext.muted,
        )
        Spacer(Modifier.height(24.dp))
        PermissionCard(
            icon = Icons.Rounded.LocationOn,
            tint = Palette.Gold,
            title = "Precise location",
            text = "Required by Android to read your Wi-Fi name, scan nearby networks and find the best channel. Your location is never stored or sent anywhere.",
            granted = locationGranted,
            onAllow = { locationLauncher.launch(LOCATION_PERMISSIONS) },
        )
        Spacer(Modifier.height(14.dp))
        PermissionCard(
            icon = Icons.Rounded.Notifications,
            tint = Palette.Lilac,
            title = "Notifications",
            text = "Optional. Lets Intruder Alert tell you the moment an unknown device joins your Wi-Fi.",
            granted = notifGranted,
            onAllow = {
                if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else notifGranted = true
            },
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Your privacy matters: scans run locally and nothing about your network is uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(28.dp))
        GradientButton(
            if (locationGranted) "Continue" else "Continue without location",
            onClick = onDone,
            icon = Icons.Rounded.ArrowForward,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    text: String,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    AppCard(Modifier.fillMaxWidth().animateContentSize()) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (granted) Icon(Icons.Rounded.CheckCircle, "Granted", tint = AppColors.ext.success)
                }
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
                if (!granted) {
                    Spacer(Modifier.height(12.dp))
                    SoftButton("Allow", onClick = onAllow, tint = tint, height = 42.dp, icon = Icons.Rounded.Security)
                }
            }
        }
    }
}
