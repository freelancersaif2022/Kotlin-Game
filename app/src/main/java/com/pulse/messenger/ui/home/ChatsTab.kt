package com.pulse.messenger.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.messenger.R
import com.pulse.messenger.data.Chat
import com.pulse.messenger.data.ChatType
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.data.PulseRepository
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.CountBadge
import com.pulse.messenger.ui.components.EmptyState
import com.pulse.messenger.ui.components.MessageTicks
import com.pulse.messenger.ui.components.SearchPill
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format

@Immutable
data class ChatListItem(
    val chat: Chat,
    val last: Message?,
    val senderName: String?,
    val typingName: String?,
    val isBlocked: Boolean,
) {
    val sortTime: Long get() = last?.timestamp ?: chat.createdAt
    val hasUnread: Boolean get() = chat.unreadCount > 0 || chat.markedUnread
}

fun buildChatItems(
    chats: List<Chat>,
    messages: Map<String, List<Message>>,
    blocked: List<String>,
    repo: PulseRepository,
): List<ChatListItem> = chats.map { chat ->
    val last = messages[chat.id]?.lastOrNull()
    val sender = when {
        last == null -> null
        last.senderId == "system" -> null
        chat.type == ChatType.GROUP && !last.isMine -> repo.displayName(last.senderId).substringBefore(" ")
        else -> null
    }
    val typing = chat.typingUserId?.let {
        if (chat.type == ChatType.GROUP) "${repo.displayName(it).substringBefore(" ")} is typing…" else "typing…"
    }
    ChatListItem(chat, last, sender, typing, chat.contactId != null && chat.contactId in blocked)
}.sortedWith(compareByDescending<ChatListItem> { it.chat.isPinned }.thenByDescending { it.sortTime })

fun previewIcon(content: MessageContent): ImageVector? = when (content) {
    is MessageContent.Image -> Icons.Outlined.Image
    is MessageContent.Video -> Icons.Outlined.Videocam
    is MessageContent.Voice -> Icons.Outlined.Mic
    is MessageContent.Document -> Icons.Outlined.InsertDriveFile
    is MessageContent.Location -> Icons.Outlined.Place
    is MessageContent.ContactCard -> Icons.Outlined.Person
    is MessageContent.Sticker -> Icons.Outlined.StickyNote2
    is MessageContent.Poll -> Icons.Outlined.Poll
    is MessageContent.Link -> Icons.Outlined.Link
    is MessageContent.Deleted -> Icons.Outlined.Block
    else -> null
}

fun previewText(content: MessageContent): String = when (content) {
    is MessageContent.Text -> content.text
    is MessageContent.Image -> content.caption.ifEmpty { "Photo" }
    is MessageContent.Video -> content.caption.ifEmpty { "Video" }
    is MessageContent.Voice -> Format.duration(content.durationSec)
    is MessageContent.Document -> content.name
    is MessageContent.Location -> if (content.isLive) "Live location" else "Location"
    is MessageContent.ContactCard -> content.name
    is MessageContent.Sticker -> "Sticker"
    is MessageContent.Link -> content.text
    is MessageContent.Poll -> content.question
    is MessageContent.Deleted -> "This message was deleted"
    is MessageContent.System -> content.text
}

enum class ChatFilter(val label: String) { ALL("All"), UNREAD("Unread"), FAVOURITES("Favourites"), GROUPS("Groups") }

