package com.pulse.messenger.ui.settings

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.pulse.messenger.R
import com.pulse.messenger.data.MockData
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.ChoiceDialog
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.ListDivider
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.MultiChoiceDialog
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseTopBar
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.components.SettingsItem
import com.pulse.messenger.ui.components.SettingsSwitch
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay

private val tones = listOf("Pulse Chime", "Pulse Ripple", "Aurora", "Bamboo", "Crystal", "Echo", "Glass", "Silent")
private val vibrations = listOf("Off", "Default", "Short", "Long")
private val ringtones = listOf("Pulse Ring", "Horizon", "Daybreak", "Lullaby", "Old Phone", "Silent")

@Composable
fun NotificationsScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }
    var reset by remember { mutableStateOf(false) }
    SettingsPage("Notifications", nav, actions = {
        IconButton(onClick = { reset = true }) { Icon(Icons.Outlined.Refresh, "Reset notification settings") }
    }) {
        item {
            SettingsSwitch("Conversation tones", s.conversationTones, { v -> settingsRepo.update { it.copy(conversationTones = v) } }, subtitle = "Play sounds for incoming and outgoing messages.")
            ListDivider()
        }
        item { SectionHeader("Messages") }
        item {
            SettingsSwitch("Show notifications", s.msgNotifications, { v -> settingsRepo.update { it.copy(msgNotifications = v) } })
            SettingsItem("Notification tone", subtitle = s.msgTone) { dialog = "msgTone" }
            SettingsItem("Vibrate", subtitle = vibrations[s.vibrate]) { dialog = "vibrate" }
            SettingsSwitch("Popup notification", s.popupNotification, { v -> settingsRepo.update { it.copy(popupNotification = v) } }, subtitle = "Show notifications as a popup on your screen")
            SettingsSwitch("Use high priority notifications", s.highPriority, { v -> settingsRepo.update { it.copy(highPriority = v) } }, subtitle = "Show previews of notifications at the top of the screen")
            SettingsSwitch("Reaction notifications", s.reactionNotifications, { v -> settingsRepo.update { it.copy(reactionNotifications = v) } }, subtitle = "Show notifications for reactions to messages you send")
            ListDivider()
        }
        item { SectionHeader("Groups") }
        item {
            SettingsSwitch("Show notifications", s.groupNotifications, { v -> settingsRepo.update { it.copy(groupNotifications = v) } })
            SettingsItem("Notification tone", subtitle = s.groupTone) { dialog = "groupTone" }
            ListDivider()
        }
        item { SectionHeader("Calls") }
        item {
            SettingsItem("Ringtone", subtitle = s.callRingtone) { dialog = "ring" }
            SettingsItem("Vibrate", subtitle = vibrations[s.callVibrate]) { dialog = "callVibrate" }
        }
    }
    when (dialog) {
        "msgTone" -> ChoiceDialog("Notification tone", tones, s.msgTone, { it }, onSelect = { v -> settingsRepo.update { it.copy(msgTone = v) } }, onDismiss = { dialog = null }, confirmMode = true)
        "groupTone" -> ChoiceDialog("Notification tone", tones, s.groupTone, { it }, onSelect = { v -> settingsRepo.update { it.copy(groupTone = v) } }, onDismiss = { dialog = null }, confirmMode = true)
        "ring" -> ChoiceDialog("Ringtone", ringtones, s.callRingtone, { it }, onSelect = { v -> settingsRepo.update { it.copy(callRingtone = v) } }, onDismiss = { dialog = null }, confirmMode = true)
        "vibrate" -> ChoiceDialog("Vibrate", vibrations.indices.toList(), s.vibrate, { vibrations[it] }, onSelect = { v -> settingsRepo.update { it.copy(vibrate = v) } }, onDismiss = { dialog = null })
        "callVibrate" -> ChoiceDialog("Vibrate", vibrations.indices.toList(), s.callVibrate, { vibrations[it] }, onSelect = { v -> settingsRepo.update { it.copy(callVibrate = v) } }, onDismiss = { dialog = null })
    }
    if (reset) {
        ConfirmDialog(
            "Reset notification settings?", "Reset all notification settings, including custom notification settings for your chats.", "Reset",
            destructive = true,
            onConfirm = {
                settingsRepo.update {
                    it.copy(conversationTones = true, msgNotifications = true, msgTone = tones[0], vibrate = 1, popupNotification = false,
                        highPriority = true, reactionNotifications = true, groupNotifications = true, groupTone = tones[1],
                        callRingtone = ringtones[0], callVibrate = 1)
                }
                snackbar.show("Notification settings reset", SnackType.SUCCESS)
            },
            onDismiss = { reset = false },
        )
    }
}

private val mediaTypes = listOf("Photos", "Audio", "Videos", "Documents")

