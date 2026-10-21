package com.netscope.wifianalyzer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.BuildConfig
import com.netscope.wifianalyzer.data.SpeedUnit
import com.netscope.wifianalyzer.data.ThemeMode
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.LocalPremiumActions
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.NavRow
import com.netscope.wifianalyzer.ui.components.ScreenScaffold
import com.netscope.wifianalyzer.ui.components.SectionTitle
import com.netscope.wifianalyzer.ui.components.SegmentedTabs
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.components.rememberPremiumState
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.navigation.navigateTab
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette

@Composable
fun SettingsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val actions = LocalPremiumActions.current
    val premium by rememberPremiumState()
    val theme by AppGraph.prefs.theme.collectAsState()
    val unit by AppGraph.prefs.unit.collectAsState()
    val sound by AppGraph.prefs.soundGuide.collectAsState()
    val intruder by AppGraph.prefs.intruderEnabled.collectAsState()
    val privacyRequired by AppGraph.ads.privacyOptionsRequired.collectAsState()

    ScreenScaffold(title = "Settings", onBack = null, topLevel = true) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            PremiumStatusCard(premium.subscribed, premium.rewardActive, premium.rewardRemainingMs, premium.rewardAdsWatched) {
                actions.openRewardUnlock()
            }
            if (!premium.subscribed) {
                Spacer(Modifier.height(10.dp))
                AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    NavRow(Icons.Rounded.ManageAccounts, Palette.Gold, "Go Premium", "Remove ads and unlock everything", pro = true) {
                        actions.openPaywall()
                    }
                }
            }

            SectionTitle("Appearance")
            AppCard(Modifier.fillMaxWidth()) {
                Text("Theme", style = MaterialTheme.typography.labelLarge, color = AppColors.ext.muted)
                Spacer(Modifier.height(8.dp))
                SegmentedTabs(listOf("System", "Light", "Dark"), theme.ordinal, onSelect = { AppGraph.prefs.setTheme(ThemeMode.entries[it]) })
                Spacer(Modifier.height(16.dp))
                Text("Speed unit", style = MaterialTheme.typography.labelLarge, color = AppColors.ext.muted)
                Spacer(Modifier.height(8.dp))
                SegmentedTabs(SpeedUnit.entries.map { it.label }, unit.ordinal, onSelect = { AppGraph.prefs.setUnit(SpeedUnit.entries[it]) })
            }

            SectionTitle("Features")
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                NavRow(
                    Icons.Rounded.Security, Palette.Rose, "Intruder alert",
                    if (intruder) "On • checks every 15 minutes" else "Off", pro = true,
                ) { nav.navigate(Routes.INTRUDER) }
                NavRow(
                    Icons.Rounded.VolumeUp, Palette.Mint, "Signal sound guide", "Beeps in the signal meter",
                    trailing = { Switch(checked = sound, onCheckedChange = { AppGraph.prefs.setSoundGuide(it) }) },
                ) { AppGraph.prefs.setSoundGuide(!sound) }
                NavRow(Icons.Rounded.Widgets, Palette.Cyan, "Home screen widgets", "Signal & speed at a glance", pro = true) {
                    nav.navigate(Routes.WIDGETS)
                }
                NavRow(Icons.Rounded.Tune, Palette.Orange, "All tools", null) { nav.navigateTab(Routes.TOOLS) }
            }

            SectionTitle("Purchases")
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                NavRow(Icons.Rounded.Restore, Palette.Blue, "Restore purchases", "Already subscribed on this Google account?") {
                    AppGraph.billing.refreshPurchases(reportRestore = true)
                    snackbar.show("Checking Google Play…", SnackType.Info)
                }
                NavRow(Icons.Rounded.ManageAccounts, Palette.Lilac, "Manage subscription", "Change plan or cancel in Google Play") {
                    openSubscriptions(context)
                }
            }

            SectionTitle("Help & support")
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                NavRow(Icons.Rounded.HelpOutline, Palette.Cyan, "Help & guide", "How every tool works, FAQ and tips") { nav.navigate(Routes.HELP) }
                NavRow(Icons.Rounded.School, Palette.Mint, "Replay tutorial", "Show the home screen tour again") {
                    AppGraph.prefs.homeTourDone = false
                    nav.navigateTab(Routes.HOME)
                }
                NavRow(Icons.Rounded.Speed, Palette.Sky, "Show intro again", "The welcome screens") {
                    nav.navigate(Routes.ONBOARDING)
                }
                NavRow(Icons.Rounded.Email, Palette.Blue, "Contact support", AppConfig.SUPPORT_EMAIL) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppConfig.SUPPORT_EMAIL}"))
                        .putExtra(Intent.EXTRA_SUBJECT, "WiFi Analyzer support (v${BuildConfig.VERSION_NAME})")
                    runCatching { context.startActivity(intent) }.onFailure { snackbar.show("No email app found", SnackType.Error) }
                }
                NavRow(Icons.Rounded.StarRate, Palette.Gold, "Rate the app", "It really helps us") { openPlayStore(context) }
                NavRow(Icons.Rounded.Share, Palette.Violet, "Share with friends", null) {
                    shareText(context, "Check your Wi-Fi speed and see who's on your network with WiFi Analyzer: https://play.google.com/store/apps/details?id=${context.packageName}")
                }
            }

            SectionTitle("Privacy")
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                if (privacyRequired) {
                    NavRow(Icons.Rounded.DarkMode, Palette.Sky, "Ad privacy choices", "Change your consent for personalized ads") {
                        context.findActivity()?.let { a ->
                            AppGraph.ads.showPrivacyOptions(a) { err -> if (err != null) snackbar.show(err, SnackType.Error) }
                        }
                    }
                }
                NavRow(Icons.Rounded.PrivacyTip, Palette.Mint, "Privacy policy", null) { nav.navigate(Routes.PRIVACY) }
                NavRow(Icons.Rounded.Description, Palette.Blue, "Terms of use", null) { openUrl(context, AppConfig.TERMS_URL) }
                NavRow(Icons.Rounded.Info, Palette.Lilac, "Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") {}
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Made with care for faster, safer Wi-Fi",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.ext.subtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
