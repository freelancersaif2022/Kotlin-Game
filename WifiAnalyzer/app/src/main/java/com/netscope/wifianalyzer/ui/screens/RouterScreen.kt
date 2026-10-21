package com.netscope.wifianalyzer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.NavRow
import com.netscope.wifianalyzer.ui.components.PremiumGate
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.components.copyToClipboard
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette

private val commonAddresses = listOf(
    "192.168.0.1" to "TP-Link, D-Link, Netgear",
    "192.168.1.1" to "Most routers, ASUS, Linksys",
    "192.168.1.254" to "BT, Thomson, ISP routers",
    "10.0.0.1" to "Xfinity, Comcast",
    "192.168.31.1" to "Xiaomi",
    "192.168.8.1" to "Huawei",
    "tplinkwifi.net" to "TP-Link",
    "routerlogin.net" to "NETGEAR",
    "router.asus.com" to "ASUS",
)

@Composable
fun RouterScreen(nav: NavHostController) {
    ScreenScaffold(title = "Router admin", onBack = { nav.popBackStack() }) { padding ->
        PremiumGate(
            feature = "Router admin shortcut",
            description = "Open your router's settings page in one tap to change the Wi-Fi password, channel or see connected devices.",
            icon = Icons.Rounded.Router,
            modifier = Modifier.padding(padding),
        ) { RouterContent(padding) }
    }
}

@Composable
private fun RouterContent(padding: PaddingValues) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val net by rememberNetState(5_000)
    val gw = net.snap.gateway
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        AppCard(Modifier.fillMaxWidth(), brush = AppColors.ext.heroGradient, padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Router, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Your router", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text(gw ?: "Not connected", color = Color.White, style = MaterialTheme.typography.displaySmall)
            Text(
                net.snap.ssid ?: if (net.snap.wifiConnected) "Wi-Fi network" else "Connect to Wi-Fi to find your router",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            GradientButton(
                "Open router settings",
                onClick = { gw?.let { openUrl(context, "http://$it") } },
                enabled = gw != null,
                icon = Icons.Rounded.OpenInBrowser,
                modifier = Modifier.fillMaxWidth(),
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White, Color(0xFFE0F7FF))),
            )
            Spacer(Modifier.height(10.dp))
            SoftButton(
                "Copy address",
                onClick = {
                    gw?.let {
                        context.copyToClipboard("Router", it)
                        snackbar.show("Router address copied", SnackType.Success)
                    }
                },
                icon = Icons.Rounded.ContentCopy,
                tint = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SectionTitle("How to log in")
        AppCard(Modifier.fillMaxWidth()) {
            Step(Icons.Rounded.VpnKey, "Find the login on your router", "The admin username and password are usually printed on a sticker under the router. They are different from your Wi-Fi password.")
            Step(Icons.Rounded.Shield, "Change the default password", "If you still use the default admin password, change it to protect your network.")
            Step(Icons.Rounded.Language, "Page won't open?", "Make sure you're connected to this router's Wi-Fi and mobile data isn't forcing traffic elsewhere.")
        }
        SectionTitle("Common router addresses")
        AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            commonAddresses.forEach { (addr, brands) ->
                NavRow(Icons.Rounded.OpenInBrowser, Palette.Violet, addr, brands) {
                    openUrl(context, if (addr.contains("://")) addr else "http://$addr")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Step(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Row(Modifier.padding(vertical = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
        }
    }
}