@Composable
fun StorageScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }
    SettingsPage("Storage and data", nav) {
        item {
            SettingsItem("Manage storage", subtitle = "1.8 GB", icon = Icons.Outlined.Storage) { nav.navigate(Routes.MANAGE_STORAGE) }
            ListDivider()
            SettingsItem("Network usage", subtitle = "412.6 MB sent · 2.1 GB received", icon = Icons.Outlined.NetworkCheck) { dialog = "network" }
            SettingsSwitch("Use less data for calls", s.lessDataCalls, { v -> settingsRepo.update { it.copy(lessDataCalls = v) } })
            ListDivider()
        }
        item { SectionHeader("Media auto-download") }
        item {
            Hint("Voice messages are always automatically downloaded")
            SettingsItem("When using mobile data", subtitle = s.autoDownloadMobile.ifEmpty { setOf("No media") }.joinToString(", ")) { dialog = "mobile" }
            SettingsItem("When connected on Wi-Fi", subtitle = s.autoDownloadWifi.ifEmpty { setOf("No media") }.joinToString(", ")) { dialog = "wifi" }
            SettingsItem("When roaming", subtitle = "No media") { snackbar.show("Auto-download is disabled while roaming", SnackType.INFO) }
            ListDivider()
        }
        item { SectionHeader("Media upload quality") }
        item {
            SettingsItem("Photo upload quality", subtitle = if (s.uploadQuality == 0) "Standard quality" else "HD quality") { dialog = "quality" }
        }
    }
    when (dialog) {
        "mobile" -> MultiChoiceDialog("When using mobile data", mediaTypes, s.autoDownloadMobile, onConfirm = { v -> settingsRepo.update { it.copy(autoDownloadMobile = v) } }, onDismiss = { dialog = null })
        "wifi" -> MultiChoiceDialog("When connected on Wi-Fi", mediaTypes, s.autoDownloadWifi, onConfirm = { v -> settingsRepo.update { it.copy(autoDownloadWifi = v) } }, onDismiss = { dialog = null })
        "quality" -> ChoiceDialog("Photo upload quality", listOf(0, 1), s.uploadQuality, { if (it == 0) "Standard quality — faster to send" else "HD quality — up to 6x larger" },
            onSelect = { v -> settingsRepo.update { it.copy(uploadQuality = v) } }, onDismiss = { dialog = null }, confirmMode = true)
        "network" -> com.pulse.messenger.ui.components.PulseDialog(
            onDismiss = { dialog = null }, title = "Network usage", icon = Icons.Outlined.NetworkCheck,
            message = "Calls: 186 MB sent · 412 MB received\nMessages: 2,318 sent · 6,904 received\nMedia: 214 MB sent · 1.6 GB received\nStatus: 12 MB sent · 88 MB received\n\nUsage since ${Format.date(System.currentTimeMillis() - 60L * 86_400_000L)}",
            buttons = listOf(com.pulse.messenger.ui.components.DialogButton("Reset statistics") { dialog = null; snackbar.show("Statistics reset", SnackType.SUCCESS) },
                com.pulse.messenger.ui.components.DialogButton("Done") { dialog = null }),
        )
    }
}

@Composable
fun ManageStorageScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val chats by repo.chats.collectAsStateWithLifecycle()
    var freed by remember { mutableStateOf(0) }
    var confirmLarge by remember { mutableStateOf(false) }
    val used = 1840 - freed
    SettingsPage("Manage storage", nav) {
        item {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${"%.1f".format(used / 1000f)} GB", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("used", color = PulseTheme.colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Text("38.2 GB free", style = MaterialTheme.typography.titleSmall, color = PulseTheme.colors.textSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(PulseTheme.colors.chip)) {
                    Box(Modifier.fillMaxHeight().weight(used.toFloat().coerceAtLeast(1f)).background(MaterialTheme.colorScheme.primary))
                    Box(Modifier.fillMaxHeight().weight(900f).background(PulseTheme.colors.textSecondary.copy(alpha = 0.4f)))
                    Box(Modifier.fillMaxHeight().weight(38200f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Legend(MaterialTheme.colorScheme.primary, "Pulse")
                    Legend(PulseTheme.colors.textSecondary.copy(alpha = 0.4f), "Apps and other items")
                    Legend(PulseTheme.colors.chip, "Free")
                }
            }
            ListDivider()
        }
        item { SectionHeader("Review and delete items") }
        item {
            Row(Modifier.fillMaxWidth().clickable { confirmLarge = true }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Larger than 5 MB", style = MaterialTheme.typography.titleSmall)
                    Text(if (freed > 0) "0 B" else "412 MB", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                }
                if (freed == 0) {
                    Row {
                        MockData.videoThumbs.take(3).forEach {
                            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.padding(start = 4.dp).size(44.dp).clip(RoundedCornerShape(8.dp)))
                        }
                    }
                }
            }
            ListDivider()
        }
        item { SectionHeader("Chats") }
        items(chats.sortedByDescending { repo.messagesOf(it.id).size }.take(12), key = { it.id }) { c ->
            val mb = (repo.messagesOf(c.id).size * 13.7 + (c.id.hashCode() and 0x3f)).toInt()
            Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chatMedia(c.id)) }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(c.avatar, c.title, 44.dp)
                Spacer(Modifier.width(14.dp))
                Text(c.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("$mb MB", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary)
            }
        }
    }
    if (confirmLarge) {
        ConfirmDialog(
            "Delete large items?", "3 videos larger than 5 MB will be deleted from this device. They'll stay in the chat as previews.", "Delete items",
            destructive = true, onConfirm = { freed = 412; snackbar.show("412 MB freed", SnackType.SUCCESS) }, onDismiss = { confirmLarge = false },
        )
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
    }
}

