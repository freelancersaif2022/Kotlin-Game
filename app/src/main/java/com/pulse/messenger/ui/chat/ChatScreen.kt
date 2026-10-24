package com.pulse.messenger.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pulse.messenger.ui.components.Appear
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.data.Chat
import com.pulse.messenger.data.ChatType
import com.pulse.messenger.data.Contact
import com.pulse.messenger.data.ME
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.data.MockData
import com.pulse.messenger.data.PulseRepository
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.BlockDialog
import com.pulse.messenger.ui.components.ChatWallpaper
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.DialogButton
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.MuteDialog
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseBottomSheet
import com.pulse.messenger.ui.components.PulseDialog
import com.pulse.messenger.ui.components.ReportDialog
import com.pulse.messenger.ui.components.SearchTopBar
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface ChatRowItem {
    val key: String

    data class Msg(val info: BubbleInfo) : ChatRowItem {
        override val key get() = info.message.id
    }

    data class Day(val ts: Long) : ChatRowItem {
        override val key get() = "day_$ts"
    }

    data class Sys(val message: Message) : ChatRowItem {
        override val key get() = message.id
    }

    data class Unread(val count: Int) : ChatRowItem {
        override val key get() = "unread"
    }

    data object E2E : ChatRowItem {
        override val key get() = "e2e"
    }
}

private enum class ChatSheet { ATTACH, GALLERY, DOCUMENT, LOCATION, CONTACT, POLL, AUDIO }

private val quickReactions = listOf("❤️", "😂", "😮", "😢", "🙏", "👍")

