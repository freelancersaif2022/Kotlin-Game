package com.pdfmaster.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.BuildConfig
import com.pdfmaster.app.R
import com.pdfmaster.app.data.prefs.ThemeMode
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.AppDialog
import com.pdfmaster.app.ui.components.IconCircle
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.SegmentedControl
import com.pdfmaster.app.ui.components.bounceClick
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.premium.PassActiveCard
import com.pdfmaster.app.ui.premium.rememberPremiumState
import com.pdfmaster.app.ui.premium.rememberRewardLauncher
import com.pdfmaster.app.ui.rememberActivity
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import com.pdfmaster.app.ui.theme.ToolColors
import com.pdfmaster.app.util.FileActions
import com.pdfmaster.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PRIVACY_URL = "https://example.com/pdfmaster/privacy"
const val TERMS_URL = "https://example.com/pdfmaster/terms"
const val SUPPORT_EMAIL = "support@example.com"

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val activity = rememberActivity()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val premium = rememberPremiumState()
    val reward = rememberRewardLauncher()
    val theme by deps.prefs.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val keepOn by deps.prefs.keepScreenOn.collectAsStateWithLifecycle(true)
    val night by deps.prefs.nightReading.collectAsStateWithLifecycle(false)
    var cacheSize by remember { mutableLongStateOf(0L) }
    var confirmClearCache by remember { mutableStateOf(false) }
    var confirmClearRecent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cacheSize = withContext(Dispatchers.IO) { cacheBytes(context) } }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 16.dp, bottom = 12.dp),
            )
        }
        item {
            when {
                premium.purchased -> MemberCard()
                premium.passActive -> PassActiveCard(premium, Modifier.padding(horizontal = 20.dp), onExtend = { reward.launch() })
                else -> UpgradeBanner { nav.premium("settings") }
            }
        }
        item {
            Section("Appearance") {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(if (theme == ThemeMode.DARK) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, ToolColors.Indigo, size = 40.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(14.dp))
                        Text("Theme", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    SegmentedControl(
                        listOf("System", "Light", "Dark"),
                        selected = theme.ordinal,
                        onSelect = { scope.launch { deps.prefs.setTheme(ThemeMode.entries[it]) } },
                    )
                }
            }
        }
        item {
            Section("Reading") {
                SwitchRow(Icons.Rounded.StayCurrentPortrait, ToolColors.Teal, "Keep screen on", "While a document is open", keepOn) {
                    scope.launch { deps.prefs.setKeepScreenOn(it) }
                }
                Divider()
                SwitchRow(Icons.Rounded.Nightlight, ToolColors.Violet, "Night reading", "Open documents with inverted colours", night) {
                    scope.launch { deps.prefs.setNightReading(it) }
                }
            }
        }
        item {
            Section("Premium") {
                if (!premium.purchased) {
                    LinkRow(Icons.Rounded.PlayCircle, Brand.GoldDeep, "Watch ad for 5 hours of Pro", if (premium.passActive) "Add 5 more hours" else "All tools unlocked, no ads") { reward.launch() }
                    Divider()
                }
                LinkRow(Icons.Rounded.Restore, ToolColors.Green, "Restore purchases", "Already bought Premium? Restore it here") { deps.billing.restore() }
                Divider()
                LinkRow(Icons.Rounded.ManageAccounts, ToolColors.Blue, "Manage subscription", "Change or cancel in Google Play") {
                    FileActions.openUrl(context, "https://play.google.com/store/account/subscriptions?package=${context.packageName}")
                }
            }
        }
        item {
            Section("Storage") {
                LinkRow(Icons.Rounded.CleaningServices, ToolColors.Orange, "Clear cache", Format.size(cacheSize)) { confirmClearCache = true }
                Divider()
                LinkRow(Icons.Rounded.AutoDelete, ToolColors.Slate, "Clear recent files", "Your files are not deleted") { confirmClearRecent = true }
            }
        }
        item {
            Section("Support") {
                LinkRow(Icons.Rounded.Star, Brand.Gold, "Rate PDF Master", "Love the app? Leave us 5 stars") {
                    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    runCatching { context.startActivity(market) }.onFailure {
                        FileActions.openUrl(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                }
                Divider()
                LinkRow(Icons.Rounded.Share, ToolColors.Blue, "Share with friends", null) {
                    FileActions.shareText(context, "I use PDF Master to read, edit and sign PDFs. Try it: https://play.google.com/store/apps/details?id=${context.packageName}")
                }
                Divider()
                LinkRow(Icons.Rounded.Email, ToolColors.Red, "Contact support", SUPPORT_EMAIL) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "PDF Master ${BuildConfig.VERSION_NAME} feedback")
                    }
                    runCatching { context.startActivity(intent) }.onFailure { snackbar.error("No email app found on this device.") }
                }
                if (deps.ads.privacyOptionsRequired) {
                    Divider()
                    LinkRow(Icons.Rounded.Shield, ToolColors.Teal, "Privacy choices", "Manage ad consent") {
                        deps.ads.showPrivacyOptions(activity) { err -> if (err != null) snackbar.error(err) }
                    }
                }
                Divider()
                LinkRow(Icons.Rounded.PrivacyTip, ToolColors.Indigo, "Privacy policy", null) { FileActions.openUrl(context, PRIVACY_URL) }
                Divider()
                LinkRow(Icons.Rounded.Description, ToolColors.Slate, "Terms of use", null) { FileActions.openUrl(context, TERMS_URL) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.img_logo_wordmark), null, Modifier.height(28.dp))
                Spacer(Modifier.height(6.dp))
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (confirmClearCache) {
        AppDialog(
            title = "Clear cache?",
            message = "Temporary files and thumbnails will be removed. Your documents stay safe.",
            confirmText = "Clear",
            onDismiss = { confirmClearCache = false },
            onConfirm = {
                confirmClearCache = false
                scope.launch {
                    withContext(Dispatchers.IO) { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                    cacheSize = 0
                    snackbar.success("Cache cleared")
                }
            },
        )
    }
    if (confirmClearRecent) {
        AppDialog(
            title = "Clear recent files?",
            message = "Your recent list will be emptied. The files themselves won't be deleted.",
            confirmText = "Clear list",
            onDismiss = { confirmClearRecent = false },
            onConfirm = {
                confirmClearRecent = false
                scope.launch {
                    deps.files.clearRecent()
                    snackbar.success("Recent files cleared")
                }
            },
        )
    }
}

private fun cacheBytes(context: android.content.Context): Long =
    context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

@Composable
private fun MemberCard() {
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2A1A0A), Color(0xFF1B1D29)))).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.img_premium_badge), null, Modifier.size(54.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Premium member", color = Brand.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Every tool unlocked · No ads · Thank you!", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UpgradeBanner(onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brand.RedGradient)
            .bounceClick(scaleDown = 0.98f, onClick = onClick).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Upgrade to Premium", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(2.dp))
                Text("Unlock OCR, password tools, watermarks and remove all ads.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Brand.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("See plans", color = Brand.Red, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            Image(painterResource(R.drawable.img_premium_hero), null, Modifier.size(104.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
    )
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface).border(1.dp, AppTheme.colors.cardBorder, MaterialTheme.shapes.large),
        content = content,
    )
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun LinkRow(icon: ImageVector, tint: Color, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircle(icon, tint, size = 40.dp, iconSize = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, tint: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        IconCircle(icon, tint, size = 40.dp, iconSize = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = Color.White),
        )
    }
}