@Composable
fun HelpScreen(nav: NavController) {
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    var contact by remember { mutableStateOf(false) }
    SettingsPage("Help", nav) {
        item {
            SettingsItem("Help center", subtitle = "Get help, contact us", icon = Icons.AutoMirrored.Outlined.HelpOutline) { snackbar.show("Opening help center…", SnackType.INFO) }
            SettingsItem("Contact us", subtitle = "Questions? Need help?", icon = Icons.Outlined.ContactSupport) { contact = true }
            SettingsItem("Terms and Privacy Policy", icon = Icons.Outlined.Description) { snackbar.show("Opening Terms and Privacy Policy…", SnackType.INFO) }
            SettingsItem("Channel reports", icon = Icons.Outlined.Policy) { snackbar.show("You haven't reported any channels", SnackType.INFO) }
            SettingsItem("App info", icon = Icons.Outlined.Info) { nav.navigate(Routes.ABOUT_APP) }
        }
    }
    if (contact) {
        var text by remember { mutableStateOf("") }
        com.pulse.messenger.ui.components.InputDialog(
            "Contact us", text, maxLength = 500, singleLine = false, placeholder = "Tell us how we can help", confirmText = "Send",
            onConfirm = { msg ->
                runCatching {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"; putExtra(Intent.EXTRA_EMAIL, arrayOf("support@pulse.app"))
                        putExtra(Intent.EXTRA_SUBJECT, "Pulse support"); putExtra(Intent.EXTRA_TEXT, msg)
                    }, "Send email"))
                }
                snackbar.show("Thanks! We'll get back to you soon", SnackType.SUCCESS)
            },
            onDismiss = { contact = false },
        )
    }
}

@Composable
fun AboutAppScreen(nav: NavController) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar("", onBack = { nav.back() }) }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.logo_full_color), null, Modifier.size(120.dp))
            Spacer(Modifier.height(12.dp))
            Text("Pulse Messenger", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Version 1.0.0", color = PulseTheme.colors.textSecondary)
            Spacer(Modifier.height(24.dp))
            Text("© 2026 Pulse Labs", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
            Text("Made with care for fast, private conversations.", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center)
        }
        Text("Licenses", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp))
    }
}

