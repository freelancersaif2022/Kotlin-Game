package com.pulse.messenger.ui.contact

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.data.Chat
import com.pulse.messenger.data.ChatType
import com.pulse.messenger.data.ME
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.data.MessageStatus
import com.pulse.messenger.data.PulseRepository
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.chat.BubbleInfo
import com.pulse.messenger.ui.chat.MessageBubble
import com.pulse.messenger.ui.chat.senderColor
import com.pulse.messenger.ui.components.ActionSheet
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.BlockDialog
import com.pulse.messenger.ui.components.ChatWallpaper
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.DialogButton
import com.pulse.messenger.ui.components.InputDialog
import com.pulse.messenger.ui.components.ListDivider
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.MuteDialog
import com.pulse.messenger.ui.components.PulseDialog
import com.pulse.messenger.ui.components.PulseSwitch
import com.pulse.messenger.ui.components.PulseTopBar
import com.pulse.messenger.ui.components.ReportDialog
import com.pulse.messenger.ui.components.SettingsItem
import com.pulse.messenger.ui.components.SheetAction
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.onboarding.AvatarPickerSheet
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.data.MockData
import com.pulse.messenger.util.Format

private fun mediaOf(messages: List<Message>): List<Message> =
    messages.filter { it.content is MessageContent.Image || it.content is MessageContent.Video }

@Composable
private fun InfoHeaderBar(onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, PulseTheme.colors.divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(PulseTheme.colors.chip.copy(alpha = 0.6f)))
    Column { content() }
}

