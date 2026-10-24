package com.pulse.messenger.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.messenger.R
import com.pulse.messenger.data.CallDirection
import com.pulse.messenger.data.CallLog
import com.pulse.messenger.data.CallType
import com.pulse.messenger.data.Chat
import com.pulse.messenger.data.Community
import com.pulse.messenger.data.Contact
import com.pulse.messenger.data.Message
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.CountBadge
import com.pulse.messenger.ui.components.EmptyState
import com.pulse.messenger.ui.components.ListDivider
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format

@Composable
fun CommunitiesTab(
    communities: List<Community>,
    chats: Map<String, Chat>,
    messages: Map<String, List<Message>>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onNewCommunity: () -> Unit,
    onCommunity: (Community) -> Unit,
    onChat: (Chat) -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "new") {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onNewCommunity).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(PulseTheme.colors.chip),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Groups, null, tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(30.dp)) }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                }
                Spacer(Modifier.width(14.dp))
                Text("New community", style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp))
            }
        }
        if (communities.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    R.drawable.empty_communities, "Stay connected with a community",
                    "Communities bring members together in topic-based groups and make it easy to get admin announcements.",
                    action = "Start your community", onAction = onNewCommunity,
                )
            }
        }
        communities.forEach { community ->
            item(key = "gap_" + community.id) {
                Box(Modifier.fillMaxWidth().height(8.dp).background(PulseTheme.colors.chip.copy(alpha = 0.6f)))
            }
            item(key = community.id) {
                Row(
                    Modifier.fillMaxWidth().clickable { onCommunity(community) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(community.avatar, community.name, 52.dp, Modifier.clip(RoundedCornerShape(16.dp)))
                    Spacer(Modifier.width(14.dp))
                    Text(community.name, style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
                }
                ListDivider(16.dp)
            }
            val ids = listOf(community.announcementChatId) + community.groupIds
            items(ids.take(3), key = { community.id + "_" + it }) { id ->
                val chat = chats[id] ?: return@items
                val last = messages[id]?.lastOrNull()
                val isAnnouncement = id == community.announcementChatId
                Row(
                    Modifier.fillMaxWidth().clickable { onChat(chat) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isAnnouncement) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        Avatar(chat.avatar, chat.title, 42.dp, fallback = AvatarFallback.GROUP)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isAnnouncement) "Announcements" else chat.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                            )
                            Text(last?.let { Format.chatListTime(it.timestamp) }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                last?.let { previewText(it.content) } ?: "No messages yet",
                                style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                            )
                            if (chat.unreadCount > 0) CountBadge(chat.unreadCount)
                        }
                    }
                }
            }
            item(key = "all_" + community.id) {
                Row(
                    Modifier.fillMaxWidth().clickable { onCommunity(community) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(42.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ChevronRight, null, tint = PulseTheme.colors.textSecondary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("View all", style = MaterialTheme.typography.bodyLarge, color = PulseTheme.colors.textSecondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallsTab(
    calls: List<CallLog>,
    favorites: List<String>,
    contacts: Map<String, Contact>,
    chats: Map<String, Chat>,
    selected: Set<String>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onCallLink: () -> Unit,
    onFavorite: (Contact, Boolean) -> Unit,
    onAddFavorite: () -> Unit,
    onCallInfo: (CallLog) -> Unit,
    onCallBack: (CallLog) -> Unit,
    onLongClick: (CallLog) -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "link") {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onCallLink).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Link, null, tint = Color.White) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Create call link", style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp))
                    Text("Share a link for your Pulse call", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary)
                }
            }
        }
        item(key = "fav_header") { SectionHeader("Favourites") }
        item(key = "fav_row") {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(favorites, key = { it }) { id ->
                    val c = contacts[id] ?: return@items
                    Column(
                        Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onFavorite(c, false) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Avatar(c.avatar, c.name, 56.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(c.name.substringBefore(" "), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                item(key = "add") {
                    Column(
                        Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddFavorite)
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(PulseTheme.colors.chip),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Add, null, tint = PulseTheme.colors.textSecondary) }
                        Spacer(Modifier.height(6.dp))
                        Text("Add", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item(key = "recent_header") { SectionHeader("Recent") }
        if (calls.isEmpty()) {
            item(key = "empty") {
                EmptyState(R.drawable.empty_calls, "No recent calls", "Start a voice or video call with friends and family. Your calls will show up here.")
            }
        }
        items(calls, key = { it.id }, contentType = { "call" }) { call ->
            val name: String
            val avatar: Int?
            if (call.isGroup) {
                val g = chats[call.peerId]
                name = g?.title ?: "Group call"; avatar = g?.avatar
            } else {
                val c = contacts[call.peerId]
                name = c?.name ?: "Unknown"; avatar = c?.avatar
            }
            val missed = call.direction == CallDirection.MISSED
            val isSelected = call.id in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) PulseTheme.colors.chipSelected.copy(alpha = 0.6f) else Color.Transparent)
                    .combinedClickable(onClick = { if (selected.isNotEmpty()) onLongClick(call) else onCallInfo(call) }, onLongClick = { onLongClick(call) })
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                    .animateItem(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Avatar(avatar, name, 52.dp, fallback = if (call.isGroup) AvatarFallback.GROUP else AvatarFallback.INITIALS)
                    if (isSelected) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(20.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name + if (call.count > 1) " (${call.count})" else "",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = if (missed) PulseTheme.colors.danger else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (icon, tint) = when (call.direction) {
                            CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to PulseTheme.colors.success
                            CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to PulseTheme.colors.success
                            CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed to PulseTheme.colors.danger
                        }
                        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            Format.relative(call.timestamp) + (Format.callDuration(call.durationSec).takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1,
                        )
                    }
                }
                IconButton(onClick = { onCallBack(call) }) {
                    Icon(
                        if (call.type == CallType.VIDEO) Icons.Outlined.Videocam else Icons.Outlined.Call,
                        "Call back", tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        item(key = "footer") {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                Text("Your personal calls are end-to-end encrypted", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
            }
        }
    }
}