@Composable
fun LinkedDevicesScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val devices by repo.linkedDevices.collectAsStateWithLifecycle()
    var scanning by remember { mutableStateOf(false) }
    var unlink by remember { mutableStateOf<String?>(null) }
    var unlinkAll by remember { mutableStateOf(false) }
    if (scanning) {
        ScannerView(
            title = "Scan QR code", hint = "Point your phone at the QR code on pulse.app/web or the desktop app",
            onClose = { scanning = false },
            onFound = {
                val d = repo.linkDevice()
                scanning = false
                snackbar.show("${d.name} linked", SnackType.SUCCESS)
            },
        )
        return
    }
    SettingsPage("Linked devices", nav) {
        item {
            Illustration(R.drawable.illus_linked_devices, 200)
            Text("Use Pulse on other devices", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                PrimaryButton("Link a device", onClick = { scanning = true }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            ListDivider()
        }
        if (devices.isNotEmpty()) {
            item { SectionHeader("Device status") }
            item { Hint("Tap a device to log out.") }
        }
        items(devices, key = { it.id }) { d ->
            Row(Modifier.fillMaxWidth().clickable { unlink = d.id }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(
                        when { d.name.contains("iPad") -> Icons.Outlined.Tablet; d.platform == "desktop" -> Icons.Outlined.Laptop; else -> Icons.Outlined.Computer },
                        null, tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(d.name, style = MaterialTheme.typography.bodyLarge)
                    Text("Last active ${Format.relative(d.lastActive).replaceFirstChar { it.lowercase() }}", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                }
            }
        }
        if (devices.size > 1) {
            item { SettingsItem("Log out from all devices", titleColor = PulseTheme.colors.danger) { unlinkAll = true } }
        }
        item { Hint("Your personal messages are end-to-end encrypted on all your devices.") }
    }
    unlink?.let { id ->
        val d = devices.firstOrNull { it.id == id }
        ConfirmDialog(
            "Log out of ${d?.name.orEmpty()}?", "This device will no longer be able to send or receive messages.", "Log out", destructive = true,
            onConfirm = { repo.unlinkDevice(id); snackbar.show("Logged out of ${d?.name.orEmpty()}", SnackType.SUCCESS) },
            onDismiss = { unlink = null },
        )
    }
    if (unlinkAll) {
        ConfirmDialog(
            "Log out from all devices?", "All linked devices will be logged out.", "Log out", destructive = true,
            onConfirm = { repo.unlinkAll(); snackbar.show("Logged out from all devices", SnackType.SUCCESS) },
            onDismiss = { unlinkAll = false },
        )
    }
}

/** Mock QR scanner: dark viewfinder with animated scan line that "finds" a code after a moment. */
@Composable
fun ScannerView(title: String, hint: String, onClose: () -> Unit, onFound: () -> Unit) {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "y")
    LaunchedEffect(Unit) { delay(2800); onFound() }
    androidx.activity.compose.BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(model = R.drawable.wp_photo_03, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White) }
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(260.dp).border(3.dp, Color.White, RoundedCornerShape(24.dp))) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp)
                        .offset { IntOffset(0, (y * 250.dp.toPx()).toInt()) }
                        .background(com.pulse.messenger.ui.theme.Mint),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(hint, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun QrScreen(nav: NavController, initialTab: Int) {
    val settingsRepo = LocalSettingsRepo.current
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(initialTab) }
    var resetDialog by remember { mutableStateOf(false) }
    var seed by remember { mutableIntStateOf(0) }
    val payload = "https://pulse.app/qr/" + (s.phone.filter { it.isDigit() }.ifEmpty { "14155550100" } + seed).hashCode().toUInt().toString(36).uppercase()
    val matrix = remember(payload) {
        QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0))
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) {
            PulseTopBar("QR code", onBack = { nav.back() }, actions = {
                if (tab == 0) {
                    IconButton(onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Chat with me on Pulse: $payload")
                        }, "Share QR link"))
                    }) { Icon(Icons.Filled.Share, "Share") }
                    IconButton(onClick = { resetDialog = true }) { Icon(Icons.Outlined.Refresh, "Reset QR code") }
                }
            })
        }
        TabRow(
            selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background,
            indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = MaterialTheme.colorScheme.primary, height = 3.dp) },
        ) {
            listOf("My code", "Scan code").forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = MaterialTheme.colorScheme.primary, unselectedContentColor = PulseTheme.colors.textSecondary)
            }
        }
        if (tab == 0) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(28.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(model = R.drawable.illus_qr_frame, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(s.avatar, s.name, 56.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(s.name.ifBlank { "You" }, color = Color(0xFF111B21), style = MaterialTheme.typography.titleMedium)
                        Text("Pulse contact", color = Color(0xFF667781), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                            val teal = com.pulse.messenger.ui.theme.TealDark
                            Canvas(Modifier.fillMaxSize()) {
                                val n = matrix.width
                                val cell = size.width / n
                                for (x in 0 until n) for (yy in 0 until n) {
                                    if (matrix.get(x, yy)) drawRect(teal, Offset(x * cell, yy * cell), Size(cell + 0.5f, cell + 0.5f))
                                }
                            }
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(4.dp)) {
                                Image(painterResource(R.drawable.logo_full_color), null, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Your QR code is private. If you share it with someone, they can scan it with their Pulse camera to add you as a contact.",
                    textAlign = TextAlign.Center, color = PulseTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            ScannerView(
                "Scan code", "Scan a Pulse QR code to add a contact",
                onClose = { tab = 0 },
                onFound = {
                    val c = repo.contacts.value.filter { !it.isBusiness }.random()
                    snackbar.show("Found ${c.name} on Pulse", SnackType.SUCCESS)
                    nav.navigate(Routes.chat(repo.directChatFor(c.id).id)) { popUpTo(Routes.HOME) }
                },
            )
        }
    }
    if (resetDialog) {
        ConfirmDialog(
            "Reset QR code?", "If you reset your QR code, your existing QR code and link will no longer work.", "Reset", destructive = true,
            onConfirm = { seed++; snackbar.show("QR code reset", SnackType.SUCCESS) }, onDismiss = { resetDialog = false },
        )
    }
}
