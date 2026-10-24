package com.pulse.messenger.ui.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulse.messenger.data.Channel
import com.pulse.messenger.data.Contact
import com.pulse.messenger.data.StatusItem
import com.pulse.messenger.data.StatusUpdate
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.CountBadge
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.components.StatusAvatar
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format

@Composable
fun UpdatesTab(
    myName: String,
    myAvatar: Int?,
    myStatus: List<StatusItem>,
    statuses: List<StatusUpdate>,
    contacts: Map<String, Contact>,
    channels: List<Channel>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onMyStatus: () -> Unit,
    onAddStatus: () -> Unit,
    onStatus: (String) -> Unit,
    onChannel: (Channel) -> Unit,
    onFollow: (Channel) -> Unit,
    onExplore: () -> Unit,
) {
    val recent = statuses.filter { !it.isMuted }.sortedWith(compareBy<StatusUpdate> { it.allViewed }.thenByDescending { it.latest.timestamp })
    val muted = statuses.filter { it.isMuted }
    val following = channels.filter { it.isFollowing }
        .sortedByDescending { ch -> ch.posts.maxOfOrNull { it.timestamp } ?: 0L }
    val suggested = channels.filter { !it.isFollowing }
    var showMuted by rememberSaveable { mutableStateOf(false) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "status_header") {
            Text(
                "Status",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp),
            )
        }
        item(key = "status_row") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "me") {
                    MyStatusCard(myName, myAvatar, myStatus, onClick = { if (myStatus.isEmpty()) onAddStatus() else onMyStatus() }, onAdd = onAddStatus)
                }
                items(recent, key = { it.contactId }) { s ->
                    val c = contacts[s.contactId]
                    StatusCard(s, c?.name.orEmpty(), c?.avatar) { onStatus(s.contactId) }
                }
            }
        }
        if (muted.isNotEmpty()) {
            item(key = "muted") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showMuted = !showMuted }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Muted updates (${muted.size})", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                    Icon(if (showMuted) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = PulseTheme.colors.textSecondary)
                }
            }
            if (showMuted) {
                items(muted, key = { "m_" + it.contactId }) { s ->
                    val c = contacts[s.contactId]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onStatus(s.contactId) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusAvatar(c?.avatar, c?.name.orEmpty(), 52.dp, s.items.size, s.viewedCount, Modifier.padding(end = 14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c?.name.orEmpty(), style = MaterialTheme.typography.titleSmall, color = PulseTheme.colors.textSecondary)
                            Text(Format.relative(s.latest.timestamp), style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }

        item(key = "channels_header") {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Channels",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onExplore) { Text("Explore", fontWeight = FontWeight.SemiBold) }
            }
        }
        if (following.isEmpty()) {
            item(key = "no_channels") {
                Text(
                    "Stay updated on topics that matter to you. Find channels to follow below.",
                    style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(following, key = { "ch_" + it.id }, contentType = { "channel" }) { ch ->
            ChannelRow(ch, onClick = { onChannel(ch) })
        }
        if (suggested.isNotEmpty()) {
            item(key = "find_header") { SectionHeader("Find channels to follow") }
            item(key = "find_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(suggested, key = { it.id }) { ch ->
                        SuggestedChannelCard(ch, onClick = { onChannel(ch) }, onFollow = { onFollow(ch) })
                    }
                }
            }
            item(key = "explore_more") {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    TextButton(
                        onClick = onExplore,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(PulseTheme.colors.chip),
                    ) { Text("Explore more", modifier = Modifier.padding(horizontal = 12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MyStatusCard(name: String, avatar: Int?, items: List<StatusItem>, onClick: () -> Unit, onAdd: () -> Unit) {
    Box(
        Modifier
            .size(width = 104.dp, height = 168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PulseTheme.colors.chip)
            .clickable(onClick = onClick),
    ) {
        val latest = items.lastOrNull()
        if (latest != null) StatusThumb(latest)
        else if (avatar != null) {
            AsyncImage(model = avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = 220f)))
        Box(Modifier.padding(8.dp)) {
            if (latest != null) {
                StatusAvatar(avatar, name, 38.dp, items.size, items.size, ringWidth = 2.dp)
            } else {
                Avatar(avatar, name, 38.dp, Modifier.border(2.dp, Color.White, CircleShape), fallback = AvatarFallback.PERSON)
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(top = 24.dp, start = 24.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, "Add status", tint = Color.White, modifier = Modifier.size(13.dp)) }
        }
        Text(
            if (items.isEmpty()) "Add status" else "My status",
            color = Color.White, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
    }
}

@Composable
private fun StatusThumb(item: StatusItem) {
    if (item.image != null) {
        AsyncImage(model = item.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
        Box(Modifier.fillMaxSize()) {
            if (item.background != null) {
                AsyncImage(model = item.background, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Text(
                item.text,
                color = Color.White, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center,
                maxLines = 5, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(10.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(s: StatusUpdate, name: String, avatar: Int?, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 104.dp, height = 168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PulseTheme.colors.chip)
            .clickable(onClick = onClick),
    ) {
        StatusThumb(s.latest)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
        StatusAvatar(avatar, name, 40.dp, s.items.size, s.viewedCount, Modifier.padding(8.dp), ringWidth = 2.5.dp)
        Text(
            name, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
    }
}

@Composable
fun ChannelRow(ch: Channel, onClick: () -> Unit) {
    val last = ch.posts.maxByOrNull { it.timestamp }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(ch.avatar, ch.name, 52.dp, Modifier.clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ch.name, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (ch.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Verified, "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    last?.let { Format.chatListTime(it.timestamp) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ch.unread > 0) PulseTheme.colors.unreadBadge else PulseTheme.colors.textSecondary,
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    last?.text ?: ch.description,
                    style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (ch.unread > 0) {
                    Spacer(Modifier.width(6.dp))
                    CountBadge(ch.unread)
                }
            }
        }
    }
}

@Composable
private fun SuggestedChannelCard(ch: Channel, onClick: () -> Unit, onFollow: () -> Unit) {
    Column(
        Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, PulseTheme.colors.divider, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(ch.avatar, ch.name, 64.dp, Modifier.clip(RoundedCornerShape(20.dp)))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ch.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (ch.isVerified) Icon(Icons.Filled.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 2.dp).size(13.dp))
        }
        Text("${Format.compact(ch.followers)} followers", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onFollow)
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Follow", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge) }
    }
}
