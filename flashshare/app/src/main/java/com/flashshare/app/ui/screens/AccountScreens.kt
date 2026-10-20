package com.flashshare.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.BuildConfig
import com.flashshare.app.billing.Plan
import com.flashshare.app.container
import com.flashshare.app.data.BandPref
import com.flashshare.app.data.Category
import com.flashshare.app.data.HistoryEntry
import com.flashshare.app.data.ThemeMode
import com.flashshare.app.net.FileSaver
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.findActivity
import com.flashshare.app.util.formatDate
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.formatSpeed
import com.flashshare.app.util.openFile
import com.flashshare.app.util.openUrl
import com.flashshare.app.util.toast
import kotlinx.coroutines.launch

// region history
@Composable
fun HistoryScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val entries by c.history.entries.collectAsStateWithLifecycle()
    var filter by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val shown = when (filter) { 1 -> entries.filter { it.sent }; 2 -> entries.filter { !it.sent }; else -> entries }

    AppScaffold("History", onBack = { nav.popBackStack() }, actions = {
        if (entries.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, "Clear history") }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                listOf("All", "Sent", "Received").forEachIndexed { i, label ->
                    SegmentedButton(selected = filter == i, onClick = { filter = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                }
            }
            val sentBytes = entries.filter { it.sent && it.status == "completed" }.sumOf { it.totalBytes }
            val recvBytes = entries.filter { !it.sent && it.status == "completed" }.sumOf { it.totalBytes }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Sent", formatSize(sentBytes), Icons.Rounded.ArrowUpward, Brand.send, Modifier.weight(1f))
                StatCard("Received", formatSize(recvBytes), Icons.Rounded.ArrowDownward, Brand.receive, Modifier.weight(1f))
            }
            if (shown.isEmpty()) {
                EmptyState(Icons.Rounded.History, "No transfers yet", "Everything you send or receive shows up here.")
            } else {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { e ->
                        Column(Modifier.animateContentSize()) {
                            HistoryRow(e) { expanded = if (expanded == e.id) null else e.id }
                            AnimatedVisibility(expanded == e.id) { HistoryFiles(e) }
                        }
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This only clears the list. Received files stay on your phone.") },
            confirmButton = { TextButton(onClick = { c.history.clear(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, brush: Brush, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(20.dp)).background(brush).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HistoryFiles(e: HistoryEntry) {
    val ctx = LocalContext.current
    Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)) {
        Text(
            "${formatDate(e.time)} • ${if (e.status == "completed") "avg " + formatSpeed(e.avgSpeed) else e.status}",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        e.files.take(50).forEach { f ->
            val cat = Category.fromMime(f.mime, f.name)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { ctx.openFile(f.uri, f.mime) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FileThumb(f.uri, cat, 36.dp)
                Spacer(Modifier.width(10.dp))
                Text(f.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(formatSize(f.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (e.files.size > 50) Text("+ ${e.files.size - 50} more", style = MaterialTheme.typography.labelSmall)
    }
}
// endregion

// region settings
@Composable
fun SettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s by c.prefs.state.collectAsStateWithLifecycle()
    val settings = s ?: return
    val scope = rememberCoroutineScope()
    var bandDialog by remember { mutableStateOf(false) }
    val busy by c.billing.busy.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { c.billing.messages.collect { ctx.toast(it) } }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            c.prefs.setSaveTree(uri.toString())
            ctx.toast("Received files will be saved here")
        }
    }

    AppScaffold("Settings", onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // Profile
            AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.profile(true)) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    MeAvatar(56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(settings.name, style = MaterialTheme.typography.titleMedium)
                        Text("Profile, avatar & device name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Premium
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brand.premium)
                    .clickable { nav.navigate(Routes.PREMIUM) }.padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (settings.premium) "Premium active" else "Upgrade to Premium", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (settings.premium) "Plan: " + (Plan.entries.firstOrNull { it.productId == settings.premiumPlan }?.title ?: "Premium")
                            else "Max speed, no ads, all PRO tools",
                            color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                }
            }
            SettingRow(Icons.Rounded.Restore, "Restore purchases", "Already subscribed on this Google account?", trailing = {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }) { scope.launch { c.billing.refreshPurchases(userInitiated = true) } }
            if (settings.premium) {
                SettingRow(Icons.Rounded.ManageAccounts, "Manage subscription", "Change or cancel in Google Play") {
                    if (c.billing.isMock) { c.billing.mockCancel(); ctx.toast("Test store: back to Free plan") }
                    else ctx.openUrl("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}")
                }
            }

            Group("Transfer")
            SettingRow(Icons.Rounded.Wifi, "Wi-Fi band", settings.band.label + " – " + settings.band.detail) { bandDialog = true }
            SwitchRow(
                Icons.Rounded.Lock, "Encryption with pairing PIN",
                if (settings.premium) "Receiver shows a 6-digit PIN; files are AES-256 encrypted" else "Premium feature",
                checked = settings.encryption && settings.premium, pro = !settings.premium,
            ) { on ->
                if (!settings.premium) nav.navigate(Routes.PREMIUM) else c.prefs.setEncryption(on)
            }
            SettingRow(Icons.Rounded.FolderOpen, "Save location", FileSaver.describe(settings.saveTreeUri), trailing = {
                if (settings.saveTreeUri != null) TextButton(onClick = { c.prefs.setSaveTree(null) }) { Text("Reset") }
            }) { folderPicker.launch(null) }
            SwitchRow(Icons.Rounded.LightMode, "Keep screen on while transferring", null, settings.keepScreenOn) { c.prefs.setKeepScreenOn(it) }
            SwitchRow(Icons.Rounded.Vibration, "Vibrate on connect & complete", null, settings.vibrate) { c.prefs.setVibrate(it) }

            Group("Appearance")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                val opts = listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System")
                opts.forEachIndexed { i, (m, label) ->
                    SegmentedButton(
                        selected = settings.theme == m, onClick = { c.prefs.setTheme(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                        icon = {
                            Icon(
                                when (m) { ThemeMode.LIGHT -> Icons.Rounded.LightMode; ThemeMode.DARK -> Icons.Rounded.DarkMode; ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness },
                                null, Modifier.size(18.dp),
                            )
                        },
                    ) { Text(label) }
                }
            }

            Group("Storage & privacy")
            SettingRow(Icons.Rounded.Security, "Permissions", "Review what FlashShare can access") {
                nav.navigate("perms/${PermFlow.ALL.name}")
            }
            SettingRow(Icons.Rounded.History, "Clear transfer history", "Files you received are kept") { c.history.clear(); ctx.toast("History cleared") }
            SettingRow(Icons.Rounded.CleaningServices, "Clear app cache", "Thumbnails and temporary files") {
                val freed = c.cleaner.clearCache(); ctx.toast("Freed ${formatSize(freed)}")
            }
            if (c.ads.privacyOptionsRequired()) {
                SettingRow(Icons.Rounded.PrivacyTip, "Ad privacy choices", "Change your consent") { ctx.findActivity()?.let { c.ads.showPrivacyOptions(it) } }
            }

            Group("About")
            SettingRow(Icons.Rounded.StarRate, "Rate FlashShare", "Enjoying it? Leave a review") {
                ctx.openUrl("https://play.google.com/store/apps/details?id=${ctx.packageName}")
            }
            SettingRow(Icons.Rounded.Share, "Share app", "Tell your friends") {
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "FlashShare – ultra-fast offline file transfer: https://play.google.com/store/apps/details?id=${ctx.packageName}"), "Share"))
            }
            SettingRow(Icons.Rounded.Info, "Version", "${BuildConfig.VERSION_NAME}${if (c.billing.isMock) " (test build)" else ""}") {}
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }

    if (bandDialog) {
        AlertDialog(
            onDismissRequest = { bandDialog = false },
            icon = { Icon(Icons.Rounded.Wifi, null) },
            title = { Text("Wi-Fi band") },
            text = {
                Column {
                    val sup6 = c.wifi.supports6Ghz
                    val sup5 = c.wifi.supports5Ghz
                    BandPref.entries.forEach { b ->
                        val supported = when (b) { BandPref.GHZ6 -> sup6; BandPref.GHZ5 -> sup5; BandPref.AUTO -> true }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { c.prefs.setBand(b); bandDialog = false }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.band == b, onClick = { c.prefs.setBand(b); bandDialog = false })
                            Column {
                                Text(b.label, style = MaterialTheme.typography.titleSmall)
                                Text(if (supported) b.detail else "Not supported by this phone – will fall back", style = MaterialTheme.typography.bodySmall,
                                    color = if (supported) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { bandDialog = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun Group(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp, start = 4.dp))
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String?, trailing: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, pro: Boolean = false, onChange: (Boolean) -> Unit) {
    SettingRow(icon, title, subtitle, trailing = {
        if (pro) ProBadge(Modifier.padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }) { onChange(!checked) }
}
// endregion

// region premium
@Composable
fun PremiumScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s by c.prefs.state.collectAsStateWithLifecycle()
    val products by c.billing.products.collectAsStateWithLifecycle()
    val busy by c.billing.busy.collectAsStateWithLifecycle()
    var plan by remember { mutableStateOf(Plan.YEARLY) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { c.billing.messages.collect { ctx.toast(it) } }
    val premium = s?.premium == true

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B1F4B), Color(0xFF3A2A8C), Color(0xFF6A3DFF))))
                    .statusBarsPadding().padding(top = 48.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                val t = rememberInfiniteTransition(label = "crown")
                val rot by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
                val glow by t.animateFloat(0.9f, 1.15f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "g")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(120.dp).scale(glow).clip(CircleShape).background(Brand.Gold.copy(alpha = 0.18f)))
                        Box(Modifier.size(88.dp).rotate(rot).clip(RoundedCornerShape(28.dp)).background(Brand.premium), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(52.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("FlashShare Premium", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(if (premium) "You have every feature unlocked. Thank you!" else "The fastest way to move files – no limits, no ads.",
                        color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                }
            }
            Column(Modifier.padding(20.dp)) {
                listOf(
                    Triple(Icons.Rounded.Bolt, "Uncapped ultra speed", "Remove the 25 MB/s cap – go as fast as your Wi-Fi 5/6/6E chip allows"),
                    Triple(Icons.Rounded.Block, "No ads, ever", "No video ads after transfers or before tools"),
                    Triple(Icons.Rounded.Groups, "Group sharing", "Send to up to 8 phones at once"),
                    Triple(Icons.Rounded.PhonelinkSetup, "Phone Clone", "Move photos, videos, apps & contacts to a new phone"),
                    Triple(Icons.Rounded.CleaningServices, "File manager & cleaner", "Find duplicates, large files and junk"),
                    Triple(Icons.Rounded.Lock, "PIN encryption", "AES-256 encrypted transfers with a pairing PIN"),
                ).forEach { (icon, title, text) ->
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Orange.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = Brand.Orange)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (!premium) {
                    Plan.entries.reversed().forEach { p ->
                        // Keyed on the loaded products so real Play prices replace the placeholders.
                        key(p, products.size) { PlanCard(p, c.billing.priceOf(p), c.billing.trialOf(p), selected = plan == p,
                            monthlyPrice = c.billing.priceOf(Plan.MONTHLY), savings = c.billing.yearlySavingsPercent()) { plan = p } }
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        if (busy) "Please wait…" else "Continue with ${plan.title}",
                        onClick = { ctx.findActivity()?.let { c.billing.purchase(it, plan) } },
                        brush = Brand.premium, icon = Icons.Rounded.WorkspacePremium, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { scope.launch { c.billing.refreshPurchases(true) } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Restore purchases")
                    }
                    Text(
                        "Subscriptions renew automatically until cancelled in Google Play. Cancel any time." +
                            if (c.billing.isMock) "\nTest build: purchases use a local test store." else "",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    GradientButton("Manage subscription", onClick = {
                        if (c.billing.isMock) { c.billing.mockCancel(); ctx.toast("Test store: back to Free plan") }
                        else ctx.openUrl("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}")
                    }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.navigationBarsPadding().height(16.dp))
            }
        }
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(Icons.Rounded.Close, "Close", tint = Color.White)
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, price: String, trial: String?, selected: Boolean, monthlyPrice: String, savings: Int, onClick: () -> Unit) {
    val border = if (selected) Brand.Orange else LocalExtra.current.cardBorder
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(if (selected) Brand.Orange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(22.dp))
                .clickable(onClick = onClick).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Brand.Orange))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    trial ?: if (plan == Plan.YEARLY) "Best value – billed yearly" else "Billed monthly, cancel any time",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("/ ${plan.period}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (plan == Plan.YEARLY) {
                    Text("$monthlyPrice×12", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
                }
            }
        }
        if (plan == Plan.YEARLY) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = (-10).dp).clip(RoundedCornerShape(8.dp)).background(Brand.premium)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text("SAVE $savings%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}
// endregion
