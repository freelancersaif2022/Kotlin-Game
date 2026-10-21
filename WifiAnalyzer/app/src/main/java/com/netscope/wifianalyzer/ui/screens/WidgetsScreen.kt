package com.netscope.wifianalyzer.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.PremiumGate
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.theme.Palette
import com.netscope.wifianalyzer.widget.SignalWidgetProvider
import com.netscope.wifianalyzer.widget.SpeedWidgetProvider
import com.netscope.wifianalyzer.widget.WidgetUpdater

private val widgetBg = Brush.linearGradient(listOf(Color(0xFF1A2A6C), Color(0xFF05081A)))

@Composable
fun WidgetsScreen(nav: NavHostController) {
    ScreenScaffold(title = "Home screen widgets", onBack = { nav.popBackStack() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PremiumGate(
                feature = "Home screen widgets",
                description = "Check your Wi-Fi signal and last speed test right from your home screen, and start a test in one tap.",
                icon = Icons.Rounded.Widgets,
                preview = { Column(Modifier.padding(20.dp)) { SignalWidgetMock(); Spacer(Modifier.height(16.dp)); SpeedWidgetMock() } },
            ) { WidgetsContent() }
        }
    }
}

@Composable
private fun WidgetsContent() {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    fun pin(provider: Class<*>) {
        val mgr = AppWidgetManager.getInstance(context)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(context, provider), null, null)
            WidgetUpdater.updateAll(context)
        } else {
            snackbar.show("Long-press your home screen, tap Widgets, then find WiFi Analyzer", SnackType.Info)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        SectionTitle("Signal widget")
        SignalWidgetMock()
        Spacer(Modifier.height(10.dp))
        SoftButton("Add to home screen", onClick = { pin(SignalWidgetProvider::class.java) }, icon = Icons.Rounded.AddToHomeScreen, modifier = Modifier.fillMaxWidth())
        SectionTitle("Speed test widget", Modifier.padding(top = 12.dp))
        SpeedWidgetMock()
        Spacer(Modifier.height(10.dp))
        SoftButton("Add to home screen", onClick = { pin(SpeedWidgetProvider::class.java) }, icon = Icons.Rounded.AddToHomeScreen, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WidgetFrame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(widgetBg)
            .border(1.dp, Palette.Cyan.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun SignalWidgetMock() {
    val net by rememberNetState(3_000)
    val s = net.snap
    WidgetFrame {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Wifi, null, tint = Palette.Cyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Wi-Fi Signal", color = Color(0xFFA9B4D0), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text(if (s.hasSignal) WifiUtils.qualityLabel(s.rssi) else "Offline", color = Palette.Cyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(s.ssid ?: "My Wi-Fi", color = Color.White, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (s.hasSignal) "${s.rssi}" else "-52", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Text(" dBm", color = Color(0xFFA9B4D0), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Text("${if (s.hasSignal) s.signalPercent else 74}%", color = Palette.Lilac, style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))) {
                Box(
                    Modifier
                        .fillMaxWidth((if (s.hasSignal) s.signalPercent else 74) / 100f)
                        .height(6.dp)
                        .background(Brush.horizontalGradient(listOf(Palette.Cyan, Palette.Lilac))),
                )
            }
        }
    }
}

@Composable
private fun SpeedWidgetMock() {
    val history by AppGraph.history.results.collectAsState()
    val unit by AppGraph.prefs.unit.collectAsState()
    val last = history.firstOrNull()
    WidgetFrame {
        Column {
            Text("Speed Test", color = Color(0xFFA9B4D0), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MockValue("Download", last?.let { WifiUtils.formatSpeed(it.downloadMbps, unit).first } ?: "245", Palette.Cyan)
                MockValue("Upload", last?.let { WifiUtils.formatSpeed(it.uploadMbps, unit).first } ?: "48.2", Palette.Lilac)
                MockValue("Ping", last?.pingMs?.toInt()?.toString() ?: "12", Palette.Gold)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Palette.Cyan, Palette.Violet))),
                contentAlignment = Alignment.Center,
            ) { Text("Run speed test", color = Color(0xFF070B18), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun MockValue(label: String, value: String, color: Color) {
    Column {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}
