package com.flashshare.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.flashshare.app.util.findActivity
import com.flashshare.app.util.formatDate
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.formatSpeed
import com.flashshare.app.util.openFile
import com.flashshare.app.util.openUrl
import com.flashshare.app.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

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
    val groups = remember(shown) { shown.groupBy { dayLabel(it.time) } }

    AppScaffold("History", onBack = { nav.popBackStack() }, actions = {
        if (entries.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, "Clear history") }
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                val sentBytes = entries.filter { it.sent && it.status == "completed" }.sumOf { it.totalBytes }
                val recvBytes = entries.filter { !it.sent && it.status == "completed" }.sumOf { it.totalBytes }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Sent", sentBytes, entries.count { it.sent }, Icons.Rounded.ArrowUpward, Brand.send, Modifier.weight(1f))
                    StatCard("Received", recvBytes, entries.count { !it.sent }, Icons.Rounded.ArrowDownward, Brand.receive, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)) {
                    listOf("All", "Sent", "Received").forEachIndexed { i, label ->
                        val sel = filter == i
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(50))
                                .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { filter = i }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            if (shown.isEmpty()) {
                item { EmptyState(Icons.Rounded.History, "No transfers yet", "Everything you send or receive shows up here.") }
            }
            var index = 0
            groups.forEach { (day, list) ->
                item(key = "h:$day") {
                    Text(day, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                }
                list.forEach { e ->
                    val i = index++
                    item(key = e.id) {
                        HistoryCard(e, expanded == e.id, Modifier.staggerIn(i, filter)) { expanded = if (expanded == e.id) null else e.id }
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AppDialog(
            onDismiss = { confirmClear = false }, icon = Icons.Rounded.DeleteSweep, title = "Clear transfer history?",
            message = "This only clears the list. Files you received stay safely on your phone.",
            confirmText = "Clear history", destructive = true,
            onConfirm = { c.history.clear(); confirmClear = false; ctx.toast("History cleared") },
        )
    }
}

private fun dayLabel(ms: Long): String {
    val d = Calendar.getInstance().apply { timeInMillis = ms }
    val now = Calendar.getInstance()
    fun same(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    if (same(d, now)) return "Today"
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (same(d, now)) return "Yesterday"
    return java.text.SimpleDateFormat("EEEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date(ms))
}

@Composable
private fun StatCard(label: String, bytes: Long, count: Int, icon: ImageVector, brush: Brush, modifier: Modifier) {
    Column(modifier.shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Brand.Blue).clip(RoundedCornerShape(24.dp)).background(brush).shine(3200, 0.2f).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        CountUpText(bytes, { formatSize(it) }, MaterialTheme.typography.titleLarge, Color.White)
        Text("$count transfers", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HistoryCard(e: HistoryEntry, open: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val extra = LocalExtra.current
    AppCard(modifier.fillMaxWidth().animateContentSize(), onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AvatarImpl(e.peerAvatarId, null, 44.dp)
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(20.dp).clip(CircleShape)
                            .background(if (e.sent) Brand.send else Brand.receive).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (e.sent) Icons.AutoMirrored.Rounded.CallMade else Icons.AutoMirrored.Rounded.CallReceived, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text((if (e.sent) "Sent to " else "Received from ") + e.peerName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${e.files.size} file${if (e.files.size == 1) "" else "s"} • ${formatSize(e.totalBytes)} • ${formatDate(e.time).substringAfter(", ")}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                when (e.status) {
                    "completed" -> InfoChip(formatSpeed(e.avgSpeed), Icons.Rounded.Bolt, extra.success)
                    "cancelled" -> InfoChip("Cancelled", Icons.Rounded.Block, MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> InfoChip("Failed", Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error)
                }
            }
            if (!open) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    e.files.take(5).forEach { f ->
                        FileThumb(f.uri, Category.fromMime(f.mime, f.name), 50.dp, thumbPath = f.thumb, name = f.name, corner = 12.dp)
                    }
                    if (e.files.size > 5) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Text("+${e.files.size - 5}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            AnimatedVisibility(open) {
                Column(Modifier.padding(top = 8.dp)) {
                    e.files.take(60).forEach { f ->
                        val cat = Category.fromMime(f.mime, f.name)
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { ctx.openFile(f.uri, f.mime) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FileThumb(f.uri, cat, 44.dp, thumbPath = f.thumb, name = f.name, corner = 12.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatSize(f.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.OpenInNew, "Open", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (e.files.size > 60) Text("+ ${e.files.size - 60} more", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
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
    var clearDialog by remember { mutableStateOf(false) }
    val busy by c.billing.busy.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            c.prefs.setSaveTree(uri.toString())
            ctx.toast("Save location updated")
        }
    }

    AppScaffold("Settings", onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // Profile
            Box(
                Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Brand.Blue).clip(RoundedCornerShape(28.dp))
                    .background(Brand.main).clickable { nav.navigate(Routes.profile(true)) }.padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeAvatar(64.dp, ring = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(settings.name, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (settings.premium) "Premium member" else "Free plan", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // Premium
            Box(
                Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(26.dp), spotColor = Brand.Orange).clip(RoundedCornerShape(26.dp))
                    .background(Brand.premium).shine(2800, 0.25f).clickable { nav.navigate(Routes.PREMIUM) }.padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlossyIcon(Icons.Rounded.WorkspacePremium, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.15f))), 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (settings.premium) "Premium • Active" else "Upgrade to Premium", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (settings.premium) (Plan.entries.firstOrNull { it.productId == settings.premiumPlan }?.title ?: "Premium") + " plan • all features unlocked"
                            else "Max speed, no ads, every PRO tool",
                            color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                }
            }

            Group("Subscription")
            SettingsCard {
                SettingRow(Icons.Rounded.Restore, IconBrushes.blue, "Restore purchases", "Already subscribed on this Google account?", trailing = {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }) { scope.launch { c.billing.refreshPurchases(userInitiated = true) } }
                if (settings.premium) {
                    RowDivider()
                    SettingRow(Icons.Rounded.ManageAccounts, IconBrushes.slate, "Manage subscription", "Change or cancel in Google Play") {
                        if (c.billing.isMock) { c.billing.mockCancel(); ctx.toast("Test store: back to the Free plan") }
                        else ctx.openUrl("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}")
                    }
                }
            }

            Group("Transfer")
            SettingsCard {
                SettingRow(Icons.Rounded.Wifi, IconBrushes.cyan, "Wi-Fi band", settings.band.label) { bandDialog = true }
                RowDivider()
                SettingRow(
                    Icons.Rounded.Lock, IconBrushes.mint, "Encryption with pairing PIN",
                    if (settings.premium) "AES-256 encrypted transfers, receiver shows a PIN" else "Unlock with Premium",
                    trailing = {
                        if (!settings.premium) ProBadge(Modifier.padding(end = 8.dp))
                        Switch(settings.encryption && settings.premium, onCheckedChange = { if (!settings.premium) nav.navigate(Routes.PREMIUM) else c.prefs.setEncryption(it) })
                    },
                ) { if (!settings.premium) nav.navigate(Routes.PREMIUM) else c.prefs.setEncryption(!settings.encryption) }
                RowDivider()
                SettingRow(Icons.Rounded.FolderOpen, IconBrushes.orange, "Save location", FileSaver.describe(settings.saveTreeUri), trailing = {
                    if (settings.saveTreeUri != null) TextButton(onClick = { c.prefs.setSaveTree(null); ctx.toast("Save location reset to default") }) { Text("Reset") }
                }) { folderPicker.launch(null) }
                RowDivider()
                SettingRow(Icons.Rounded.LightMode, IconBrushes.gold, "Keep screen on", "While transferring", trailing = {
                    Switch(settings.keepScreenOn, onCheckedChange = { c.prefs.setKeepScreenOn(it) })
                }) { c.prefs.setKeepScreenOn(!settings.keepScreenOn) }
                RowDivider()
                SettingRow(Icons.Rounded.Vibration, IconBrushes.pink, "Vibration", "On connect and when a transfer completes", trailing = {
                    Switch(settings.vibrate, onCheckedChange = { c.prefs.setVibrate(it) })
                }) { c.prefs.setVibrate(!settings.vibrate) }
            }

            Group("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System").forEach { (m, label) ->
                    ThemePreview(m, label, settings.theme == m, Modifier.weight(1f)) { c.prefs.setTheme(m) }
                }
            }

            Group("Storage & privacy")
            SettingsCard {
                SettingRow(Icons.Rounded.Security, IconBrushes.violet, "App permissions", "Open Android's permission settings for FlashShare") {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                RowDivider()
                SettingRow(Icons.Rounded.History, IconBrushes.blue, "Clear transfer history", "Received files are kept") { clearDialog = true }
                RowDivider()
                SettingRow(Icons.Rounded.CleaningServices, IconBrushes.mint, "Clear app cache", "Thumbnails and temporary files") {
                    val freed = c.cleaner.clearCache(); ctx.toast("Cache cleared – ${formatSize(freed)} freed")
                }
                if (c.ads.privacyOptionsRequired()) {
                    RowDivider()
                    SettingRow(Icons.Rounded.PrivacyTip, IconBrushes.slate, "Ad privacy choices", "Change your consent") { ctx.findActivity()?.let { c.ads.showPrivacyOptions(it) } }
                }
            }

            Group("About")
            SettingsCard {
                SettingRow(Icons.Rounded.StarRate, IconBrushes.gold, "Rate FlashShare", "Enjoying it? Leave a review") { ctx.openUrl(playLink(ctx.packageName)) }
                RowDivider()
                SettingRow(Icons.Rounded.Share, IconBrushes.pink, "Share app", "Tell your friends") {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "FlashShare – ultra-fast offline file transfer: ${playLink(ctx.packageName)}"), "Share"))
                }
                RowDivider()
                SettingRow(Icons.Rounded.Info, IconBrushes.slate, "Version", "${BuildConfig.VERSION_NAME}${if (c.billing.isMock) " (test build)" else ""}") {}
            }
            Spacer(Modifier.navigationBarsPadding().height(28.dp))
        }
    }

    if (bandDialog) {
        val sup6 = c.wifi.supports6Ghz
        val sup5 = c.wifi.supports5Ghz
        AppDialog(
            onDismiss = { bandDialog = false }, icon = Icons.Rounded.Wifi, title = "Wi-Fi band",
            message = "Higher bands are much faster. FlashShare falls back automatically if a phone can't use them.",
            confirmText = "Done", onConfirm = { bandDialog = false }, dismissText = null, iconBrush = IconBrushes.cyan,
        ) {
            BandPref.entries.forEach { b ->
                val supported = when (b) { BandPref.GHZ6 -> sup6; BandPref.GHZ5 -> sup5; BandPref.AUTO -> true }
                val sel = settings.band == b
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .border(if (sel) 2.dp else 0.dp, if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable { c.prefs.setBand(b) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlossyIcon(
                        when (b) { BandPref.GHZ6 -> Icons.Rounded.RocketLaunch; BandPref.GHZ5 -> Icons.Rounded.Bolt; BandPref.AUTO -> Icons.Rounded.AutoMode },
                        when (b) { BandPref.GHZ6 -> IconBrushes.violet; BandPref.GHZ5 -> IconBrushes.blue; BandPref.AUTO -> IconBrushes.slate }, 38.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.label, style = MaterialTheme.typography.titleSmall)
                        Text(if (supported) b.detail else "Not supported by this phone – will fall back", style = MaterialTheme.typography.bodySmall,
                            color = if (supported) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                    }
                    if (sel) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    if (clearDialog) {
        AppDialog(
            onDismiss = { clearDialog = false }, icon = Icons.Rounded.DeleteSweep, title = "Clear transfer history?",
            message = "This only clears the list. Files you received stay safely on your phone.", confirmText = "Clear history", destructive = true,
            onConfirm = { c.history.clear(); clearDialog = false; ctx.toast("History cleared") },
        )
    }
}

@Composable
private fun ThemePreview(mode: ThemeMode, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = when (mode) { ThemeMode.LIGHT -> Brush.linearGradient(listOf(Color(0xFFF5F7FD), Color(0xFFF5F7FD))); ThemeMode.DARK -> Brush.linearGradient(listOf(Color(0xFF0A0E18), Color(0xFF0A0E18)))
        ThemeMode.SYSTEM -> Brush.linearGradient(0f to Color(0xFFF5F7FD), 0.5f to Color(0xFFF5F7FD), 0.5f to Color(0xFF0A0E18), 1f to Color(0xFF0A0E18)) }
    val card = if (mode == ThemeMode.DARK) Color(0xFF1C2436) else Color.White
    val sc by animateFloatAsState(if (selected) 1f else 0.95f, spring(dampingRatio = 0.5f), label = "tp")
    Column(modifier.scale(sc), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.72f).shadow(if (selected) 12.dp else 2.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)).background(bg)
                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else LocalExtra.current.cardBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick).padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(8.dp)).background(Brand.main))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(Brand.send))
                    Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(Brand.receive))
                }
                repeat(2) { Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(card)) }
            }
            if (selected) Box(Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Group(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 26.dp, bottom = 8.dp, start = 6.dp))
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 4.dp), content = content) }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 70.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SettingRow(icon: ImageVector, brush: Brush, title: String, subtitle: String?, trailing: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(icon, brush, 40.dp, corner = 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}
// endregion

// region premium
private data class Perk(val icon: ImageVector, val brush: Brush, val title: String, val text: String)

private val perks = listOf(
    Perk(Icons.Rounded.Bolt, IconBrushes.gold, "Uncapped ultra speed", "Remove the 25 MB/s cap – go as fast as your Wi-Fi chip allows"),
    Perk(Icons.Rounded.Block, IconBrushes.red, "No ads, ever", "No video ads after transfers or before tools"),
    Perk(Icons.Rounded.Groups, IconBrushes.violet, "Group sharing", "Send to up to 8 phones at once"),
    Perk(Icons.Rounded.PhonelinkSetup, IconBrushes.cyan, "Phone Clone", "Move photos, videos, apps & contacts to a new phone"),
    Perk(Icons.Rounded.RocketLaunch, IconBrushes.mint, "Cleaner & file manager", "Find duplicates, large files and junk in seconds"),
    Perk(Icons.Rounded.Lock, IconBrushes.blue, "PIN encryption", "AES-256 encrypted transfers with a pairing PIN"),
)

@Composable
fun PremiumScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s by c.prefs.state.collectAsStateWithLifecycle()
    val products by c.billing.products.collectAsStateWithLifecycle()
    val busy by c.billing.busy.collectAsStateWithLifecycle()
    var plan by remember { mutableStateOf(Plan.YEARLY) }
    val scope = rememberCoroutineScope()
    val premium = s?.premium == true

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PremiumHero(premium)
            Column(Modifier.padding(20.dp)) {
                AnimatedContent(premium, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(200)) }, label = "prem") { isPremium ->
                    if (isPremium) {
                        ActiveCard(s?.premiumPlan, onManage = {
                            if (c.billing.isMock) { c.billing.mockCancel(); ctx.toast("Test store: back to the Free plan") }
                            else ctx.openUrl("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}")
                        })
                    } else Column {
                        Text("Choose your plan", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        Plan.entries.reversed().forEach { p ->
                            // Keyed on the loaded products so real Play prices replace the placeholders.
                            key(p, products.size) {
                                PlanCard(p, c.billing.priceOf(p), c.billing.trialOf(p), selected = plan == p,
                                    monthlyPrice = c.billing.priceOf(Plan.MONTHLY), savings = c.billing.yearlySavingsPercent()) { plan = p }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        GradientButton(
                            if (busy) "Please wait…" else "Continue with ${plan.title}",
                            onClick = { ctx.findActivity()?.let { c.billing.purchase(it, plan) } },
                            brush = Brand.premium, icon = Icons.Rounded.WorkspacePremium, enabled = !busy, modifier = Modifier.fillMaxWidth().shine(2400, 0.3f), height = 58.dp,
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { scope.launch { c.billing.refreshPurchases(true) } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Icon(Icons.Rounded.Restore, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore purchases")
                        }
                        Text(
                            "Renews automatically until cancelled in Google Play. Cancel any time." + if (c.billing.isMock) "\nTest build: purchases use a local test store." else "",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(22.dp))
                        Text("Everything in Premium", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        perks.forEachIndexed { i, p ->
                            Row(Modifier.padding(vertical = 8.dp).staggerIn(i), verticalAlignment = Alignment.CenterVertically) {
                                GlossyIcon(p.icon, p.brush, 44.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.title, style = MaterialTheme.typography.titleSmall)
                                    Text(p.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.CheckCircle, null, tint = LocalExtra.current.success)
                            }
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(16.dp))
            }
        }
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(Icons.Rounded.Close, "Close", tint = Color.White)
        }
    }
}

/** Dark hero with a crown and a carousel that shows each Premium feature in turn. */
@Composable
private fun PremiumHero(premium: Boolean) {
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2200); idx = (idx + 1) % perks.size } }
    val t = rememberInfiniteTransition(label = "hero")
    val rot by t.animateFloat(-5f, 5f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
    val glow by t.animateFloat(0.9f, 1.15f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "g")
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0E1230), Color(0xFF2B1F73), Color(0xFF5B34D1))))
    ) {
        AmbientBlobs(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 40.dp, bottom = 28.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(150.dp).scale(glow).clip(CircleShape).background(Brush.radialGradient(listOf(Brand.Gold.copy(alpha = 0.35f), Color.Transparent))))
                AnimatedContent(idx, transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                }, label = "perk") { i ->
                    GlossyIcon(perks[i].icon, perks[i].brush, 96.dp, Modifier.rotate(rot), iconScale = 0.52f)
                }
                CrownBadge(40.dp, Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.height(14.dp))
            Text("FlashShare Premium", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            AnimatedContent(idx, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) }, label = "ptext") { i ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(perks[i].title, color = Brand.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(perks[i].text, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                perks.indices.forEach { i ->
                    val w by animateDpAsState(if (i == idx) 18.dp else 6.dp, label = "pd")
                    Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(if (i == idx) Brand.Gold else Color.White.copy(alpha = 0.35f)))
                }
            }
            if (premium) {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Brand.mint).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("ACTIVE", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ActiveCard(planId: String?, onManage: () -> Unit) {
    val plan = Plan.entries.firstOrNull { it.productId == planId }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Brand.Orange).clip(RoundedCornerShape(28.dp))
                .background(Brand.premium).shine(2600, 0.25f).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Verified, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Premium is active", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("${plan?.title ?: "Premium"} plan • every feature unlocked", color = Color.White.copy(alpha = 0.9f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("Max speed", Icons.Rounded.Bolt, Brand.Orange)
            InfoChip("No ads", Icons.Rounded.Block, Brand.Red)
            InfoChip("All tools", Icons.Rounded.AutoAwesome, Brand.Violet)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onManage, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Rounded.ManageAccounts, null)
            Spacer(Modifier.width(8.dp))
            Text("Manage subscription")
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, price: String, trial: String?, selected: Boolean, monthlyPrice: String, savings: Int, onClick: () -> Unit) {
    val border = if (selected) Brand.Orange else LocalExtra.current.cardBorder
    val sc by animateFloatAsState(if (selected) 1f else 0.97f, spring(dampingRatio = 0.6f), label = "pc")
    Box(Modifier.scale(sc)) {
        Row(
            Modifier.fillMaxWidth().shadow(if (selected) 14.dp else 0.dp, RoundedCornerShape(24.dp), spotColor = Brand.Orange).clip(RoundedCornerShape(24.dp))
                .background(if (selected) Brand.Orange.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (selected) 0.9f else 1f))
                .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(24.dp))
                .clickable(onClick = onClick).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Brand.Orange))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(trial ?: if (plan == Plan.YEARLY) "Best value – billed yearly" else "Billed monthly, cancel any time",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("/ ${plan.period}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (plan == Plan.YEARLY) {
                    Text("$monthlyPrice×12", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
                }
            }
        }
        if (plan == Plan.YEARLY) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-18).dp, y = (-10).dp).clip(RoundedCornerShape(8.dp)).background(Brand.premium).shine(1800)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text("SAVE $savings%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}
// endregion