@Composable
private fun MediaStrip(media: List<Message>, total: Int, onAll: () -> Unit, onOpen: (Message) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onAll).padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Media, links and docs", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(total.toString(), color = PulseTheme.colors.textSecondary)
        Icon(Icons.Outlined.ChevronRight, null, tint = PulseTheme.colors.textSecondary)
    }
    if (media.isNotEmpty()) {
        LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(media.takeLast(12).asReversed(), key = { it.id }) { m ->
                val res = when (val c = m.content) {
                    is MessageContent.Image -> c.res
                    is MessageContent.Video -> c.thumb
                    else -> null
                }
                AsyncImage(
                    model = res, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)).clickable { onOpen(m) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- contact info

@Composable
fun ContactInfoScreen(nav: NavController, contactId: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    val chats by repo.chats.collectAsStateWithLifecycle()
    val messages by repo.messages.collectAsStateWithLifecycle()
    val blocked by repo.blocked.collectAsStateWithLifecycle()
    val contact = contacts.firstOrNull { it.id == contactId }
    if (contact == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val chat = chats.firstOrNull { it.type == ChatType.DIRECT && it.contactId == contactId }
    val chatMessages = chat?.let { messages[it.id] }.orEmpty()
    val media = mediaOf(chatMessages)
    val docsLinks = chatMessages.count { it.content is MessageContent.Document || it.content is MessageContent.Link }
    val starred = chatMessages.count { it.isStarred }
    val common = chats.filter { it.type == ChatType.GROUP && contactId in it.memberIds && ME in it.memberIds }
    val isBlocked = contactId in blocked
    var menu by remember { mutableStateOf(false) }
    var blockDialog by remember { mutableStateOf(false) }
    var reportDialog by remember { mutableStateOf(false) }
    var muteDialog by remember { mutableStateOf(false) }
    var photoOpen by remember { mutableStateOf(false) }

    fun chatId() = chat?.id ?: repo.directChatFor(contactId).id

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            InfoHeaderBar(onBack = { nav.back() }) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                        DropdownMenuItem(text = { Text("Share") }, onClick = {
                            menu = false
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${contact.name}\n${contact.phone}")
                            }
                            context.startActivity(Intent.createChooser(send, "Share contact"))
                        })
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; snackbar.show("Contact edited in your address book", SnackType.INFO) })
                        DropdownMenuItem(text = { Text("Verify security code") }, onClick = { menu = false; nav.navigate(Routes.qr(0)) })
                    }
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(contact.avatar, contact.name, 124.dp, Modifier.clip(CircleShape).clickable { if (contact.avatar != null) photoOpen = true })
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contact.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp))
                    if (contact.isBusiness) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Verified, "Verified business", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(contact.phone, style = MaterialTheme.typography.bodyLarge, color = PulseTheme.colors.textSecondary)
                if (contact.isBusiness) {
                    Text("Business account", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(Icons.Outlined.Call, "Audio", Modifier.weight(1f)) {
                        if (isBlocked) snackbar.show("Unblock ${contact.name} to place a call", SnackType.ERROR)
                        else nav.navigate(Routes.call(contactId, false))
                    }
                    ActionTile(Icons.Outlined.Videocam, "Video", Modifier.weight(1f)) {
                        if (isBlocked) snackbar.show("Unblock ${contact.name} to place a call", SnackType.ERROR)
                        else nav.navigate(Routes.call(contactId, true))
                    }
                    ActionTile(Icons.Outlined.Search, "Search", Modifier.weight(1f)) { nav.navigate(Routes.chat(chatId())) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        item {
            SectionCard {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(contact.about, style = MaterialTheme.typography.bodyLarge)
                    if (contact.aboutUpdated > 0) {
                        Text(Format.date(contact.aboutUpdated), style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                    }
                }
            }
        }
        item {
            SectionCard {
                MediaStrip(media, media.size + docsLinks, onAll = { chat?.let { nav.navigate(Routes.chatMedia(it.id)) } }) {
                    chat?.let { c -> nav.navigate(Routes.viewer(c.id, it.id)) }
                }
            }
        }
        item {
            SectionCard {
                SettingsItem("Starred messages", icon = Icons.Outlined.Star, trailing = { Text(if (starred > 0) "$starred" else "None", color = PulseTheme.colors.textSecondary) }) { nav.navigate(Routes.STARRED) }
                SettingsItem(
                    "Notifications", icon = Icons.Outlined.Notifications,
                    trailing = { PulseSwitch(chat?.isMuted != true) { on -> if (on) repo.setMuted(listOf(chatId()), false) else muteDialog = true } },
                ) { if (chat?.isMuted == true) repo.setMuted(listOf(chatId()), false) else muteDialog = true }
                SettingsItem("Media visibility", subtitle = "Show newly downloaded media in your device's gallery", icon = Icons.Outlined.Image) {
                    snackbar.show("Media visibility set to default", SnackType.INFO)
                }
                SettingsItem("Wallpaper", icon = Icons.Outlined.Wallpaper) { nav.navigate(Routes.wallpaper(chatId())) }
            }
        }
        item {
            SectionCard {
                SettingsItem("Encryption", subtitle = "Messages and calls are end-to-end encrypted. Tap to verify.", icon = Icons.Outlined.Lock) { nav.navigate(Routes.qr(0)) }
                SettingsItem(
                    "Disappearing messages",
                    subtitle = PulseRepository.disappearingLabel(chat?.disappearingSeconds ?: 0L),
                    icon = Icons.Outlined.Timer,
                ) { nav.navigate(Routes.disappearing(chatId())) }
                SettingsItem(
                    "Chat lock", subtitle = "Lock and hide this chat on this device.", icon = Icons.Outlined.Lock,
                    trailing = { PulseSwitch(chat?.isLocked == true) { repo.setLocked(chatId(), it); snackbar.show(if (it) "Chat locked" else "Chat unlocked", SnackType.SUCCESS) } },
                ) {
                    val locked = chat?.isLocked == true
                    repo.setLocked(chatId(), !locked)
                    snackbar.show(if (!locked) "Chat locked" else "Chat unlocked", SnackType.SUCCESS)
                }
            }
        }
        if (common.isNotEmpty()) {
            item {
                SectionCard {
                    Text(
                        "${common.size} group${if (common.size > 1) "s" else ""} in common",
                        style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                    )
                }
            }
            items(common, key = { it.id }) { g ->
                Row(
                    Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(g.id)) }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(g.avatar, g.title, 44.dp, fallback = AvatarFallback.GROUP)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(g.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            g.memberIds.joinToString(", ") { repo.displayName(it).substringBefore(" ") },
                            style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        item {
            SectionCard {
                val fav = chat?.isFavorite == true
                SettingsItem(
                    if (fav) "Remove from Favourites" else "Add to Favourites",
                    icon = if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    iconTint = MaterialTheme.colorScheme.onBackground,
                ) {
                    repo.setFavorite(listOf(chatId()), !fav)
                    snackbar.show(if (fav) "Removed from Favourites" else "Added to Favourites", SnackType.SUCCESS)
                }
                SettingsItem(
                    if (isBlocked) "Unblock ${contact.name}" else "Block ${contact.name}",
                    icon = Icons.Outlined.Block, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger,
                ) {
                    if (isBlocked) {
                        repo.setBlocked(contactId, false); snackbar.show("${contact.name} unblocked", SnackType.SUCCESS)
                    } else blockDialog = true
                }
                SettingsItem("Report ${contact.name}", icon = Icons.Outlined.ThumbDown, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger) {
                    reportDialog = true
                }
            }
        }
    }

    if (photoOpen && contact.avatar != null) {
        FullPhotoDialog(contact.avatar, contact.name) { photoOpen = false }
    }
    if (blockDialog) {
        BlockDialog(contact.name, onBlock = { repo.setBlocked(contactId, true); snackbar.show("${contact.name} blocked", SnackType.SUCCESS) }, onDismiss = { blockDialog = false })
    }
    if (reportDialog) {
        ReportDialog(contact.name, isGroup = false, onReport = { also ->
            if (also) {
                repo.setBlocked(contactId, true)
                chat?.let { repo.deleteChats(listOf(it.id)) }
                nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = false } }
            }
            snackbar.show("Report sent", SnackType.SUCCESS)
        }, onDismiss = { reportDialog = false })
    }
    if (muteDialog) {
        MuteDialog(onMute = { repo.setMuted(listOf(chatId()), true); snackbar.show("Muted for ${it.lowercase()}", SnackType.SUCCESS) }, onDismiss = { muteDialog = false })
    }
}

@Composable
fun FullPhotoDialog(res: Int, name: String, onDismiss: () -> Unit) {
    PulseDialog(onDismiss = onDismiss, title = name) {
        AsyncImage(
            model = res, contentDescription = name, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)),
        )
    }
}

