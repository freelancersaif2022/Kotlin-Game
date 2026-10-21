package com.netscope.wifianalyzer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.IconBadge
import com.netscope.wifianalyzer.ui.components.ProBadge
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette

private data class Faq(val q: String, val a: String)

private val guides = mapOf(
    "Speed test" to "Tap GO and keep the app open for about 20 seconds. We measure ping and jitter first, then download and upload using several connections at once, just like professional speed tests. For the most accurate result, pause downloads and streaming on other devices.",
    "Who's on my Wi-Fi" to "Tap Scan network. We check every address on your network and list the devices that answer, with names and brands when they share them. The free version shows ${AppConfig.FREE_DEVICE_LIMIT} devices; Premium shows all of them.",
    "Signal meter" to "Shows your live signal in dBm. Walk around slowly to find the strongest spot. -30 to -55 is excellent, -56 to -67 good, -68 to -75 fair and below -75 weak. Turn on Sound guide to hear the signal while you walk.",
    "Channel analyzer" to "Shows every nearby network on the channels they use. Overlapping hills mean interference. We recommend the least crowded channel; change it in your router settings using Router admin.",
    "Signal heatmap" to "Treat the grid as your floor plan. Stand in a spot, tap the matching square and hold still for 3 seconds. Repeat across your home to see weak zones in red and strong zones in green.",
    "Intruder alert" to "Turn it on and we'll scan your network every 15 minutes while you're on Wi-Fi. If an unknown device appears you'll get a notification. Mark devices you recognize as trusted.",
    "Data usage" to "Allow usage access when asked. You'll see total Wi-Fi and mobile data for today, the last 7 days or this month, plus the apps that use the most.",
    "Widgets" to "Open Home screen widgets and tap Add to home screen, or long-press your home screen and pick WiFi Analyzer widgets.",
)

private val faqs = listOf(
    Faq("Why does the app need location permission?", "Since Android 8, Google requires location permission for any app to read the Wi-Fi network name (SSID) or scan nearby networks, because Wi-Fi data can reveal where you are. We only use it to show Wi-Fi details on your device and never store or share your location."),
    Faq("Why is my speed lower than my plan?", "Wi-Fi signal, distance from the router, interference, other devices using the network and your phone's own Wi-Fi chip all affect results. Test close to the router on 5 GHz to see your best possible speed."),
    Faq("How does the free 12 hour Premium work?", "Watch ${AppConfig.REWARD_ADS_REQUIRED} short video ads in a row and every Pro feature unlocks for 12 hours with no ads. Your progress is saved, so you can watch them at different times. You can do this again whenever it expires."),
    Faq("Why can't I see MAC addresses?", "Android 10 and newer block apps from reading other devices' MAC addresses for privacy, and many phones use random private addresses. We show the best details available: IP, name, brand and services."),
    Faq("A device is missing from the scan. Why?", "Some devices sleep, block network probes or are on a guest network. Run the scan again while the device is in use, and make sure your phone is on the same Wi-Fi."),
    Faq("Is the speed test using my mobile data?", "Only if you're not on Wi-Fi. A full test uses roughly 50 to 500 MB depending on your speed, so prefer Wi-Fi when testing."),
    Faq("How do I cancel my subscription?", "Open Settings > Manage subscription, or Google Play > Profile > Payments & subscriptions > Subscriptions. You keep Premium until the end of the paid period."),
    Faq("I bought Premium but it's not active.", "Go to Settings > Restore purchases while signed in to the same Google account you used to buy. Pending payments activate once Google Play confirms them."),
    Faq("Is my data private?", "Yes. Scans, history and heatmaps are stored only on your phone. The speed test talks to Cloudflare's public speed test servers. Ads are provided by Google AdMob; see our privacy policy for details."),
)

private val boostTips = listOf(
    "Move the router to a central, raised spot",
    "Use 5 GHz near the router, 2.4 GHz far away",
    "Switch to the least crowded channel",
    "Restart the router monthly",
    "Update router firmware",
    "Add a mesh node for big homes",
)

@Composable
fun HelpScreen(nav: NavHostController) {
    ScreenScaffold(title = "Help & guide", onBack = { nav.popBackStack() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionTitle("Boost your Wi-Fi") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(boostTips) { tip ->
                        AppCard(Modifier.width(170.dp).height(112.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)) {
                            Icon(Icons.Rounded.Lightbulb, null, tint = AppColors.ext.gold)
                            Spacer(Modifier.height(8.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { SectionTitle("How each tool works") }
            items(guides.entries.toList(), key = { it.key }) { (title, text) ->
                val pro = title in listOf("Channel analyzer", "Signal heatmap", "Intruder alert", "Data usage", "Widgets")
                Expandable(title, text, pro)
            }
            item { SectionTitle("Frequently asked questions") }
            items(faqs, key = { it.q }) { f -> Expandable(f.q, f.a, false) }
            item {
                Spacer(Modifier.height(8.dp))
                SoftButton("Read our privacy policy", onClick = { nav.navigate(Routes.PRIVACY) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Expandable(title: String, body: String, pro: Boolean) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    AppCard(Modifier.fillMaxWidth(), onClick = { open = !open }, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
            if (pro) {
                Spacer(Modifier.width(6.dp))
                ProBadge()
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.rotate(rot), tint = AppColors.ext.muted)
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(body, style = MaterialTheme.typography.bodyMedium, color = AppColors.ext.muted, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
fun PrivacyScreen(nav: NavHostController) {
    val context = LocalContext.current
    ScreenScaffold(title = "Privacy policy", onBack = { nav.popBackStack() }, showBanner = false) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Lightbulb, Palette.Mint)
                    Spacer(Modifier.width(12.dp))
                    Text("The short version: your network data stays on your phone.", style = MaterialTheme.typography.titleSmall)
                }
            }
            PRIVACY_SECTIONS.forEach { (h, b) ->
                SectionTitle(h)
                Text(b, style = MaterialTheme.typography.bodyMedium, color = AppColors.ext.muted)
            }
            Spacer(Modifier.height(16.dp))
            Text("Full policy online: ${AppConfig.PRIVACY_POLICY_URL}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { openUrl(context, AppConfig.PRIVACY_POLICY_URL) }.padding(vertical = 8.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

val PRIVACY_SECTIONS = listOf(
    "Information we process" to "To show Wi-Fi details the app reads your network name, signal strength, IP addresses and nearby networks using Android APIs. Device scans find devices on your local network. All of this is processed on your device and is not uploaded to us.",
    "Location permission" to "Android requires location permission to read Wi-Fi names and scan results. The app does not collect, store or share your location.",
    "Speed tests" to "Speed tests connect to Cloudflare's public speed test service (speed.cloudflare.com), which sees your public IP address like any website. Results are saved only on your device.",
    "Ads" to "The free version shows ads from Google AdMob. AdMob may use device identifiers and collect data to show and measure ads, as described in Google's privacy policy. Where required, we ask for your consent and you can change it any time in Settings > Ad privacy choices.",
    "Purchases" to "Subscriptions are processed by Google Play. We never see your payment details.",
    "Data usage" to "If you grant usage access, the app reads data usage statistics to show them to you. They never leave your device.",
    "Children" to "This app is not directed to children under 13.",
    "Contact" to "Questions? Email ${AppConfig.SUPPORT_EMAIL}.",
)