@Composable
fun ChatsTab(
    items: List<ChatListItem>,
    archivedCount: Int,
    archivedUnread: Int,
    filter: ChatFilter,
    onFilter: (ChatFilter) -> Unit,
    selected: Set<String>,
    listState: LazyListState,
    onSearch: () -> Unit,
    onOpenArchived: () -> Unit,
    onClick: (Chat) -> Unit,
    onLongClick: (Chat) -> Unit,
    onAvatarClick: (Chat) -> Unit,
    unreadTotal: Int,
    contentPadding: PaddingValues,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "search", contentType = "search") {
            SearchPill("Search chats and messages", onSearch, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        item(key = "filters", contentType = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ChatFilter.entries) { f ->
                    FilterPill(
                        label = if (f == ChatFilter.UNREAD && unreadTotal > 0) "${f.label} $unreadTotal" else f.label,
                        selected = filter == f,
                        onClick = { onFilter(f) },
                    )
                }
            }
        }
        if (archivedCount > 0 && filter == ChatFilter.ALL) {
            item(key = "archived", contentType = "archived") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenArchived)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Archive, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Archived", style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
                    if (archivedUnread > 0) {
                        Text(archivedUnread.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text(archivedCount.toString(), color = PulseTheme.colors.textSecondary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                when (filter) {
                    ChatFilter.ALL -> EmptyState(R.drawable.empty_chats, "Start chatting on Pulse", "Tap the button below to message a contact or create a group.")
                    ChatFilter.UNREAD -> EmptyState(R.drawable.empty_notifications, "No unread chats", "You're all caught up. New messages will appear here.")
                    ChatFilter.FAVOURITES -> EmptyState(R.drawable.empty_starred, "No favourites yet", "Add people and groups to Favourites to find them quickly.")
                    ChatFilter.GROUPS -> EmptyState(R.drawable.empty_communities, "No groups yet", "Create a group to chat with several people at once.")
                }
            }
        }
        items(items, key = { it.chat.id }, contentType = { "chat" }) { item ->
            ChatRow(
                item = item,
                selected = item.chat.id in selected,
                onClick = { onClick(item.chat) },
                onLongClick = { onLongClick(item.chat) },
                onAvatarClick = { onAvatarClick(item.chat) },
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "footer", contentType = "footer") {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 22.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, null, tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("Your personal messages are ", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                Text("end-to-end encrypted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = PulseTheme.colors
    val bg by animateColorAsState(if (selected) colors.chipSelected else colors.chip, label = "chipbg")
    val fg by animateColorAsState(if (selected) colors.onChipSelected else colors.textSecondary, label = "chipfg")
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRow(
    item: ChatListItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chat = item.chat
    val colors = PulseTheme.colors
    val bg by animateColorAsState(if (selected) colors.chipSelected.copy(alpha = 0.6f) else Color.Transparent, label = "rowbg")
    Row(
        modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(
                chat.avatar, chat.title, 52.dp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAvatarClick),
                fallback = when (chat.type) {
                    ChatType.GROUP -> AvatarFallback.GROUP
                    ChatType.BROADCAST -> AvatarFallback.BROADCAST
                    ChatType.DIRECT -> AvatarFallback.INITIALS
                },
            )
            AnimatedVisibility(selected, enter = scaleIn(), exit = scaleOut(), modifier = Modifier.align(Alignment.BottomEnd)) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.isLocked) {
                    Icon(Icons.Outlined.Lock, null, tint = colors.textSecondary, modifier = Modifier.padding(start = 4.dp).size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    item.last?.let { Format.chatListTime(it.timestamp) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = if (item.hasUnread) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (item.hasUnread) colors.unreadBadge else colors.textSecondary,
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    ChatPreviewLine(item)
                }
                if (chat.isMuted) {
                    Icon(Icons.AutoMirrored.Filled.VolumeOff, "Muted", tint = colors.textSecondary, modifier = Modifier.padding(start = 6.dp).size(17.dp))
                }
                if (chat.isPinned) {
                    Icon(Icons.Filled.PushPin, "Pinned", tint = colors.textSecondary, modifier = Modifier.padding(start = 6.dp).size(16.dp).graphicsLayer { rotationZ = 45f })
                }
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    CountBadge(chat.unreadCount, color = if (chat.isMuted) colors.textSecondary.copy(alpha = 0.6f) else colors.unreadBadge)
                } else if (chat.markedUnread) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(18.dp).background(colors.unreadBadge, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun ChatPreviewLine(item: ChatListItem) {
    val colors = PulseTheme.colors
    val style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp)
    val typing = item.typingName
    if (typing != null) {
        Text(typing, style = style, color = colors.unreadBadge, maxLines = 1, fontWeight = FontWeight.Medium)
        return
    }
    val draft = item.chat.draft
    if (draft.isNotBlank()) {
        Text("Draft: ", style = style, color = colors.unreadBadge, fontWeight = FontWeight.Medium)
        Text(draft, style = style, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    val last = item.last
    if (last == null) {
        val hint = if (item.chat.disappearingSeconds > 0) "Disappearing messages are on" else "Tap to start chatting"
        if (item.chat.disappearingSeconds > 0) {
            Icon(Icons.Outlined.Timer, null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(hint, style = style, color = colors.textSecondary, maxLines = 1)
        return
    }
    if (item.chat.type == ChatType.BROADCAST) {
        Icon(Icons.Outlined.Campaign, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
    }
    if (last.isMine && last.content !is MessageContent.Deleted && last.content !is MessageContent.System) {
        MessageTicks(last.status, size = 17.dp, baseColor = colors.textSecondary)
        Spacer(Modifier.width(3.dp))
    }
    if (item.senderName != null) {
        Text("${item.senderName}: ", style = style, color = colors.textSecondary, maxLines = 1)
    }
    previewIcon(last.content)?.let {
        Icon(it, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
    }
    Text(
        previewText(last.content),
        style = style,
        color = colors.textSecondary,
        fontStyle = if (last.content is MessageContent.Deleted) FontStyle.Italic else FontStyle.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
