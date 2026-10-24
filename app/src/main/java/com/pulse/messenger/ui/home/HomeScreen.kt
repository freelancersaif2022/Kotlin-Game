package com.pulse.messenger.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pulse.messenger.data.CallLog
import com.pulse.messenger.data.ChatType
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.DialogButton
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.MuteDialog
import com.pulse.messenger.ui.components.PulseDialog
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.theme.PulseTheme

private data class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("Chats", Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat),
    Tab("Updates", Icons.Outlined.DonutLarge, Icons.Filled.DonutLarge),
    Tab("Communities", Icons.Outlined.Groups, Icons.Filled.Groups),
    Tab("Calls", Icons.Outlined.Call, Icons.Filled.Call),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val repo = LocalRepo.current
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current

    val settings by settingsRepo.settings.collectAsStateWithLifecycle()
    val chats by repo.chats.collectAsStateWithLifecycle()
    val messages by repo.messages.collectAsStateWithLifecycle()
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    val blocked by repo.blocked.collectAsStateWithLifecycle()
    val statuses by repo.statuses.collectAsStateWithLifecycle()
    val myStatus by repo.myStatus.collectAsStateWithLifecycle()
    val channels by repo.channels.collectAsStateWithLifecycle()
    val communities by repo.communities.collectAsStateWithLifecycle()
    val calls by repo.calls.collectAsStateWithLifecycle()
    val favoriteCalls by repo.favoriteCalls.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(ChatFilter.ALL) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var selectedCalls by remember { mutableStateOf(emptySet<String>()) }
    var menu by remember { mutableStateOf(false) }
    var selectionMenu by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var muteDialog by remember { mutableStateOf(false) }
    var clearCallsDialog by remember { mutableStateOf(false) }
    var callLinkDialog by remember { mutableStateOf(false) }

    val contactMap = remember(contacts) { contacts.associateBy { it.id } }
    val chatMap = remember(chats) { chats.associateBy { it.id } }
    val allItems = remember(chats, messages, blocked) { buildChatItems(chats, messages, blocked, repo) }
    val visibleItems = remember(allItems, filter) {
        allItems.filter { !it.chat.isArchived }
            .filter {
                when (filter) {
                    ChatFilter.ALL -> true
                    ChatFilter.UNREAD -> it.hasUnread
                    ChatFilter.FAVOURITES -> it.chat.isFavorite
                    ChatFilter.GROUPS -> it.chat.type == ChatType.GROUP
                }
            }
    }
    val archived = remember(allItems) { allItems.filter { it.chat.isArchived } }
    val unreadChats = remember(allItems) { allItems.count { !it.chat.isArchived && it.hasUnread } }
    val unseenStatus = statuses.any { !it.allViewed && !it.isMuted }
    val channelUnread = channels.any { it.isFollowing && it.unread > 0 }
    val missedCalls = remember(calls) { calls.take(5).count { it.direction == com.pulse.messenger.data.CallDirection.MISSED } }

    val chatsState = rememberLazyListState()
    val updatesState = rememberLazyListState()
    val communitiesState = rememberLazyListState()
    val callsState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val selectionMode = selected.isNotEmpty() && tab == 0
    val callSelection = selectedCalls.isNotEmpty() && tab == 3
    BackHandler(selectionMode) { selected = emptySet() }
    BackHandler(callSelection) { selectedCalls = emptySet() }
    BackHandler(!selectionMode && !callSelection && tab != 0) { tab = 0 }

    fun toggle(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun openChat(chatId: String) = nav.navigate(Routes.chat(chatId))

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedContent(
                targetState = when {
                    selectionMode -> 1
                    callSelection -> 2
                    else -> 0
                },
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "topbar",
            ) { mode ->
                when (mode) {
                    1 -> {
                        val selChats = chats.filter { it.id in selected }
                        val allPinned = selChats.all { it.isPinned }
                        val allMuted = selChats.all { it.isMuted }
                        TopAppBar(
                            title = { Text(selected.size.toString(), style = MaterialTheme.typography.titleLarge) },
                            navigationIcon = {
                                IconButton(onClick = { selected = emptySet() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel selection") }
                            },
                            actions = {
                                IconButton(onClick = {
                                    if (!repo.setPinned(selected, !allPinned)) {
                                        snackbar.show("You can only pin up to 3 chats", SnackType.ERROR)
                                    } else {
                                        snackbar.show(if (allPinned) "Chat unpinned" else "Chat pinned", SnackType.SUCCESS)
                                    }
                                    selected = emptySet()
                                }) {
                                    Icon(Icons.Filled.PushPin, if (allPinned) "Unpin" else "Pin", modifier = Modifier.graphicsLayer { rotationZ = if (allPinned) 0f else 45f })
                                }
                                IconButton(onClick = { deleteDialog = true }) { Icon(Icons.Outlined.Delete, "Delete") }
                                IconButton(onClick = {
                                    if (allMuted) {
                                        repo.setMuted(selected, false)
                                        snackbar.show("Notifications unmuted", SnackType.SUCCESS)
                                        selected = emptySet()
                                    } else muteDialog = true
                                }) { Icon(if (allMuted) Icons.AutoMirrored.Outlined.VolumeUp else Icons.Outlined.NotificationsOff, "Mute") }
                                IconButton(onClick = {
                                    val ids = selected
                                    repo.setArchived(ids, true)
                                    selected = emptySet()
                                    snackbar.undo(if (ids.size == 1) "Chat archived" else "${ids.size} chats archived") { repo.setArchived(ids, false) }
                                }) { Icon(Icons.Outlined.Archive, "Archive") }
                                Box {
                                    IconButton(onClick = { selectionMenu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                                    DropdownMenu(selectionMenu, onDismissRequest = { selectionMenu = false }, shape = RoundedCornerShape(16.dp)) {
                                        val anyUnread = selChats.any { it.unreadCount > 0 || it.markedUnread }
                                        DropdownMenuItem(text = { Text(if (anyUnread) "Mark as read" else "Mark as unread") }, onClick = {
                                            if (anyUnread) repo.markRead(selected) else repo.markUnread(selected)
                                            selectionMenu = false; selected = emptySet()
                                        })
                                        val allFav = selChats.all { it.isFavorite }
                                        DropdownMenuItem(text = { Text(if (allFav) "Remove from Favourites" else "Add to Favourites") }, onClick = {
                                            repo.setFavorite(selected, !allFav)
                                            snackbar.show(if (allFav) "Removed from Favourites" else "Added to Favourites", SnackType.SUCCESS)
                                            selectionMenu = false; selected = emptySet()
                                        })
                                        if (selected.size == 1) {
                                            val c = selChats.firstOrNull()
                                            DropdownMenuItem(text = { Text(if (c?.isLocked == true) "Unlock chat" else "Lock chat") }, onClick = {
                                                c?.let {
                                                    repo.setLocked(it.id, !it.isLocked)
                                                    snackbar.show(if (it.isLocked) "Chat unlocked" else "Chat locked. Only you can open it.", SnackType.SUCCESS)
                                                }
                                                selectionMenu = false; selected = emptySet()
                                            })
                                            if (c?.type == ChatType.DIRECT) {
                                                DropdownMenuItem(text = { Text("View contact") }, onClick = {
                                                    selectionMenu = false; selected = emptySet()
                                                    c.contactId?.let { nav.navigate(Routes.contact(it)) }
                                                })
                                            }
                                        }
                                        DropdownMenuItem(text = { Text("Select all") }, onClick = {
                                            selected = visibleItems.map { it.chat.id }.toSet(); selectionMenu = false
                                        })
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        )
                    }
                    2 -> TopAppBar(
                        title = { Text(selectedCalls.size.toString()) },
                        navigationIcon = {
                            IconButton(onClick = { selectedCalls = emptySet() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel selection") }
                        },
                        actions = {
                            IconButton(onClick = {
                                val removed = calls.filter { it.id in selectedCalls }
                                repo.deleteCalls(selectedCalls)
                                selectedCalls = emptySet()
                                snackbar.show(if (removed.size == 1) "Call deleted" else "${removed.size} calls deleted", SnackType.SUCCESS)
                            }) { Icon(Icons.Outlined.Delete, "Delete") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                    else -> TopAppBar(
                        title = {
                            if (tab == 0) {
                                Text(
                                    "Pulse",
                                    color = PulseTheme.colors.brandTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                                )
                            } else {
                                Text(tabs[tab].label, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold))
                            }
                        },
                        actions = {
                            if (tab == 0) {
                                IconButton(onClick = { nav.navigate(Routes.qr(1)) }) { Icon(Icons.Outlined.QrCodeScanner, "Scan QR code") }
                                IconButton(onClick = { nav.navigate(Routes.camera("share")) }) { Icon(Icons.Outlined.CameraAlt, "Camera") }
                            } else if (tab == 1 || tab == 3) {
                                IconButton(onClick = { nav.navigate(Routes.SEARCH) }) { Icon(Icons.Outlined.Search, "Search") }
                            }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More options") }
                                DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                                    @Composable
                                    fun entry(label: String, action: () -> Unit) =
                                        DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                                    when (tab) {
                                        0 -> {
                                            entry("New group") { nav.navigate(Routes.NEW_GROUP) }
                                            entry("New community") { nav.navigate(Routes.NEW_COMMUNITY) }
                                            entry("New broadcast") { nav.navigate(Routes.NEW_BROADCAST) }
                                            entry("Linked devices") { nav.navigate(Routes.LINKED_DEVICES) }
                                            entry("Starred messages") { nav.navigate(Routes.STARRED) }
                                            entry("Read all") {
                                                repo.markAllRead()
                                                snackbar.show("All chats marked as read", SnackType.SUCCESS)
                                            }
                                            entry("Settings") { nav.navigate(Routes.SETTINGS) }
                                        }
                                        1 -> {
                                            entry("Find channels") { nav.navigate(Routes.FIND_CHANNELS) }
                                            entry("Status privacy") { nav.navigate(Routes.PRIVACY) }
                                            entry("Settings") { nav.navigate(Routes.SETTINGS) }
                                        }
                                        2 -> {
                                            entry("New community") { nav.navigate(Routes.NEW_COMMUNITY) }
                                            entry("Settings") { nav.navigate(Routes.SETTINGS) }
                                        }
                                        else -> {
                                            entry("Clear call log") { clearCallsDialog = true }
                                            entry("Settings") { nav.navigate(Routes.SETTINGS) }
                                        }
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, t ->
                    val isSel = tab == index
                    NavigationBarItem(
                        selected = isSel,
                        onClick = { tab = index; selected = emptySet(); selectedCalls = emptySet() },
                        icon = {
                            BadgedBox(badge = {
                                when {
                                    index == 0 && unreadChats > 0 -> Badge(containerColor = PulseTheme.colors.unreadBadge, contentColor = PulseTheme.colors.onUnreadBadge) { Text(unreadChats.toString()) }
                                    index == 1 && (unseenStatus || channelUnread) -> Badge(containerColor = PulseTheme.colors.unreadBadge)
                                    index == 3 && missedCalls > 0 -> Badge(containerColor = PulseTheme.colors.unreadBadge, contentColor = PulseTheme.colors.onUnreadBadge) { Text(missedCalls.toString()) }
                                }
                            }) {
                                Icon(if (isSel) t.selectedIcon else t.icon, t.label)
                            }
                        },
                        label = { Text(t.label, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium, fontSize = 12.5.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                            unselectedTextColor = PulseTheme.colors.textSecondary,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedContent(
                targetState = if (selectionMode || callSelection) -1 else tab,
                transitionSpec = { (scaleIn(tween(200)) + fadeIn()) togetherWith (scaleOut(tween(150)) + fadeOut()) },
                label = "fab",
            ) { t ->
                when (t) {
                    0 -> PulseFab(Icons.Filled.AddComment, "New chat") { nav.navigate(Routes.NEW_CHAT) }
                    1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SmallFloatingActionButton(
                            onClick = { nav.navigate(Routes.TEXT_STATUS) },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(14.dp),
                        ) { Icon(Icons.Filled.Edit, "Text status", modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.height(14.dp))
                        PulseFab(Icons.Filled.CameraAlt, "Camera") { nav.navigate(Routes.camera("status")) }
                    }
                    3 -> PulseFab(Icons.Filled.AddIcCall, "New call") { nav.navigate(Routes.NEW_CHAT) }
                    else -> Box {}
                }
            }
        },
    ) { padding ->
        val listPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp)
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "tabs",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                0 -> ChatsTab(
                    items = visibleItems,
                    archivedCount = archived.size,
                    archivedUnread = archived.count { it.hasUnread },
                    filter = filter,
                    onFilter = { filter = it },
                    selected = selected,
                    listState = chatsState,
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onOpenArchived = { nav.navigate(Routes.ARCHIVED) },
                    onClick = { chat -> if (selected.isNotEmpty()) toggle(chat.id) else openChat(chat.id) },
                    onLongClick = { chat -> toggle(chat.id) },
                    onAvatarClick = { chat ->
                        when {
                            selected.isNotEmpty() -> toggle(chat.id)
                            chat.type == ChatType.DIRECT && chat.contactId != null -> nav.navigate(Routes.contact(chat.contactId))
                            chat.type == ChatType.GROUP -> nav.navigate(Routes.group(chat.id))
                            else -> openChat(chat.id)
                        }
                    },
                    unreadTotal = unreadChats,
                    contentPadding = listPadding,
                )
                1 -> UpdatesTab(
                    myName = settings.name,
                    myAvatar = settings.avatar,
                    myStatus = myStatus,
                    statuses = statuses,
                    contacts = contactMap,
                    channels = channels,
                    listState = updatesState,
                    contentPadding = listPadding,
                    onMyStatus = { nav.navigate(Routes.status("me")) },
                    onAddStatus = { nav.navigate(Routes.camera("status")) },
                    onStatus = { nav.navigate(Routes.status(it)) },
                    onChannel = { nav.navigate(Routes.channel(it.id)) },
                    onFollow = {
                        repo.setFollowing(it.id, true)
                        snackbar.show("You're now following ${it.name}", SnackType.SUCCESS)
                    },
                    onExplore = { nav.navigate(Routes.FIND_CHANNELS) },
                )
                2 -> CommunitiesTab(
                    communities = communities,
                    chats = chatMap,
                    messages = messages,
                    listState = communitiesState,
                    contentPadding = listPadding,
                    onNewCommunity = { nav.navigate(Routes.NEW_COMMUNITY) },
                    onCommunity = { nav.navigate(Routes.community(it.id)) },
                    onChat = { openChat(it.id) },
                )
                else -> CallsTab(
                    calls = calls,
                    favorites = favoriteCalls,
                    contacts = contactMap,
                    chats = chatMap,
                    selected = selectedCalls,
                    listState = callsState,
                    contentPadding = listPadding,
                    onCallLink = { callLinkDialog = true },
                    onFavorite = { c, _ -> nav.navigate(Routes.call(c.id, video = false)) },
                    onAddFavorite = { nav.navigate(Routes.NEW_CHAT) },
                    onCallInfo = { call -> openCallPeer(nav, call) },
                    onCallBack = { call -> nav.navigate(Routes.call(call.peerId, call.type == com.pulse.messenger.data.CallType.VIDEO)) },
                    onLongClick = { call ->
                        selectedCalls = if (call.id in selectedCalls) selectedCalls - call.id else selectedCalls + call.id
                    },
                )
            }
        }
    }

    if (deleteDialog) {
        val ids = selected
        val n = ids.size
        ConfirmDialog(
            title = if (n == 1) "Delete this chat?" else "Delete $n chats?",
            message = "Messages will be removed from this device. You can undo this for a few seconds.",
            confirmText = if (n == 1) "Delete chat" else "Delete chats",
            destructive = true,
            icon = Icons.Outlined.Delete,
            checkboxLabel = "Also delete media received in ${if (n == 1) "this chat" else "these chats"} from the device gallery",
            onConfirm = {},
            onConfirmWithCheck = {
                val removed = repo.deleteChats(ids)
                selected = emptySet()
                snackbar.undo(if (n == 1) "Chat deleted" else "$n chats deleted") { repo.restoreChats(removed) }
            },
            onDismiss = { deleteDialog = false },
        )
    }
    if (muteDialog) {
        MuteDialog(
            onMute = { duration ->
                repo.setMuted(selected, true)
                selected = emptySet()
                snackbar.show("Muted for ${duration.lowercase()}", SnackType.SUCCESS)
            },
            onDismiss = { muteDialog = false },
        )
    }
    if (clearCallsDialog) {
        ConfirmDialog(
            title = "Clear call log?",
            message = "Do you want to clear your entire call log?",
            confirmText = "Clear",
            destructive = true,
            icon = Icons.Outlined.Delete,
            onConfirm = {
                repo.clearCallLog()
                snackbar.show("Call log cleared", SnackType.SUCCESS)
            },
            onDismiss = { clearCallsDialog = false },
        )
    }
    if (callLinkDialog) {
        val link = remember { "https://call.pulse.app/" + (1..10).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("") }
        PulseDialog(
            onDismiss = { callLinkDialog = false },
            icon = Icons.Outlined.Link,
            title = "Call link created",
            message = "Anyone with Pulse can use this link to join the call. Only share it with people you trust.\n\n$link",
            buttons = listOf(
                DialogButton("Copy") {
                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("Call link", link))
                    snackbar.show("Link copied", SnackType.SUCCESS)
                    callLinkDialog = false
                },
                DialogButton("Share") {
                    callLinkDialog = false
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join my Pulse call: $link")
                    }
                    context.startActivity(Intent.createChooser(send, "Share call link"))
                },
            ),
        )
    }
}

private fun openCallPeer(nav: NavController, call: CallLog) {
    if (call.isGroup) nav.navigate(Routes.group(call.peerId)) else nav.navigate(Routes.contact(call.peerId))
}

@Composable
private fun PulseFab(icon: ImageVector, description: String, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
    ) { Icon(icon, description) }
}