// ---------------------------------------------------------------- group info

@Composable
fun GroupInfoScreen(nav: NavController, chatId: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val chats by repo.chats.collectAsStateWithLifecycle()
    val messages by repo.messages.collectAsStateWithLifecycle()
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    val settings by LocalSettingsRepo.current.settings.collectAsStateWithLifecycle()
    val chat = chats.firstOrNull { it.id == chatId }
    if (chat == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val isBroadcast = chat.type == ChatType.BROADCAST
    val chatMessages = messages[chatId].orEmpty()
    val media = mediaOf(chatMessages)
    val docsLinks = chatMessages.count { it.content is MessageContent.Document || it.content is MessageContent.Link }
    val amAdmin = ME in chat.adminIds
    val isMember = ME in chat.memberIds
    val community = repo.community(chat.communityId)
    var editName by remember { mutableStateOf(false) }
    var editDesc by remember { mutableStateOf(false) }
    var iconPicker by remember { mutableStateOf(false) }
    var memberSheet by remember { mutableStateOf<String?>(null) }
    var exitDialog by remember { mutableStateOf(false) }
    var reportDialog by remember { mutableStateOf(false) }
    var muteDialog by remember { mutableStateOf(false) }
    var inviteDialog by remember { mutableStateOf(false) }
    val members = remember(chat.memberIds, contacts) {
        chat.memberIds.sortedWith(compareBy<String> { it != ME }.thenBy { it !in chat.adminIds }.thenBy { repo.displayName(it) })
    }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            InfoHeaderBar(onBack = { nav.back() }) {
                if (amAdmin || isBroadcast) {
                    IconButton(onClick = { editName = true }) { Icon(Icons.Outlined.Edit, "Edit name") }
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(
                    chat.avatar, chat.title, 124.dp,
                    Modifier.clip(CircleShape).clickable { if (amAdmin) iconPicker = true },
                    fallback = if (isBroadcast) AvatarFallback.BROADCAST else AvatarFallback.GROUP,
                )
                Spacer(Modifier.height(14.dp))
                Text(chat.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isBroadcast) "Broadcast list · ${chat.memberIds.size - 1} recipients" else "Group · ${chat.memberIds.size} members",
                    style = MaterialTheme.typography.bodyLarge, color = PulseTheme.colors.textSecondary,
                )
                if (community != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Part of ${community.name}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.navigate(Routes.community(community.id)) }.padding(4.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (!isBroadcast) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(Icons.Outlined.Call, "Audio", Modifier.weight(1f)) { nav.navigate(Routes.call(chatId, false)) }
                        ActionTile(Icons.Outlined.Videocam, "Video", Modifier.weight(1f)) { nav.navigate(Routes.call(chatId, true)) }
                        if (amAdmin) ActionTile(Icons.Outlined.PersonAdd, "Add", Modifier.weight(1f)) { nav.navigate(Routes.addMembers(chatId)) }
                        ActionTile(Icons.Outlined.Search, "Search", Modifier.weight(1f)) { nav.back() }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        if (!isBroadcast) {
            item {
                SectionCard {
                    Column(
                        Modifier.fillMaxWidth().clickable(enabled = amAdmin) { editDesc = true }.padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        if (chat.description.isBlank()) {
                            Text("Add group description", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text(chat.description, style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Created by ${repo.displayName(chat.adminIds.firstOrNull() ?: ME)}" + if (chat.createdAt > 0) ", ${Format.date(chat.createdAt)}" else "",
                            style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
        item {
            SectionCard {
                MediaStrip(media, media.size + docsLinks, onAll = { nav.navigate(Routes.chatMedia(chatId)) }) { nav.navigate(Routes.viewer(chatId, it.id)) }
            }
        }
        if (!isBroadcast) {
            item {
                SectionCard {
                    SettingsItem(
                        "Notifications", icon = Icons.Outlined.Notifications,
                        trailing = { PulseSwitch(!chat.isMuted) { on -> if (on) repo.setMuted(listOf(chatId), false) else muteDialog = true } },
                    ) { if (chat.isMuted) repo.setMuted(listOf(chatId), false) else muteDialog = true }
                    SettingsItem("Wallpaper", icon = Icons.Outlined.Wallpaper) { nav.navigate(Routes.wallpaper(chatId)) }
                    SettingsItem("Disappearing messages", subtitle = PulseRepository.disappearingLabel(chat.disappearingSeconds), icon = Icons.Outlined.Timer) {
                        nav.navigate(Routes.disappearing(chatId))
                    }
                    SettingsItem("Encryption", subtitle = "Messages and calls are end-to-end encrypted. Tap to learn more.", icon = Icons.Outlined.Lock) {
                        snackbar.show("Only people in this group can read its messages", SnackType.INFO)
                    }
                    if (amAdmin) {
                        SettingsItem("Group permissions", icon = Icons.Outlined.AdminPanelSettings) { snackbar.show("All members can send messages and edit group settings", SnackType.INFO) }
                    }
                }
            }
        }
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isBroadcast) "${chat.memberIds.size - 1} recipients" else "${chat.memberIds.size} members",
                        style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.Search, null, tint = PulseTheme.colors.textSecondary)
                }
                if (amAdmin || isBroadcast) {
                    MemberActionRow(Icons.Outlined.PersonAdd, if (isBroadcast) "Edit recipients" else "Add members") { nav.navigate(Routes.addMembers(chatId)) }
                }
                if (amAdmin && !isBroadcast) {
                    MemberActionRow(Icons.Outlined.Link, "Invite via link") { inviteDialog = true }
                }
            }
        }
        items(members.filter { !isBroadcast || it != ME }, key = { it }) { id ->
            val c = repo.contact(id)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = id != ME) { memberSheet = id }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(if (id == ME) settings.avatar else c?.avatar, if (id == ME) settings.name else c?.name.orEmpty(), 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (id == ME) "You" else c?.name ?: id, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (id == ME) settings.about else c?.about.orEmpty(),
                        style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (id in chat.adminIds && !isBroadcast) {
                    Text(
                        "Admin", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        item {
            SectionCard {
                if (!isBroadcast) {
                    val fav = chat.isFavorite
                    SettingsItem(if (fav) "Remove from Favourites" else "Add to Favourites", icon = if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, iconTint = MaterialTheme.colorScheme.onBackground) {
                        repo.setFavorite(listOf(chatId), !fav)
                        snackbar.show(if (fav) "Removed from Favourites" else "Added to Favourites", SnackType.SUCCESS)
                    }
                }
                if (isMember && !isBroadcast) {
                    SettingsItem("Exit group", icon = Icons.AutoMirrored.Outlined.ExitToApp, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger) { exitDialog = true }
                }
                if (isBroadcast) {
                    SettingsItem("Delete broadcast list", icon = Icons.AutoMirrored.Outlined.ExitToApp, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger) {
                        val removed = repo.deleteChats(listOf(chatId))
                        snackbar.undo("Broadcast list deleted") { repo.restoreChats(removed) }
                        nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
                    }
                } else {
                    SettingsItem("Report group", icon = Icons.Outlined.Report, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger) { reportDialog = true }
                }
            }
        }
    }

    if (editName) {
        InputDialog(
            title = if (isBroadcast) "Broadcast list name" else "Enter group name", initial = chat.title, maxLength = 100,
            onConfirm = { repo.updateGroup(chatId, title = it); snackbar.show("Name updated", SnackType.SUCCESS) },
            onDismiss = { editName = false },
        )
    }
    if (editDesc) {
        InputDialog(
            title = "Group description", initial = chat.description, maxLength = 512, singleLine = false, allowEmpty = true,
            placeholder = "Add group description",
            onConfirm = { repo.updateGroup(chatId, description = it); snackbar.show("Description updated", SnackType.SUCCESS) },
            onDismiss = { editDesc = false },
        )
    }
    if (iconPicker) {
        AvatarPickerSheet(
            onPick = { repo.updateGroup(chatId, avatar = it); snackbar.show("Group icon updated", SnackType.SUCCESS) },
            onRemove = null,
            onDismiss = { iconPicker = false },
            options = MockData.groupAvatars + MockData.photos.take(12),
            title = "Group icon",
        )
    }
    memberSheet?.let { id ->
        val name = repo.displayName(id)
        ActionSheet(
            onDismiss = { memberSheet = null },
            title = name,
            actions = buildList {
                add(SheetAction("Message $name", Icons.Outlined.Edit) { nav.navigate(Routes.chat(repo.directChatFor(id).id)) })
                add(SheetAction("View $name", Icons.Outlined.Visibility) { nav.navigate(Routes.contact(id)) })
                add(SheetAction("Voice call", Icons.Outlined.Call) { nav.navigate(Routes.call(id, false)) })
                add(SheetAction("Video call", Icons.Outlined.Videocam) { nav.navigate(Routes.call(id, true)) })
                if (amAdmin && !isBroadcast) {
                    val isAdmin = id in chat.adminIds
                    add(SheetAction(if (isAdmin) "Dismiss as admin" else "Make group admin", Icons.Outlined.AdminPanelSettings) {
                        repo.toggleAdmin(chatId, id)
                        snackbar.show(if (isAdmin) "$name is no longer an admin" else "$name is now an admin", SnackType.SUCCESS)
                    })
                }
                if (amAdmin || isBroadcast) {
                    add(SheetAction("Remove $name", Icons.Outlined.Block, destructive = true) {
                        repo.removeMember(chatId, id)
                        snackbar.show("$name removed", SnackType.SUCCESS)
                    })
                }
            },
        )
    }
    if (exitDialog) {
        ConfirmDialog(
            title = "Exit \"${chat.title}\"?", message = "Only group admins will be notified that you left the group.",
            confirmText = "Exit group", destructive = true, icon = Icons.AutoMirrored.Outlined.ExitToApp,
            onConfirm = { repo.exitGroup(chatId); snackbar.show("You left ${chat.title}", SnackType.SUCCESS) },
            onDismiss = { exitDialog = false },
        )
    }
    if (reportDialog) {
        ReportDialog(chat.title, isGroup = true, onReport = { also ->
            if (also) {
                repo.exitGroup(chatId); repo.deleteChats(listOf(chatId))
                nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
            }
            snackbar.show("Report sent", SnackType.SUCCESS)
        }, onDismiss = { reportDialog = false })
    }
    if (muteDialog) {
        MuteDialog(onMute = { repo.setMuted(listOf(chatId), true); snackbar.show("Muted for ${it.lowercase()}", SnackType.SUCCESS) }, onDismiss = { muteDialog = false })
    }
    if (inviteDialog) {
        val link = remember { "https://chat.pulse.app/" + (1..22).map { "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".random() }.joinToString("") }
        PulseDialog(
            onDismiss = { inviteDialog = false },
            icon = Icons.Outlined.Link,
            title = "Invite to group via link",
            message = "Anyone with Pulse can follow this link to join this group. Only share it with people you trust.\n\n$link",
            buttons = listOf(
                DialogButton("Copy link") {
                    context.getSystemService(android.content.ClipboardManager::class.java)
                        ?.setPrimaryClip(android.content.ClipData.newPlainText("Group link", link))
                    snackbar.show("Link copied", SnackType.SUCCESS); inviteDialog = false
                },
                DialogButton("Share") {
                    inviteDialog = false
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Join my Pulse group \"${chat.title}\": $link")
                    }, "Share link"))
                },
            ),
        )
    }
}

@Composable
private fun MemberActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---------------------------------------------------------------- message info

@Composable
fun MessageInfoScreen(nav: NavController, chatId: String, messageId: String) {
    val repo = LocalRepo.current
    val messages by repo.messages.collectAsStateWithLifecycle()
    val settings by LocalSettingsRepo.current.settings.collectAsStateWithLifecycle()
    val m = messages[chatId]?.firstOrNull { it.id == messageId }
    val chat: Chat? = repo.chat(chatId)
    if (m == null || chat == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar("Message info", onBack = { nav.back() }) }
        Box(Modifier.fillMaxWidth().height(220.dp)) {
            ChatWallpaper(chat.wallpaper, 0f) {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterEnd) {
                    MessageBubble(
                        info = BubbleInfo(m, null, null, "You", senderColor(ME), settings.avatar, isFirst = true, isLast = true, isGroup = false),
                        maxWidth = 300.dp, highlight = null,
                        onClickContent = {}, onLongClick = {}, onReplyClick = {}, onReactionsClick = {}, onVote = {}, onContactMessage = {},
                        selectionMode = false,
                    )
                }
            }
        }
        val others = chat.memberIds.filter { it != ME }
        val read = m.status == MessageStatus.READ
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            item {
                InfoStatusHeader(Icons.Outlined.DoneAll, PulseTheme.colors.readTick, if (chat.type == ChatType.GROUP) "Read by" else "Read")
            }
            if (read) {
                items(if (chat.type == ChatType.GROUP) others.take(3) else others.take(1)) { id ->
                    InfoPersonRow(repo, id, Format.relative(m.timestamp + 90_000))
                }
            } else {
                item { Text("—", color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            }
            item { ListDivider() }
            item { InfoStatusHeader(Icons.Outlined.DoneAll, PulseTheme.colors.textSecondary, if (chat.type == ChatType.GROUP) "Delivered to" else "Delivered") }
            if (m.status >= MessageStatus.DELIVERED) {
                items(if (chat.type == ChatType.GROUP) others else others.take(1)) { id ->
                    InfoPersonRow(repo, id, Format.relative(m.timestamp + 3_000))
                }
            }
            item { ListDivider() }
            item { InfoStatusHeader(Icons.Outlined.Done, PulseTheme.colors.textSecondary, "Sent") }
            item { Text(Format.relative(m.timestamp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = PulseTheme.colors.textSecondary) }
        }
    }
}

@Composable
private fun InfoStatusHeader(icon: ImageVector, tint: Color, label: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun InfoPersonRow(repo: PulseRepository, id: String, time: String) {
    val c = repo.contact(id)
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(c?.avatar, c?.name.orEmpty(), 40.dp)
        Spacer(Modifier.width(14.dp))
        Text(c?.name ?: id, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
    }
}
