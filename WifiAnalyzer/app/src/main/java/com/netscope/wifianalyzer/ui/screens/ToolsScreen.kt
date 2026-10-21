package com.netscope.wifianalyzer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
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
import com.netscope.wifianalyzer.ui.components.rememberIsPremium
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

private val toolSections = listOf(
    "Test & measure" to listOf(Routes.SPEED, Routes.SIGNAL, Routes.HISTORY, Routes.WIFI_INFO),
    "Network & security" to listOf(Routes.DEVICES, Routes.INTRUDER, Routes.ROUTER),
    "Optimize" to listOf(Routes.CHANNELS, Routes.HEATMAP, Routes.DATA_USAGE, Routes.WIDGETS),
)

@Composable
fun ToolTile(tool: ToolItem, locked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(
        modifier.height(140.dp),
        onClick = onClick,
        padding = PaddingValues(14.dp),
        brush = Brush.linearGradient(
            listOf(tool.color.copy(alpha = 0.13f), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface),
        ),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(tool.color, tool.color.copy(alpha = 0.6f)))),
                contentAlignment = Alignment.Center,
            ) { Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(23.dp)) }
            Spacer(Modifier.weight(1f))
            when {
                tool.pro && locked -> Box(
                    Modifier.size(26.dp).clip(CircleShape).background(AppColors.ext.gold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Lock, "Locked", tint = AppColors.ext.gold, modifier = Modifier.size(14.dp)) }
                tool.pro -> ProBadge(size = 24.dp)
                else -> Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = AppColors.ext.subtle, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(tool.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted, maxLines = 2, lineHeight = 15.sp)
    }
}

@Composable
fun ToolsScreen(nav: NavHostController) {
    val isPremium by rememberIsPremium()
    val actions = LocalPremiumActions.current
    val lockedCount = allTools.count { it.pro }
    ScreenScaffold(title = "Tools", onBack = null, topLevel = true) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(2) }, contentType = "hero") {
                if (isPremium) {
                    AppCard(Modifier.fillMaxWidth(), brush = AppColors.ext.heroGradient) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProBadge(size = 44.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("All ${allTools.size} tools unlocked", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Text("Enjoy every Pro feature with no ads", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    PremiumStatusCardCompact(
                        lockedCount = lockedCount,
                        onUnlock = actions.openRewardUnlock,
                        onPaywall = actions.openPaywall,
                    )
                }
            }
            toolSections.forEach { (title, routes) ->
                item(span = { GridItemSpan(2) }, key = "h_$title", contentType = "header") {
                    Text(
                        title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.ext.muted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                    )
                }
                val tools = routes.mapNotNull { r -> allTools.firstOrNull { it.route == r } }
                items(tools, key = { it.route }, contentType = { "tool" }) { tool ->
                    ToolTile(tool, locked = tool.pro && !isPremium) { openTool(nav, tool) }
                }
            }
        }
    }
}