private fun buildRows(
    messages: List<Message>,
    chat: Chat,
    repo: PulseRepository,
    myAvatar: Int?,
    unreadAnchor: String?,
    unreadCount: Int,
): List<ChatRowItem> {
    val rows = ArrayList<ChatRowItem>(messages.size + 16)
    rows += ChatRowItem.E2E
    val byId = messages.associateBy { it.id }
    val isGroup = chat.type == ChatType.GROUP
    for (i in messages.indices) {
        val m = messages[i]
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        if (prev == null || !Format.sameDay(prev.timestamp, m.timestamp)) rows += ChatRowItem.Day(m.timestamp)
        if (m.id == unreadAnchor) rows += ChatRowItem.Unread(unreadCount)
        if (m.content is MessageContent.System) {
            rows += ChatRowItem.Sys(m)
            continue
        }
        fun sameRun(a: Message?, b: Message): Boolean =
            a != null && a.senderId == b.senderId && a.content !is MessageContent.System &&
                Format.sameDay(a.timestamp, b.timestamp) && kotlin.math.abs(b.timestamp - a.timestamp) < 10 * 60_000 &&
                !(a.id == unreadAnchor || b.id == unreadAnchor)
        val reply = m.replyToId?.let { byId[it] }
        val senderContact = if (m.isMine) null else repo.contact(m.senderId)
        rows += ChatRowItem.Msg(
            BubbleInfo(
                message = m,
                replyTo = reply,
                replyToName = reply?.let { repo.displayName(it.senderId) },
                senderName = if (m.isMine) "You" else senderContact?.name ?: repo.displayName(m.senderId),
                senderColor = senderColor(m.senderId),
                senderAvatar = if (m.isMine) myAvatar else senderContact?.avatar,
                isFirst = !sameRun(prev, m),
                isLast = next == null || !sameRun(m, next),
                isGroup = isGroup,
            ),
        )
    }
    return rows
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(nav: NavController, chatId: String) {
    val repo = LocalRepo.current
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val chats by repo.chats.collectAsStateWithLifecycle()
    val allMessages by repo.messages.collectAsStateWithLifecycle()
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    val blocked by repo.blocked.collectAsStateWithLifecycle()
    val settings by settingsRepo.settings.collectAsStateWithLifecycle()

    val chat = chats.firstOrNull { it.id == chatId }
    if (chat == null) {
        LaunchedEffect(Unit) { nav.back() }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val messages = allMessages[chatId].orEmpty()
    val contact: Contact? = chat.contactId?.let { id -> contacts.firstOrNull { it.id == id } }
    val isBlocked = contact != null && contact.id in blocked
    val isMember = chat.type != ChatType.GROUP || ME in chat.memberIds

    // Unread separator is anchored to the state when the chat was opened.
    val initialUnread = rememberSaveable { chat.unreadCount }
    val unreadAnchor = remember {
        if (initialUnread <= 0) null else messages.filter { !it.isMine && it.content !is MessageContent.System }.takeLast(initialUnread).firstOrNull()?.id
    }

    var input by remember { mutableStateOf(TextFieldValue(chat.draft)) }
    var replyToId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<ChatSheet?>(null) }
    var menu by remember { mutableStateOf(false) }
    var selectionMenu by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var blockDialog by remember { mutableStateOf(false) }
    var reportDialog by remember { mutableStateOf(false) }
    var muteDialog by remember { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }
    var reactionsFor by remember { mutableStateOf<String?>(null) }
    var reactionPickerFor by remember { mutableStateOf<String?>(null) }
    var documentPreview by remember { mutableStateOf<MessageContent.Document?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var matchCursor by remember { mutableIntStateOf(0) }
    var flashId by remember { mutableStateOf<String?>(null) }
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var showGroupHint by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    val rows = remember(messages, chat.type, contacts, settings.avatar) {
        buildRows(messages, chat, repo, settings.avatar, unreadAnchor, initialUnread).asReversed()
    }
    val listState = rememberLazyListState()
    val maxBubble = (LocalConfiguration.current.screenWidthDp * 0.8f).dp

    val matches = remember(query, messages) {
        if (query.isBlank()) emptyList() else messages.filter {
            val t = when (val c = it.content) {
                is MessageContent.Text -> c.text
                is MessageContent.Image -> c.caption
                is MessageContent.Video -> c.caption
                is MessageContent.Link -> c.text
                is MessageContent.Document -> c.name
                else -> ""
            }
            t.contains(query, ignoreCase = true)
        }.map { it.id }.asReversed()
    }

    fun scrollToMessage(id: String) {
        val index = rows.indexOfFirst { it.key == id }
        if (index >= 0) scope.launch {
            listState.animateScrollToItem(index, -200)
            flashId = id
            delay(1200)
            if (flashId == id) flashId = null
        }
    }

    // Read receipts & active chat bookkeeping.
    DisposableEffect(chatId) {
        repo.activeChatId = chatId
        onDispose { if (repo.activeChatId == chatId) repo.activeChatId = null }
    }
    LaunchedEffect(messages.size) { repo.markRead(listOf(chatId)) }
    val latestInput by rememberUpdatedState(input.text)
    DisposableEffect(chatId) { onDispose { repo.setDraft(chatId, latestInput) } }
    LaunchedEffect(Unit) { delay(3000); showGroupHint = false }

    // Keep pinned to the bottom when new messages arrive, otherwise count them.
    var newWhileAway by remember { mutableIntStateOf(0) }
    val lastId = messages.lastOrNull()?.id
    LaunchedEffect(lastId) {
        if (lastId == null) return@LaunchedEffect
        if (listState.firstVisibleItemIndex <= 2 || messages.last().isMine) {
            listState.animateScrollToItem(0)
            newWhileAway = 0
        } else {
            newWhileAway++
        }
    }
    val showScrollDown by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
    LaunchedEffect(showScrollDown) { if (!showScrollDown) newWhileAway = 0 }

    LaunchedEffect(Unit) {
        if (unreadAnchor != null) {
            val idx = rows.indexOfFirst { it is ChatRowItem.Unread }
            if (idx > 0) listState.scrollToItem(idx, -400)
        }
    }

    BackHandler(panelOpen || selected.isNotEmpty() || searching) {
        when {
            selected.isNotEmpty() -> selected = emptySet()
            panelOpen -> panelOpen = false
            searching -> { searching = false; query = "" }
        }
    }

    fun toggleSelect(id: String) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selected = if (id in selected) selected - id else selected + id
    }

    fun sendText() {
        val text = input.text.trim()
        if (text.isEmpty() && editingId == null) return
        val editing = editingId
        if (editing != null) {
            if (text.isNotEmpty()) repo.edit(chatId, editing, text)
            editingId = null
            input = TextFieldValue("")
            snackbar.show("Message edited", SnackType.SUCCESS)
            return
        }
        val url = Regex("""https?://\S+""").find(text)?.value
        val content: MessageContent = if (url != null) {
            val host = Uri.parse(url).host?.removePrefix("www.") ?: url
            val previews = listOf(
                com.pulse.messenger.R.drawable.media_link_preview_01, com.pulse.messenger.R.drawable.media_link_preview_02,
                com.pulse.messenger.R.drawable.media_link_preview_03, com.pulse.messenger.R.drawable.media_link_preview_05,
                com.pulse.messenger.R.drawable.media_link_preview_06, com.pulse.messenger.R.drawable.media_link_preview_07,
            )
            MessageContent.Link(text, url, host.replaceFirstChar { it.uppercase() }, "Tap to open $host", previews[(url.hashCode() and 0x7fffffff) % previews.size])
        } else {
            MessageContent.Text(text)
        }
        repo.send(chatId, content, replyToId)
        replyToId = null
        input = TextFieldValue("")
    }

    fun sendContent(content: MessageContent) {
        repo.send(chatId, content, replyToId)
        replyToId = null
    }

    val selectedMessages = messages.filter { it.id in selected }
    val title = chat.title
    val subtitle = when {
        chat.type == ChatType.DIRECT && isBlocked -> null
        chat.typingUserId != null && chat.type == ChatType.GROUP -> "${repo.displayName(chat.typingUserId).substringBefore(" ")} is typing…"
        chat.typingUserId != null -> "typing…"
        chat.type == ChatType.DIRECT && contact?.isBusiness == true -> "Business account"
        chat.type == ChatType.DIRECT && contact?.isOnline == true -> "online"
        chat.type == ChatType.DIRECT && contact != null -> if (contact.lastSeen > 0) Format.lastSeen(contact.lastSeen) else null
        chat.type == ChatType.GROUP && showGroupHint -> "tap here for group info"
        chat.type == ChatType.GROUP -> chat.memberIds.sortedBy { it != ME }.joinToString(", ") { repo.displayName(it).substringBefore(" ") }
        chat.type == ChatType.BROADCAST -> "${chat.memberIds.size - 1} recipients"
        else -> null
    }
    val wallpaper = chat.wallpaper ?: if (PulseTheme.colors.isDark) settings.wallpaperDark else settings.wallpaperLight

    fun openInfo() {
        when (chat.type) {
            ChatType.DIRECT -> chat.contactId?.let { nav.navigate(Routes.contact(it)) }
            ChatType.GROUP -> nav.navigate(Routes.group(chat.id))
            ChatType.BROADCAST -> nav.navigate(Routes.group(chat.id))
        }
    }

    fun call(video: Boolean) {
        if (isBlocked) {
            snackbar.show("Unblock ${contact?.name} to place a call", SnackType.ERROR)
            return
        }
        nav.navigate(Routes.call(chat.contactId ?: chat.id, video))
    }

    if (chat.isLocked && !unlocked) {
        LockedChatGate(title = chat.title, onUnlock = { unlocked = true }, onBack = { nav.back() })
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ------------------------------------------------------------ top bar
        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
            Box(Modifier.statusBarsPadding()) {
                AnimatedContent(
                    when {
                        selected.isNotEmpty() -> 1
                        searching -> 2
                        else -> 0
                    },
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "chatbar",
                ) { mode ->
                    when (mode) {
                        1 -> SelectionBar(
                            count = selected.size,
                            canReply = selected.size == 1 && isMember && !isBlocked && selectedMessages.firstOrNull()?.content !is MessageContent.Deleted,
                            allStarred = selectedMessages.isNotEmpty() && selectedMessages.all { it.isStarred },
                            canCopy = selectedMessages.any { it.content is MessageContent.Text || it.content is MessageContent.Link },
                            canForward = selectedMessages.none { it.content is MessageContent.Deleted },
                            onClose = { selected = emptySet() },
                            onReply = {
                                replyToId = selected.first(); selected = emptySet()
                                panelOpen = false; focusRequester.requestFocus()
                            },
                            onStar = {
                                val star = !selectedMessages.all { it.isStarred }
                                repo.setStarred(chatId, selected, star)
                                snackbar.show(if (star) "Message starred" else "Message unstarred", SnackType.SUCCESS)
                                selected = emptySet()
                            },
                            onDelete = { deleteDialog = true },
                            onCopy = {
                                val text = selectedMessages.sortedBy { it.timestamp }.joinToString("\n") {
                                    when (val c = it.content) {
                                        is MessageContent.Text -> c.text
                                        is MessageContent.Link -> c.text
                                        else -> ""
                                    }
                                }.trim()
                                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("message", text))
                                snackbar.show(if (selected.size == 1) "Message copied" else "${selected.size} messages copied", SnackType.SUCCESS)
                                selected = emptySet()
                            },
                            onForward = {
                                val ids = selectedMessages.sortedBy { it.timestamp }.map { it.id }
                                selected = emptySet()
                                nav.navigate(Routes.forward(chatId, ids))
                            },
                            menuOpen = selectionMenu,
                            onMenu = { selectionMenu = it },
                            menuContent = {
                                val single = selectedMessages.singleOrNull()
                                if (single != null && single.isMine && single.content !is MessageContent.Deleted) {
                                    DropdownMenuItem(text = { Text("Info") }, onClick = {
                                        selectionMenu = false; selected = emptySet()
                                        nav.navigate(Routes.messageInfo(chatId, single.id))
                                    })
                                }
                                if (single != null && single.isMine && single.content is MessageContent.Text) {
                                    DropdownMenuItem(text = { Text("Edit") }, onClick = {
                                        selectionMenu = false; selected = emptySet()
                                        editingId = single.id
                                        val t = (single.content as MessageContent.Text).text
                                        input = TextFieldValue(t, androidx.compose.ui.text.TextRange(t.length))
                                        panelOpen = false
                                        focusRequester.requestFocus()
                                    })
                                }
                                if (single != null && single.content !is MessageContent.Deleted) {
                                    DropdownMenuItem(text = { Text("React") }, onClick = {
                                        selectionMenu = false; reactionPickerFor = single.id; selected = emptySet()
                                    })
                                }
                                DropdownMenuItem(text = { Text("Select all") }, onClick = {
                                    selectionMenu = false
                                    selected = messages.filter { it.content !is MessageContent.System }.map { it.id }.toSet()
                                })
                            },
                        )
                        2 -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                val fr = remember { FocusRequester() }
                                LaunchedEffect(Unit) { fr.requestFocus() }
                                SearchTopBar(
                                    query = query,
                                    onQueryChange = { query = it; matchCursor = 0; matches.firstOrNull()?.let { id -> scrollToMessage(id) } },
                                    onClose = { searching = false; query = "" },
                                    placeholder = "Search…",
                                    focusRequester = fr,
                                )
                            }
                            if (query.isNotBlank()) {
                                Text(
                                    if (matches.isEmpty()) "0/0" else "${matchCursor + 1}/${matches.size}",
                                    style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary,
                                )
                            }
                            IconButton(onClick = {
                                if (matches.isNotEmpty()) {
                                    matchCursor = (matchCursor + 1).coerceAtMost(matches.lastIndex)
                                    scrollToMessage(matches[matchCursor])
                                }
                            }) { Icon(Icons.Filled.KeyboardArrowUp, "Older match") }
                            IconButton(onClick = {
                                if (matches.isNotEmpty()) {
                                    matchCursor = (matchCursor - 1).coerceAtLeast(0)
                                    scrollToMessage(matches[matchCursor])
                                }
                            }) { Icon(Icons.Filled.KeyboardArrowDown, "Newer match") }
                        }
                        else -> Row(
                            Modifier.fillMaxWidth().height(64.dp).padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { openInfo() }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Avatar(
                                    chat.avatar, chat.title, 40.dp,
                                    fallback = when (chat.type) {
                                        ChatType.GROUP -> AvatarFallback.GROUP
                                        ChatType.BROADCAST -> AvatarFallback.BROADCAST
                                        else -> AvatarFallback.INITIALS
                                    },
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    AnimatedContent(subtitle, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "sub") { s ->
                                        if (s != null) {
                                            Text(
                                                s,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                                color = if (chat.typingUserId != null) PulseTheme.colors.unreadBadge else PulseTheme.colors.textSecondary,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                            if (chat.type != ChatType.BROADCAST && isMember) {
                                IconButton(onClick = { call(true) }) { Icon(Icons.Filled.Videocam, "Video call") }
                                IconButton(onClick = { call(false) }) { Icon(Icons.Filled.Call, "Voice call") }
                            }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More options") }
                                DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                                    @Composable
                                    fun entry(label: String, action: () -> Unit) =
                                        DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                                    when (chat.type) {
                                        ChatType.DIRECT -> entry("View contact") { openInfo() }
                                        ChatType.GROUP -> entry("Group info") { openInfo() }
                                        ChatType.BROADCAST -> entry("Broadcast list info") { openInfo() }
                                    }
                                    entry("Search") { searching = true }
                                    entry("Media, links and docs") { nav.navigate(Routes.chatMedia(chatId)) }
                                    if (chat.type != ChatType.BROADCAST) {
                                        entry(if (chat.isMuted) "Unmute notifications" else "Mute notifications") {
                                            if (chat.isMuted) {
                                                repo.setMuted(listOf(chatId), false)
                                                snackbar.show("Notifications unmuted", SnackType.SUCCESS)
                                            } else muteDialog = true
                                        }
                                        entry("Disappearing messages") { nav.navigate(Routes.disappearing(chatId)) }
                                    }
                                    entry("Wallpaper") { nav.navigate(Routes.wallpaper(chatId)) }
                                    entry(if (chat.isFavorite) "Remove from Favourites" else "Add to Favourites") {
                                        repo.setFavorite(listOf(chatId), !chat.isFavorite)
                                        snackbar.show(if (chat.isFavorite) "Removed from Favourites" else "Added to Favourites", SnackType.SUCCESS)
                                    }
                                    entry("Clear chat") { clearDialog = true }
                                    entry("Export chat") {
                                        val text = messages.joinToString("\n") {
                                            "[${Format.chatListTime(it.timestamp)} ${Format.clock(it.timestamp)}] ${repo.displayName(it.senderId)}: ${com.pulse.messenger.ui.home.previewText(it.content)}"
                                        }
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Pulse chat with ${chat.title}")
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(send, "Export chat"))
                                    }
                                    when (chat.type) {
                                        ChatType.DIRECT -> {
                                            entry(if (isBlocked) "Unblock" else "Block") {
                                                if (isBlocked) {
                                                    repo.setBlocked(contact!!.id, false)
                                                    snackbar.show("${contact.name} unblocked", SnackType.SUCCESS)
                                                } else blockDialog = true
                                            }
                                            entry("Report") { reportDialog = true }
                                        }
                                        ChatType.GROUP -> {
                                            if (isMember) entry("Exit group") { exitDialog = true }
                                            entry("Report") { reportDialog = true }
                                        }
                                        ChatType.BROADCAST -> entry("Delete broadcast list") {
                                            val removed = repo.deleteChats(listOf(chatId))
                                            snackbar.undo("Broadcast list deleted") { repo.restoreChats(removed) }
                                            nav.back()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------ messages
        ChatWallpaper(wallpaper, settings.wallpaperDim, Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
            ) {
                if (chat.typingUserId != null) {
                    item(key = "typing", contentType = "typing") {
                        TypingBubble(if (chat.type == ChatType.GROUP) repo.displayName(chat.typingUserId) else null)
                    }
                }
                items(rows, key = { it.key }, contentType = { it::class }) { row ->
                    when (row) {
                        is ChatRowItem.Day -> DayChip(Format.daySeparator(row.ts))
                        is ChatRowItem.E2E -> EncryptionBanner {
                            nav.navigate(if (chat.type == ChatType.DIRECT && chat.contactId != null) Routes.contact(chat.contactId) else Routes.group(chat.id))
                        }
                        is ChatRowItem.Unread -> Box(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color.White.copy(alpha = if (PulseTheme.colors.isDark) 0.06f else 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${row.count} unread message${if (row.count > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium, color = PulseTheme.colors.textSecondary,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                        is ChatRowItem.Sys -> {
                            val sysText = (row.message.content as MessageContent.System).text
                            SystemChip(
                            sysText,
                            icon = if (sysText.contains("disappearing")) Icons.Outlined.Timer else null,
                            modifier = Modifier.clickable(enabled = isBlocked && sysText.contains("unblock")) {
                                contact?.let { repo.setBlocked(it.id, false); snackbar.show("${it.name} unblocked", SnackType.SUCCESS) }
                            },
                        )
                        }
                        is ChatRowItem.Msg -> {
                            val m = row.info.message
                            val isSel = m.id in selected
                            val flash = flashId == m.id
                            MessageRow(
                                info = row.info,
                                selected = isSel,
                                flash = flash,
                                selectionMode = selected.isNotEmpty(),
                                swipeEnabled = isMember && !isBlocked && m.content !is MessageContent.Deleted,
                                maxBubble = maxBubble,
                                highlight = if (searching) query else null,
                                onTap = {
                                    if (selected.isNotEmpty()) toggleSelect(m.id)
                                },
                                onLongPress = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selected = selected + m.id
                                },
                                onContentClick = {
                                    if (selected.isNotEmpty()) {
                                        toggleSelect(m.id)
                                    } else {
                                        when (val c = m.content) {
                                            is MessageContent.Image, is MessageContent.Video -> nav.navigate(Routes.viewer(chatId, m.id))
                                            is MessageContent.Document -> documentPreview = c
                                            is MessageContent.Location -> runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(c.title + " " + c.address))))
                                            }.onFailure { snackbar.show("No maps app found on this device", SnackType.ERROR) }
                                            is MessageContent.Link -> runCatching { uriHandler.openUri(c.url) }
                                            is MessageContent.Sticker -> snackbar.show("Sticker from ${MockData.stickerPacks.firstOrNull { p -> c.res in p.second }?.first ?: "Pulse"}")
                                            else -> Unit
                                        }
                                    }
                                },
                                onReply = {
                                    replyToId = m.id
                                    panelOpen = false
                                    focusRequester.requestFocus()
                                    keyboard?.show()
                                },
                                onReplyClick = { scrollToMessage(it) },
                                onReactionsClick = { reactionsFor = m.id },
                                onVote = { repo.vote(chatId, m.id, it) },
                                onContactMessage = { phone ->
                                    val target = contacts.firstOrNull { it.phone == phone }
                                    if (target != null) nav.navigate(Routes.chat(repo.directChatFor(target.id).id))
                                    else snackbar.show("This contact isn't on Pulse yet", SnackType.INFO)
                                },
                            )
                        }
                    }
                }
            }

            // Quick reaction bar for a single selected message.
            val single = selectedMessages.singleOrNull()
            Appear(
                single != null && single.content !is MessageContent.Deleted,
                enter = fadeIn() + scaleIn(initialScale = 0.85f) + slideInVertically { -it / 2 },
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            ) {
                Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val mine = single?.reactions?.get(ME)
                        quickReactions.forEach { e ->
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (mine == e) PulseTheme.colors.chipSelected else Color.Transparent)
                                    .clickable {
                                        single?.let { repo.react(chatId, it.id, ME, e) }
                                        selected = emptySet()
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 26.sp) }
                        }
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(PulseTheme.colors.chip).clickable {
                                reactionPickerFor = single?.id; selected = emptySet()
                            },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Add, "More reactions", tint = PulseTheme.colors.textSecondary) }
                    }
                }
            }

            Appear(
                showScrollDown,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            ) {
                Box {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) }; newWhileAway = 0 },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = PulseTheme.colors.textSecondary,
                        shape = CircleShape,
                    ) { Icon(Icons.Filled.KeyboardDoubleArrowDown, "Scroll to bottom") }
                    if (newWhileAway > 0) {
                        com.pulse.messenger.ui.components.CountBadge(newWhileAway, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
                    }
                }
            }
        }

        // ------------------------------------------------------------ input
        Box(Modifier.background(PulseTheme.colors.chatBackground).navigationBarsPadding().then(if (panelOpen) Modifier else Modifier.imePadding())) {
            when {
                isBlocked -> BlockedFooter(contact!!.name) {
                    repo.setBlocked(contact.id, false)
                    snackbar.show("${contact.name} unblocked", SnackType.SUCCESS)
                }
                !isMember -> Text(
                    "You can't send messages to this group because you're no longer a member.",
                    textAlign = TextAlign.Center, color = PulseTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
                )
                else -> Column {
                    val replyMsg = replyToId?.let { id -> messages.firstOrNull { it.id == id } }
                    ChatInputBar(
                        value = input,
                        onValueChange = { input = it },
                        replyTo = replyMsg,
                        replyName = replyMsg?.let { repo.displayName(it.senderId) }.orEmpty(),
                        onCancelReply = { replyToId = null },
                        editing = editingId?.let { id -> messages.firstOrNull { it.id == id } },
                        onCancelEdit = { editingId = null; input = TextFieldValue("") },
                        panelOpen = panelOpen,
                        onTogglePanel = {
                            if (panelOpen) {
                                panelOpen = false
                                focusRequester.requestFocus()
                                keyboard?.show()
                            } else {
                                keyboard?.hide()
                                focusManager.clearFocus()
                                scope.launch { delay(90); panelOpen = true }
                            }
                        },
                        onFieldFocused = { panelOpen = false },
                        onSend = { sendText() },
                        onAttach = { keyboard?.hide(); sheet = ChatSheet.ATTACH },
                        onCamera = { nav.navigate(Routes.camera("chat:$chatId")) },
                        onVoice = { sec -> sendContent(MessageContent.Voice(sec, MockData.waveform(sec * 31 + messages.size))) },
                        onVoiceTooShort = { snackbar.show("Hold to record, release to send", SnackType.INFO) },
                        enterIsSend = settings.enterIsSend,
                        focusRequester = focusRequester,
                    )
                    AnimatedVisibility(panelOpen, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                        EmojiStickerPanel(
                            onEmoji = { input = input.insert(it) },
                            onSticker = { sendContent(MessageContent.Sticker(it)) },
                            onBackspace = { input = input.backspace() },
                        )
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- sheets & dialogs
    when (sheet) {
        ChatSheet.ATTACH -> AttachSheet(onDismiss = { sheet = null }) { t ->
            sheet = when (t) {
                AttachType.GALLERY -> ChatSheet.GALLERY
                AttachType.CAMERA -> { nav.navigate(Routes.camera("chat:$chatId")); null }
                AttachType.LOCATION -> ChatSheet.LOCATION
                AttachType.CONTACT -> ChatSheet.CONTACT
                AttachType.DOCUMENT -> ChatSheet.DOCUMENT
                AttachType.AUDIO -> ChatSheet.AUDIO
                AttachType.POLL -> ChatSheet.POLL
            }
        }
        ChatSheet.GALLERY -> GalleryPickerSheet(onDismiss = { sheet = null }) { items -> items.forEach { sendContent(it) } }
        ChatSheet.DOCUMENT -> DocumentPickerSheet(onDismiss = { sheet = null }) { d ->
            sendContent(MessageContent.Document(d.name, d.sizeLabel, d.pages, d.extension, d.thumb))
        }
        ChatSheet.LOCATION -> LocationSheet(onDismiss = { sheet = null }) { sendContent(it) }
        ChatSheet.CONTACT -> ContactPickerSheet(contacts.filter { !it.isBusiness }, onDismiss = { sheet = null }, onPick = { c ->
            sendContent(MessageContent.ContactCard(c.name, c.phone, c.avatar))
        })
        ChatSheet.POLL -> PollSheet(onDismiss = { sheet = null }) { sendContent(it) }
        ChatSheet.AUDIO -> AudioPickerSheet(onDismiss = { sheet = null }) { sec -> sendContent(MessageContent.Voice(sec, MockData.waveform(sec))) }
        null -> Unit
    }

    if (deleteDialog) {
        val ids = selected
        val canEveryone = selectedMessages.isNotEmpty() && selectedMessages.all { it.isMine && it.content !is MessageContent.Deleted }
        val n = ids.size
        PulseDialog(
            onDismiss = { deleteDialog = false },
            icon = Icons.Outlined.Delete,
            iconTint = PulseTheme.colors.danger,
            title = if (n == 1) "Delete message?" else "Delete $n messages?",
            stackedButtons = true,
            buttons = buildList {
                if (canEveryone) add(DialogButton("Delete for everyone", destructive = true) {
                    repo.deleteForEveryone(chatId, ids)
                    selected = emptySet(); deleteDialog = false
                    snackbar.show(if (n == 1) "Message deleted for everyone" else "$n messages deleted for everyone", SnackType.SUCCESS)
                })
                add(DialogButton("Delete for me", destructive = true) {
                    val removed = messages.filter { it.id in ids }
                    repo.deleteForMe(chatId, ids)
                    selected = emptySet(); deleteDialog = false
                    snackbar.undo(if (n == 1) "Message deleted" else "$n messages deleted") { repo.restoreMessages(chatId, removed) }
                })
                add(DialogButton("Cancel") { deleteDialog = false })
            },
        )
    }
    if (clearDialog) {
        ConfirmDialog(
            title = "Clear this chat?",
            message = "Messages will be removed from this device only.",
            confirmText = "Clear chat",
            destructive = true,
            icon = Icons.Outlined.Delete,
            checkboxLabel = "Keep starred messages",
            checkboxInitial = true,
            onConfirm = {},
            onConfirmWithCheck = { keep ->
                repo.clearChat(chatId, keep)
                snackbar.show("Chat cleared", SnackType.SUCCESS)
            },
            onDismiss = { clearDialog = false },
        )
    }
    if (blockDialog && contact != null) {
        BlockDialog(
            contact.name,
            onBlock = { repo.setBlocked(contact.id, true); snackbar.show("${contact.name} blocked", SnackType.SUCCESS) },
            onDismiss = { blockDialog = false },
            onBlockAndReport = {
                repo.setBlocked(contact.id, true)
                snackbar.show("${contact.name} reported and blocked", SnackType.SUCCESS)
            },
        )
    }
    if (reportDialog) {
        ReportDialog(
            name = chat.title,
            isGroup = chat.type == ChatType.GROUP,
            onReport = { also ->
                if (also) {
                    if (chat.type == ChatType.GROUP) repo.exitGroup(chatId) else contact?.let { repo.setBlocked(it.id, true) }
                    repo.deleteChats(listOf(chatId))
                    nav.back()
                }
                snackbar.show("Report sent. Thanks for helping keep Pulse safe.", SnackType.SUCCESS)
            },
            onDismiss = { reportDialog = false },
        )
    }
    if (muteDialog) {
        MuteDialog(
            onMute = { repo.setMuted(listOf(chatId), true); snackbar.show("Muted for ${it.lowercase()}", SnackType.SUCCESS) },
            onDismiss = { muteDialog = false },
        )
    }
    if (exitDialog) {
        ConfirmDialog(
            title = "Exit \"${chat.title}\"?",
            message = "Only group admins will be notified that you left the group.",
            confirmText = "Exit group",
            destructive = true,
            icon = Icons.Outlined.ExitToApp,
            onConfirm = { repo.exitGroup(chatId); snackbar.show("You left ${chat.title}", SnackType.SUCCESS) },
            onDismiss = { exitDialog = false },
        )
    }
    reactionsFor?.let { id ->
        val m = messages.firstOrNull { it.id == id }
        if (m == null) reactionsFor = null else ReactionsSheet(
            m, repo,
            onRemoveMine = { repo.react(chatId, id, ME, null); reactionsFor = null },
            onDismiss = { reactionsFor = null },
        )
    }
    reactionPickerFor?.let { id ->
        PulseBottomSheet(onDismiss = { reactionPickerFor = null }) {
            Text("Choose a reaction", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp))
            EmojiStickerPanel(
                onEmoji = { repo.react(chatId, id, ME, it); reactionPickerFor = null },
                onSticker = { reactionPickerFor = null; sendContent(MessageContent.Sticker(it)) },
                onBackspace = {},
            )
        }
    }
    documentPreview?.let { d ->
        PulseDialog(
            onDismiss = { documentPreview = null },
            title = d.name,
            message = listOfNotNull(if (d.pages > 0) "${d.pages} pages" else null, d.extension, d.sizeLabel).joinToString(" · "),
            buttons = listOf(
                DialogButton("Close") { documentPreview = null },
                DialogButton("Share") {
                    documentPreview = null
                    nav.navigate(Routes.forward(chatId, messages.filter { it.content == d }.map { it.id }.take(1)))
                },
            ),
        ) {
            if (d.thumb != null) {
                AsyncImage(
                    model = d.thumb, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 24.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    info: BubbleInfo,
    selected: Boolean,
    flash: Boolean,
    selectionMode: Boolean,
    swipeEnabled: Boolean,
    maxBubble: androidx.compose.ui.unit.Dp,
    highlight: String?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onContentClick: () -> Unit,
    onReply: () -> Unit,
    onReplyClick: (String) -> Unit,
    onReactionsClick: () -> Unit,
    onVote: (Int) -> Unit,
    onContactMessage: (String) -> Unit,
) {
    val m = info.message
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val offset = remember { Animatable(0f) }
    var triggered by remember { mutableStateOf(false) }
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
    val bg by animateColorAsState(
        when {
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            flash -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        label = "msgbg",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress, indication = null, interactionSource = null)
            .draggable(
                state = rememberDraggableState { delta ->
                    if (!swipeEnabled || selectionMode) return@rememberDraggableState
                    scope.launch {
                        val next = (offset.value + delta * 0.6f).coerceIn(0f, thresholdPx * 1.4f)
                        offset.snapTo(next)
                        if (!triggered && next >= thresholdPx) {
                            triggered = true
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else if (triggered && next < thresholdPx) {
                            triggered = false
                        }
                    }
                },
                orientation = Orientation.Horizontal,
                enabled = swipeEnabled && !selectionMode,
                onDragStopped = {
                    if (triggered) onReply()
                    triggered = false
                    offset.animateTo(0f, tween(220))
                },
            )
            .padding(
                start = 8.dp, end = 8.dp,
                top = if (info.isFirst) 5.dp else 1.5.dp,
                bottom = if (m.reactions.isNotEmpty()) 2.dp else 0.dp,
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(32.dp)
                .alpha((offset.value / thresholdPx).coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) },
            contentAlignment = if (m.isMine) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            MessageBubble(
                info = info,
                maxWidth = maxBubble,
                highlight = highlight,
                onClickContent = onContentClick,
                onLongClick = onLongPress,
                onReplyClick = onReplyClick,
                onReactionsClick = onReactionsClick,
                onVote = onVote,
                onContactMessage = onContactMessage,
                selectionMode = selectionMode,
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    canReply: Boolean,
    allStarred: Boolean,
    canCopy: Boolean,
    canForward: Boolean,
    onClose: () -> Unit,
    onReply: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    menuOpen: Boolean,
    onMenu: (Boolean) -> Unit,
    menuContent: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel selection") }
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 8.dp))
        if (canReply) IconButton(onClick = onReply) { Icon(Icons.AutoMirrored.Filled.Reply, "Reply") }
        IconButton(onClick = onStar) { Icon(if (allStarred) Icons.Outlined.StarOutline else Icons.Filled.Star, if (allStarred) "Unstar" else "Star") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
        if (canCopy) IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, "Copy") }
        if (canForward) IconButton(onClick = onForward) { Icon(Icons.AutoMirrored.Filled.Forward, "Forward") }
        Box {
            IconButton(onClick = { onMenu(true) }) { Icon(Icons.Filled.MoreVert, "More") }
            DropdownMenu(menuOpen, onDismissRequest = { onMenu(false) }, shape = RoundedCornerShape(16.dp)) { menuContent() }
        }
    }
}

@Composable
private fun BlockedFooter(name: String, onUnblock: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Block, null, tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("You blocked $name", color = PulseTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton("Unblock", onClick = onUnblock)
    }
}

@Composable
private fun ReactionsSheet(m: Message, repo: PulseRepository, onRemoveMine: () -> Unit, onDismiss: () -> Unit) {
    var filter by remember { mutableStateOf<String?>(null) }
    val grouped = m.reactions.values.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
    PulseBottomSheet(onDismiss) {
        Text("${m.reactions.size} reaction${if (m.reactions.size != 1) "s" else ""}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.pulse.messenger.ui.home.FilterPill("All ${m.reactions.size}", filter == null) { filter = null }
            grouped.forEach { (e, n) -> com.pulse.messenger.ui.home.FilterPill("$e $n", filter == e) { filter = e } }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = PulseTheme.colors.divider)
        m.reactions.entries.filter { filter == null || it.value == filter }.sortedBy { it.key != ME }.forEach { (user, emoji) ->
            val c = repo.contact(user)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = user == ME, onClick = onRemoveMine)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(if (user == ME) LocalSettingsRepo.current.current.avatar else c?.avatar, repo.displayName(user), 42.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(repo.displayName(user), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    if (user == ME) Text("Tap to remove", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                }
                Text(emoji, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LockedChatGate(title: String, onUnlock: () -> Unit, onBack: () -> Unit) {
    var authenticating by remember { mutableStateOf(false) }
    LaunchedEffect(authenticating) {
        if (authenticating) { delay(900); onUnlock() }
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("Chat is locked", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Text("Unlock to view your chat with $title", color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(PulseTheme.colors.chip).clickable { authenticating = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Fingerprint, "Unlock with fingerprint",
                tint = if (authenticating) PulseTheme.colors.success else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(if (authenticating) "Verifying…" else "Touch the fingerprint sensor", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
        Spacer(Modifier.weight(1.4f))
    }
}
