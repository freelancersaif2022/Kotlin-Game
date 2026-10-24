package com.pulse.messenger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.data.ChatType
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MockData
import com.pulse.messenger.data.ThemeMode
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.chat.BubbleInfo
import com.pulse.messenger.ui.chat.MessageBubble
import com.pulse.messenger.ui.chat.senderColor
import com.pulse.messenger.ui.components.ChatWallpaper
import com.pulse.messenger.ui.components.ChoiceDialog
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.EmptyState
import com.pulse.messenger.ui.components.ListDivider
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseTopBar
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.components.SettingsItem
import com.pulse.messenger.ui.components.SettingsSwitch
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.home.ChatRow
import com.pulse.messenger.ui.home.buildChatItems
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay

private val fontSizes = listOf("Small", "Medium", "Large")

@Composable
fun ChatsSettingsScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var themeDialog by remember { mutableStateOf(false) }
    var fontDialog by remember { mutableStateOf(false) }
    var clearAll by remember { mutableStateOf(false) }
    var archiveAll by remember { mutableStateOf(false) }
    SettingsPage("Chats", nav) {
        item { SectionHeader("Display") }
        item {
            SettingsItem("Theme", subtitle = when (s.themeMode) { ThemeMode.SYSTEM -> "System default"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" }, icon = Icons.Outlined.BrightnessMedium) { themeDialog = true }
            SettingsItem("Wallpaper", icon = Icons.Outlined.Wallpaper) { nav.navigate(Routes.wallpaper()) }
            ListDivider()
        }
        item { SectionHeader("Chat settings") }
        item {
            SettingsSwitch("Enter is send", s.enterIsSend, { v -> settingsRepo.update { it.copy(enterIsSend = v) } }, subtitle = "Enter key will send your message")
            SettingsSwitch("Media visibility", s.mediaVisibility, { v -> settingsRepo.update { it.copy(mediaVisibility = v) } }, subtitle = "Show newly downloaded media in your device's gallery")
            SettingsItem("Font size", subtitle = fontSizes[s.fontSize], icon = Icons.Outlined.TextFields) { fontDialog = true }
            SettingsSwitch("Keep chats archived", s.keepArchived, { v -> settingsRepo.update { it.copy(keepArchived = v) } }, subtitle = "Archived chats will remain archived when you receive a new message")
            ListDivider()
        }
        item {
            SettingsItem("Chat backup", icon = Icons.Outlined.CloudUpload, subtitle = if (s.lastBackup > 0) "Last backup: ${Format.relative(s.lastBackup)}" else "Never") { nav.navigate(Routes.BACKUP) }
            SettingsItem("Transfer chats", icon = Icons.Outlined.SwapHoriz, subtitle = "Move chat history to a new phone") { snackbar.show("Open Pulse on your new phone and scan the QR code", SnackType.INFO) }
            SettingsItem("Archive all chats", icon = Icons.Outlined.Archive) { archiveAll = true }
            SettingsItem("Clear all chats", icon = Icons.Outlined.DeleteSweep) { clearAll = true }
            SettingsItem("Chat history", icon = Icons.Outlined.History, subtitle = "Export, archive or delete chats") { nav.navigate(Routes.STORAGE) }
        }
    }
    if (themeDialog) {
        ChoiceDialog(
            "Choose theme", ThemeMode.entries, s.themeMode,
            { when (it) { ThemeMode.SYSTEM -> "System default"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" } },
            onSelect = { m -> settingsRepo.update { it.copy(themeMode = m) } }, onDismiss = { themeDialog = false }, confirmMode = true,
        )
    }
    if (fontDialog) {
        ChoiceDialog("Font size", fontSizes.indices.toList(), s.fontSize, { fontSizes[it] }, onSelect = { v -> settingsRepo.update { it.copy(fontSize = v) } }, onDismiss = { fontDialog = false })
    }
    if (archiveAll) {
        ConfirmDialog(
            "Archive all chats?", "You can view archived chats from the top of your chat list.", "Archive",
            icon = Icons.Outlined.Archive,
            onConfirm = {
                val ids = repo.chats.value.filter { !it.isArchived }.map { it.id }
                repo.setArchived(ids, true)
                snackbar.undo("${ids.size} chats archived") { repo.setArchived(ids, false) }
            },
            onDismiss = { archiveAll = false },
        )
    }
    if (clearAll) {
        ConfirmDialog(
            "Clear all chats?", "Messages in all chats will be removed from this device.", "Clear",
            destructive = true, icon = Icons.Outlined.DeleteSweep, checkboxLabel = "Keep starred messages", checkboxInitial = true,
            onConfirm = {},
            onConfirmWithCheck = { keep ->
                repo.chats.value.forEach { repo.clearChat(it.id, keep) }
                snackbar.show("All chats cleared", SnackType.SUCCESS)
            },
            onDismiss = { clearAll = false },
        )
    }
}

@Composable
fun WallpaperScreen(nav: NavController, chatId: String) {
    val settingsRepo = LocalSettingsRepo.current
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    val isDefault = chatId == "default"
    val dark = PulseTheme.colors.isDark
    val chat = if (isDefault) null else repo.chat(chatId)
    val current = chat?.wallpaper ?: if (dark) s.wallpaperDark else s.wallpaperLight
    var preview by remember { mutableStateOf<Int?>(null) }
    var previewing by remember { mutableStateOf(false) }
    var dim by remember { mutableFloatStateOf(s.wallpaperDim) }

    if (previewing) {
        Box(Modifier.fillMaxSize()) {
            ChatWallpaper(preview, dim) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { previewing = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        Text("Preview", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(24.dp))
                    val now = System.currentTimeMillis()
                    val m1 = Message("p1", "p", "c16", com.pulse.messenger.data.MessageContent.Text("Swipe or tap to see more wallpapers 🎨"), now - 60_000)
                    val m2 = Message("p2", "p", com.pulse.messenger.data.ME, com.pulse.messenger.data.MessageContent.Text("This one looks great! Setting it now ✨"), now)
                    listOf(m1, m2).forEach { m ->
                        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp), contentAlignment = if (m.isMine) Alignment.CenterEnd else Alignment.CenterStart) {
                            MessageBubble(
                                BubbleInfo(m, null, null, null, senderColor(m.senderId), null, isFirst = true, isLast = true, isGroup = false),
                                maxWidth = 300.dp, highlight = null, onClickContent = {}, onLongClick = {}, onReplyClick = {},
                                onReactionsClick = {}, onVote = {}, onContactMessage = {}, selectionMode = false,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)).padding(16.dp)) {
                        if (dark) {
                            Text("Wallpaper dimming", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                dim, { dim = it }, valueRange = 0f..0.6f,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                            )
                        }
                        PrimaryButton(if (isDefault) "Set wallpaper" else "Set for this chat", onClick = {
                            if (isDefault) {
                                settingsRepo.update { if (dark) it.copy(wallpaperDark = preview, wallpaperDim = dim) else it.copy(wallpaperLight = preview, wallpaperDim = dim) }
                            } else repo.setWallpaper(chatId, preview)
                            snackbar.show("Wallpaper set", SnackType.SUCCESS)
                            nav.back()
                        }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) {
            PulseTopBar(if (isDefault) "Chat wallpaper" else "Wallpaper · ${chat?.title.orEmpty()}", onBack = { nav.back() }, subtitle = if (dark) "Dark theme" else "Light theme")
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            fun header(text: String) = item(span = { GridItemSpan(maxLineSpan) }) {
                Text(text, style = MaterialTheme.typography.labelLarge, color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
            }
            header("Default")
            item {
                WallpaperTile(null, current == null) { preview = null; previewing = true }
            }
            if (!isDefault) {
                item {
                    Box(
                        Modifier.aspectRatio(0.55f).clip(RoundedCornerShape(14.dp)).background(PulseTheme.colors.chip).clickable {
                            repo.setWallpaper(chatId, null); snackbar.show("Using default wallpaper", SnackType.SUCCESS); nav.back()
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text("Reset to\ndefault", textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge) }
                }
            }
            header(if (dark) "Dark" else "Bright")
            items(if (dark) MockData.wallpapersDark else MockData.wallpapersBright) { w -> WallpaperTile(w, current == w) { preview = w; previewing = true } }
            header(if (dark) "Bright" else "Dark")
            items(if (dark) MockData.wallpapersBright else MockData.wallpapersDark) { w -> WallpaperTile(w, current == w) { preview = w; previewing = true } }
            header("Photos")
            items(MockData.wallpapersPhoto) { w -> WallpaperTile(w, current == w) { preview = w; previewing = true } }
        }
    }
}

@Composable
private fun WallpaperTile(res: Int?, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(0.55f)
            .clip(RoundedCornerShape(14.dp))
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick),
    ) {
        if (res == null) {
            ChatWallpaper(null, 0f) {}
        } else {
            AsyncImage(model = res, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

private val backupFrequencies = listOf("Never", "Daily", "Weekly", "Monthly", "Only when I tap \"Back up\"")

@Composable
fun BackupScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var freqDialog by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        if (running) {
            progress = 0f
            while (progress < 1f) { delay(50); progress = (progress + 0.02f).coerceAtMost(1f) }
            settingsRepo.update { it.copy(lastBackup = System.currentTimeMillis()) }
            running = false
            snackbar.show("Backup complete · 248 MB", SnackType.SUCCESS)
        }
    }
    SettingsPage("Chat backup", nav) {
        item {
            Illustration(R.drawable.illus_backup_cloud, 190)
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Last backup", style = MaterialTheme.typography.titleSmall)
                Text(if (s.lastBackup > 0) Format.relative(s.lastBackup) else "Never", color = PulseTheme.colors.textSecondary)
                Text("Total size: 248 MB", color = PulseTheme.colors.textSecondary)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Back up your messages and media to your cloud account. You can restore them when you reinstall Pulse. Your backup is end-to-end encrypted.",
                    style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary,
                )
                Spacer(Modifier.height(16.dp))
                if (running) {
                    LinearProgressIndicator(
                        progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary, trackColor = PulseTheme.colors.chip,
                    )
                    Text("Backing up messages… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(top = 6.dp))
                } else {
                    PrimaryButton("Back up", onClick = { running = true }, icon = Icons.Outlined.Backup)
                }
                Spacer(Modifier.height(16.dp))
            }
            ListDivider()
            SettingsItem("Back up to cloud", subtitle = backupFrequencies[s.backupFrequency]) { freqDialog = true }
            SettingsItem("Cloud account", subtitle = s.email.ifBlank { "Not connected" }) { nav.navigate(Routes.PROFILE) }
            SettingsSwitch("Include videos", s.backupIncludeVideos, { v -> settingsRepo.update { it.copy(backupIncludeVideos = v) } }, subtitle = "Videos increase backup size")
            SettingsSwitch("End-to-end encrypted backup", true, { snackbar.show("Your backup is protected with a 64-digit key", SnackType.INFO) })
        }
    }
    if (freqDialog) {
        ChoiceDialog("Back up to cloud", backupFrequencies.indices.toList(), s.backupFrequency, { backupFrequencies[it] },
            onSelect = { v -> settingsRepo.update { it.copy(backupFrequency = v) } }, onDismiss = { freqDialog = false })
    }
}

@Composable
fun ArchivedScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val chats by repo.chats.collectAsStateWithLifecycle()
    val messages by repo.messages.collectAsStateWithLifecycle()
    val blocked by repo.blocked.collectAsStateWithLifecycle()
    val items = remember(chats, messages, blocked) { buildChatItems(chats.filter { it.isArchived }, messages, blocked, repo) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar("Archived", onBack = { nav.back() }) }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            item { Hint("These chats stay archived when new messages are received. Long-press a chat to unarchive it.") }
            if (items.isEmpty()) {
                item { EmptyState(R.drawable.empty_archived, "No archived chats", "Archive chats to tidy up your chat list. They'll appear here.") }
            }
            items(items, key = { it.chat.id }) { item ->
                ChatRow(
                    item, selected = false,
                    onClick = { nav.navigate(Routes.chat(item.chat.id)) },
                    onLongClick = {
                        repo.setArchived(listOf(item.chat.id), false)
                        snackbar.undo("Chat unarchived") { repo.setArchived(listOf(item.chat.id), true) }
                    },
                    onAvatarClick = { nav.navigate(Routes.chat(item.chat.id)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
fun StarredScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val messages by repo.messages.collectAsStateWithLifecycle()
    val settings by LocalSettingsRepo.current.settings.collectAsStateWithLifecycle()
    val starred = remember(messages) { messages.values.flatten().filter { it.isStarred }.sortedByDescending { it.timestamp } }
    val maxW = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * 0.8f).dp
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) {
            PulseTopBar("Starred messages", onBack = { nav.back() }, actions = {
                if (starred.isNotEmpty()) {
                    androidx.compose.material3.TextButton(onClick = {
                        val byChat = starred.groupBy { it.chatId }
                        byChat.forEach { (chatId, list) -> repo.setStarred(chatId, list.map { it.id }, false) }
                        snackbar.undo("All messages unstarred") { byChat.forEach { (chatId, list) -> repo.setStarred(chatId, list.map { it.id }, true) } }
                    }) { Text("Unstar all") }
                }
            })
        }
        if (starred.isEmpty()) {
            EmptyState(R.drawable.empty_starred, "No starred messages", "Tap and hold on any message in any chat to star it, so you can easily find it later.")
        } else {
            ChatWallpaper(null, 0f, Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(starred, key = { it.id }) { m ->
                        val chat = repo.chat(m.chatId)
                        Column(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(m.chatId)) }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${repo.displayName(m.senderId)}  ›  ${if (chat?.type == ChatType.GROUP) chat.title else if (m.isMine) chat?.title.orEmpty() else "You"}",
                                    style = MaterialTheme.typography.labelMedium, color = PulseTheme.colors.textSecondary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                )
                                Text(Format.chatListTime(m.timestamp), style = MaterialTheme.typography.labelSmall, color = PulseTheme.colors.textSecondary)
                            }
                            Box(Modifier.fillMaxWidth(), contentAlignment = if (m.isMine) Alignment.CenterEnd else Alignment.CenterStart) {
                                MessageBubble(
                                    BubbleInfo(
                                        m, null, null, repo.displayName(m.senderId), senderColor(m.senderId),
                                        if (m.isMine) settings.avatar else repo.contact(m.senderId)?.avatar,
                                        isFirst = true, isLast = true, isGroup = chat?.type == ChatType.GROUP,
                                    ),
                                    maxWidth = maxW, highlight = null,
                                    onClickContent = { nav.navigate(Routes.chat(m.chatId)) },
                                    onLongClick = {
                                        repo.setStarred(m.chatId, listOf(m.id), false)
                                        snackbar.undo("Message unstarred") { repo.setStarred(m.chatId, listOf(m.id), true) }
                                    },
                                    onReplyClick = {}, onReactionsClick = {}, onVote = {}, onContactMessage = {}, selectionMode = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
