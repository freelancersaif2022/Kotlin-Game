package com.netscope.wifianalyzer.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.net.ServerInfo
import com.netscope.wifianalyzer.net.SpeedTestEngine
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.InfoRow
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.signalColor
import com.netscope.wifianalyzer.ui.theme.AppColors

@Composable
fun WifiInfoScreen(nav: NavHostController) {
    val net by rememberNetState()
    val snap = net.snap
    val publicInfo by produceState<ServerInfo?>(null, snap.wifiConnected, snap.connectionType) {
        value = SpeedTestEngine.publicInfo()
    }
    ScreenScaffold(title = "Wi-Fi details", onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            if (net.needsLocation || !net.wifiOn) {
                LocationNeededCard(net)
                Spacer(Modifier.height(12.dp))
            }
            SectionTitle("Connection")
            AppCard(Modifier.fillMaxWidth()) {
                InfoRow("Status", if (snap.wifiConnected) "Connected" else "Not on Wi-Fi (${snap.connectionType})")
                Divider()
                InfoRow("Network name", snap.ssid ?: if (snap.wifiConnected) "Hidden (needs location)" else "-", copyable = snap.ssid != null)
                Divider()
                InfoRow("BSSID", snap.bssid ?: "-", copyable = snap.bssid != null)
                Divider()
                InfoRow(
                    "Signal",
                    if (snap.hasSignal) "${snap.rssi} dBm • ${WifiUtils.qualityLabel(snap.rssi)}" else "-",
                    valueColor = if (snap.hasSignal) signalColor(snap.rssi) else null,
                )
                Divider()
                InfoRow("Security", snap.security ?: "-")
                Divider()
                InfoRow("Wi-Fi standard", snap.standard ?: "-")
                Divider()
                InfoRow("Frequency", if (snap.frequency > 0) "${snap.frequency} MHz (${snap.band})" else "-")
                Divider()
                InfoRow("Channel", if (snap.channel > 0) "${snap.channel}" else "-")
                Divider()
                InfoRow("Link speed", if (snap.linkSpeed > 0) "${snap.linkSpeed} Mbps" else "-")
                if (snap.txSpeed > 0 || snap.rxSpeed > 0) {
                    Divider()
                    InfoRow("Tx / Rx speed", "${snap.txSpeed} / ${snap.rxSpeed} Mbps")
                }
            }

            SectionTitle("Network")
            AppCard(Modifier.fillMaxWidth()) {
                InfoRow("Local IP", snap.ip ?: "-", copyable = snap.ip != null)
                Divider()
                InfoRow("Gateway (router)", snap.gateway ?: "-", copyable = snap.gateway != null)
                Divider()
                InfoRow("Subnet mask", if (snap.ip != null) snap.subnetMask else "-")
                Divider()
                InfoRow("DNS 1", snap.dns.getOrNull(0) ?: "-", copyable = snap.dns.isNotEmpty())
                Divider()
                InfoRow("DNS 2", snap.dns.getOrNull(1) ?: "-", copyable = snap.dns.size > 1)
                Divider()
                InfoRow("IPv6", snap.ipv6 ?: "Not available", copyable = snap.ipv6 != null)
                Divider()
                InfoRow("Public IP", publicInfo?.ip?.ifBlank { null } ?: "Loading…", copyable = publicInfo?.ip?.isNotBlank() == true)
                Divider()
                InfoRow("Internet provider", publicInfo?.isp?.ifBlank { null } ?: "-")
                Divider()
                InfoRow(
                    "Location (approx.)",
                    listOfNotNull(publicInfo?.city?.ifBlank { null }, publicInfo?.country?.ifBlank { null }).joinToString(", ").ifBlank { "-" },
                )
            }

            SectionTitle("This device")
            AppCard(Modifier.fillMaxWidth()) {
                InfoRow("Model", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}")
                Divider()
                InfoRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                Divider()
                InfoRow("Wi-Fi MAC", "Private (randomized by Android)")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = AppColors.ext.cardBorder)
