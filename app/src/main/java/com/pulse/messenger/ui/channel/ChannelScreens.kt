package com.pulse.messenger.ui.channel

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.data.Channel
import com.pulse.messenger.data.ChannelPost
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.chat.DayChip
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.ChatWallpaper
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.SearchTopBar
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.home.FilterPill
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format

private val postReactions = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelScreen(nav: NavController, channelId: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val channels by repo.channels.collectAsStateWithLifecycle()
    val channel = channels.firstOrNull { it.id == channelId }
    if (channel == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    var menu by remember { mutableStateOf(false) }
    var reactFor by remember { mutableStateOf<String?>(null) }
    var unfollowDialog by remember { mutableStateOf(false) }
    val posts = channel.posts.sortedByDescending { it.timestamp }
    val listState = rememberLazyListState()
    LaunchedEffect(channelId) { repo.markChannelRead(channelId) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Avatar(channel.avatar, channel.name, 40.dp, Modifier.clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(channel.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (channel.isVerified) Icon(Icons.Filled.Verified, "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                    }
                    Text("${Format.compact(channel.followers)} followers", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                }
                if (channel.isFollowing) {
                    IconButton(onClick = {
                        repo.setChannelMuted(channelId, !channel.isMuted)
                        snackbar.show(if (channel.isMuted) "Notifications on" else "Channel muted", SnackType.SUCCESS)
                    }) { Icon(if (channel.isMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications, "Notifications") }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                        DropdownMenuItem(text = { Text("Share channel") }, onClick = {
                            menu = false
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Follow ${channel.name} on Pulse: https://pulse.app/channel/${channel.id}")
                            }, "Share channel"))
                        })
                        if (channel.isFollowing) DropdownMenuItem(text = { Text("Unfollow") }, onClick = { menu = false; unfollowDialog = true })
                        DropdownMenuItem(text = { Text("Report") }, onClick = { menu = false; snackbar.show("Thanks, we'll review this channel", SnackType.SUCCESS) })
                    }
                }
            }
        }
        ChatWallpaper(null, 0f, Modifier.weight(1f)) {
            LazyColumn(state = listState, reverseLayout = true, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(posts, key = { it.id }) { p ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                        PostCard(
                            p,
                            onLongClick = { reactFor = p.id },
                            onReact = { e -> repo.reactToPost(channelId, p.id, e) },
                            onForward = { snackbar.show("Copied link to post", SnackType.SUCCESS) },
                        )
                    }
                }
                item(key = "intro") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(16.dp)).background(PulseTheme.colors.systemBubble).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Avatar(channel.avatar, channel.name, 64.dp, Modifier.clip(RoundedCornerShape(18.dp)))
                        Spacer(Modifier.height(8.dp))
                        Text(channel.name, style = MaterialTheme.typography.titleMedium)
                        Text(channel.description, style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.systemBubbleText, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                if (posts.isNotEmpty()) item(key = "day") { DayChip(Format.daySeparator(posts.last().timestamp)) }
            }
        }
        if (!channel.isFollowing) {
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding().padding(16.dp)) {
                PrimaryButton("Follow", onClick = {
                    repo.setFollowing(channelId, true)
                    snackbar.show("You're now following ${channel.name}", SnackType.SUCCESS)
                }, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Text(
                "Only admins can send messages",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding().padding(14.dp),
            )
        }
    }

    reactFor?.let { id ->
        com.pulse.messenger.ui.components.PulseBottomSheet(onDismiss = { reactFor = null }) {
            Text("React to post", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp))
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                postReactions.forEach { e ->
                    Text(e, fontSize = 32.sp, modifier = Modifier.clip(CircleShape).clickable { repo.reactToPost(channelId, id, e); reactFor = null }.padding(6.dp))
                }
            }
        }
    }
    if (unfollowDialog) {
        ConfirmDialog(
            title = "Unfollow ${channel.name}?", message = "You won't receive updates from this channel anymore.",
            confirmText = "Unfollow", destructive = true,
            onConfirm = { repo.setFollowing(channelId, false); snackbar.show("Unfollowed ${channel.name}", SnackType.SUCCESS) },
            onDismiss = { unfollowDialog = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostCard(p: ChannelPost, onLongClick: () -> Unit, onReact: (String) -> Unit, onForward: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PulseTheme.colors.bubbleIn)
                .combinedClickable(onClick = {}, onLongClick = onLongClick),
        ) {
            if (p.image != null) {
                AsyncImage(
                    model = p.image, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(4.dp).fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(10.dp)),
                )
            }
            Text(p.text, style = MaterialTheme.typography.bodyLarge, color = PulseTheme.colors.bubbleInText, modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Visibility, null, tint = PulseTheme.colors.bubbleMeta, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(Format.compact(p.views), fontSize = 11.sp, color = PulseTheme.colors.bubbleMeta)
                Spacer(Modifier.weight(1f))
                Text(Format.clock(p.timestamp), fontSize = 11.sp, color = PulseTheme.colors.bubbleMeta)
            }
            if (p.reactions.isNotEmpty()) {
                LazyRow(contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(p.reactions, key = { it.first }) { (e, n) ->
                        val mine = p.myReaction == e
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (mine) PulseTheme.colors.chipSelected else PulseTheme.colors.chip)
                                .then(if (mine) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
                                .clickable { onReact(e) }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(e, fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(Format.compact(n), fontSize = 12.sp, color = PulseTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.12f)).clickable(onClick = onForward),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.Forward, "Share post", tint = Color.White, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun FindChannelsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val channels by repo.channels.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("All") }
    val categories = listOf("All") + channels.map { it.category }.distinct()
    val filtered = channels.filter {
        (category == "All" || it.category == category) &&
            (query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true))
    }.sortedByDescending { it.followers }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) {
            if (searching) {
                SearchTopBar(query, { query = it }, onClose = { searching = false; query = "" }, placeholder = "Search channels")
            } else {
                com.pulse.messenger.ui.components.PulseTopBar("Channels", onBack = { nav.back() }, actions = {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, "Search") }
                })
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { c -> FilterPill(c, category == c) { category = c } }
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            if (filtered.isEmpty()) {
                item {
                    com.pulse.messenger.ui.components.EmptyState(com.pulse.messenger.R.drawable.empty_search, "No channels found", "Try a different name or category.")
                }
            }
            items(filtered, key = { it.id }) { ch -> DirectoryRow(ch, onOpen = { nav.navigate(com.pulse.messenger.ui.navigation.Routes.channel(ch.id)) }) {
                repo.setFollowing(ch.id, !ch.isFollowing)
                snackbar.show(if (ch.isFollowing) "Unfollowed ${ch.name}" else "You're now following ${ch.name}", SnackType.SUCCESS)
            } }
        }
    }
}

@Composable
private fun DirectoryRow(ch: Channel, onOpen: () -> Unit, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(ch.avatar, ch.name, 52.dp, Modifier.clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ch.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                if (ch.isVerified) Icon(Icons.Filled.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp).size(15.dp))
            }
            Text("${Format.compact(ch.followers)} followers · ${ch.category}", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (ch.isFollowing) "Following" else "Follow",
            color = if (ch.isFollowing) PulseTheme.colors.textSecondary else MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (ch.isFollowing) PulseTheme.colors.chip else MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
