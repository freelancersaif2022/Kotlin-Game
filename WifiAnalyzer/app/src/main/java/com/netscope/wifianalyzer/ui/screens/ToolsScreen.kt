package com.netscope.wifianalyzer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.IconBadge
import com.netscope.wifianalyzer.ui.components.LocalPremiumActions
import com.netscope.wifianalyzer.ui.components.PremiumStatusCardCompact
import com.netscope.wifianalyzer.ui.components.ProBadge
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.rememberPremiumState
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.navigation.navigateTab
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette

data class ToolItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val pro: Boolean,
    val tab: Boolean = false,
)

val allTools = listOf(
    ToolItem(Routes.SPEED, "Speed test", "Download, upload, ping & jitter", Icons.Rounded.Speed, Palette.Cyan, pro = false, tab = true),
    ToolItem(Routes.DEVICES, "Who's on my Wi-Fi", "Find every connected device", Icons.Rounded.Devices, Palette.Lilac, pro = false, tab = true),
    ToolItem(Routes.SIGNAL, "Signal meter", "Find the best spot at home", Icons.Rounded.SignalWifi4Bar, Palette.Mint, pro = false),
    ToolItem(Routes.WIFI_INFO, "Wi-Fi details", "IP, gateway, DNS, security", Icons.Rounded.Info, Palette.Sky, pro = false),
    ToolItem(Routes.HISTORY, "Speed history", "Track results over time", Icons.Rounded.History, Palette.Blue, pro = false),
    ToolItem(Routes.CHANNELS, "Channel analyzer", "Pick the least crowded channel", Icons.Rounded.Tune, Palette.Orange, pro = true),
    ToolItem(Routes.HEATMAP, "Signal heatmap", "Map weak & strong zones", Icons.Rounded.GridOn, Palette.Pink, pro = true),
    ToolItem(Routes.INTRUDER, "Intruder alert", "Get notified of new devices", Icons.Rounded.Security, Palette.Rose, pro = true),
    ToolItem(Routes.DATA_USAGE, "Data usage", "Daily & monthly per app", Icons.Rounded.DataUsage, Palette.Gold, pro = true),
    ToolItem(Routes.ROUTER, "Router admin", "Open your router settings", Icons.Rounded.Router, Palette.Violet, pro = true),
    ToolItem(Routes.WIDGETS, "Home widgets", "Speed & signal at a glance", Icons.Rounded.Widgets, Palette.Cyan, pro = true),
)

val homeTools: List<ToolItem> = listOf(
    Routes.DEVICES, Routes.SIGNAL, Routes.CHANNELS, Routes.HEATMAP, Routes.INTRUDER, Routes.DATA_USAGE, Routes.ROUTER, Routes.WIFI_INFO,
).mapNotNull { r -> allTools.firstOrNull { it.route == r } }

fun openTool(nav: NavHostController, tool: ToolItem) {
    if (tool.tab) nav.navigateTab(tool.route) else nav.navigate(tool.route) { launchSingleTop = true }
}

@Composable
fun ToolTile(tool: ToolItem, locked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier.height(138.dp), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            IconBadge(tool.icon, tool.color, size = 42.dp)
            Spacer(Modifier.weight(1f))
            if (tool.pro) {
                if (locked) Icon(Icons.Rounded.Lock, null, tint = AppColors.ext.gold, modifier = Modifier.size(16.dp)) else ProBadge()
            }
        }
        Spacer(Modifier.weight(1f))
        Text(tool.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted, maxLines = 2)
    }
}

@Composable
fun ToolsScreen(nav: NavHostController) {
    val premium by rememberPremiumState()
    val actions = LocalPremiumActions.current
    ScreenScaffold(title = "Tools", onBack = null, topLevel = true) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!premium.isPremium) {
                item(span = { GridItemSpan(2) }) {
                    Box(Modifier.padding(bottom = 4.dp)) {
                        PremiumStatusCardCompact(onUnlock = actions.openRewardUnlock, onPaywall = actions.openPaywall)
                    }
                }
            }
            items(allTools, key = { it.route }) { tool ->
                ToolTile(tool, locked = tool.pro && !premium.isPremium) { openTool(nav, tool) }
            }
        }
    }
}
