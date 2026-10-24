package com.pulse.messenger.ui.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.data.ChatType
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.EmptyState
import com.pulse.messenger.ui.components.SearchTopBar
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.home.FilterPill
import com.pulse.messenger.ui.home.previewIcon
import com.pulse.messenger.ui.home.previewText
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format

private enum class SearchFilter(val label: String) { UNREAD("Unread"), PHOTOS("Photos"), VIDEOS("Videos"), LINKS("Links"), DOCS("Documents"), AUDIO("Audio"), POLLS("Polls") }

private fun Message.searchText(): String = when (val c = content) {
    is MessageContent.Text -> c.text
    is MessageContent.Image -> c.caption
    is MessageContent.Video -> c.caption
    is MessageContent.Link -> c.text + " " + c.title
    is MessageContent.Document -> c.name
    is MessageContent.Location -> c.title + " " + c.address
    is MessageContent.ContactCard -> c.name
    is MessageContent.Poll -> c.question
    else -> ""
}

@Composable
fun SearchScreen(nav: NavController) {
    val repo = LocalRepo.current
    val chats by repo.chats.collectAsStateWithLifecycle()
    val messages by repo.messages.collectAsStateWithLifecycle()
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<SearchFilter?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val q = query.trim()
    val chatResults = remember(q, chats, filter) {
        when {
            filter == SearchFilter.UNREAD -> chats.filter { it.unreadCount > 0 || it.markedUnread }
            q.isEmpty() || filter != null -> emptyList()
            else -> chats.filter { it.title.contains(q, true) }
        }
    }
    val contactResults = remember(q, contacts, chats, filter) {
        if (q.isEmpty() || filter != null) emptyList() else {
            val withChat = chats.mapNotNull { it.contactId }.toSet()
            contacts.filter { it.id !in withChat && (it.name.contains(q, true) || it.phone.replace(" ", "").contains(q.replace(" ", ""))) }
        }
    }
    val messageResults = remember(q, messages, filter) {
        val all = messages.values.flatten()
        val typed = when (filter) {
            SearchFilter.PHOTOS -> all.filter { it.content is MessageContent.Image }
            SearchFilter.VIDEOS -> all.filter { it.content is MessageContent.Video }
            SearchFilter.LINKS -> all.filter { it.content is MessageContent.Link }
            SearchFilter.DOCS -> all.filter { it.content is MessageContent.Document }
            SearchFilter.AUDIO -> all.filter { it.content is MessageContent.Voice }
            SearchFilter.POLLS -> all.filter { it.content is MessageContent.Poll }
            SearchFilter.UNREAD -> emptyList()
            null -> if (q.isEmpty()) emptyList() else all
        }
        (if (q.isEmpty()) typed else typed.filter { it.searchText().contains(q, true) })
            .filter { it.content !is MessageContent.System && it.content !is MessageContent.Deleted }
            .sortedByDescending { it.timestamp }
            .take(200)
    }
    val hl = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Box(Modifier.statusBarsPadding()) {
            SearchTopBar(query, { query = it }, onClose = { nav.back() }, placeholder = "Search chats, messages and contacts", focusRequester = focus)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SearchFilter.entries) { f -> FilterPill(f.label, filter == f) { filter = if (filter == f) null else f } }
        }
        val photoGrid = filter == SearchFilter.PHOTOS || filter == SearchFilter.VIDEOS
        LazyColumn(Modifier.fillMaxSize()) {
            if (q.isEmpty() && filter == null) {
                item { SectionHeader("Recent chats") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(chats.filter { it.type == ChatType.DIRECT && it.avatar != null }.take(10), key = { it.id }) { c ->
                            Column(
                                Modifier.width(72.dp).clip(RoundedCornerShape(12.dp)).clickable { nav.navigate(Routes.chat(c.id)) }.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Avatar(c.avatar, c.title, 54.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(c.title.substringBefore(" "), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                item {
                    EmptyState(R.drawable.empty_search, "Search Pulse", "Find chats, contacts and messages. Use filters to look for photos, links, documents and more.", imageSize = 160.dp)
                }
            }
            if (chatResults.isNotEmpty()) {
                item { SectionHeader("Chats") }
                items(chatResults, key = { "c" + it.id }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(c.id)) }.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.avatar, c.title, 48.dp, fallback = if (c.type == ChatType.GROUP) AvatarFallback.GROUP else AvatarFallback.INITIALS)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(highlight(c.title, q, hl), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            val last = messages[c.id]?.lastOrNull()
                            Text(last?.let { previewText(it.content) }.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (contactResults.isNotEmpty()) {
                item { SectionHeader("Contacts") }
                items(contactResults, key = { "p" + it.id }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(repo.directChatFor(c.id).id)) }.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.avatar, c.name, 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(highlight(c.name, q, hl), style = MaterialTheme.typography.titleSmall)
                            Text(c.about, style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1)
                        }
                    }
                }
            }
            if (messageResults.isNotEmpty()) {
                item { SectionHeader(if (filter != null) filter!!.label else "Messages") }
                if (photoGrid) {
                    items(messageResults.chunked(3), key = { row -> "g" + row.first().id }) { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            row.forEach { m ->
                                val res = (m.content as? MessageContent.Image)?.res ?: (m.content as? MessageContent.Video)?.thumb
                                AsyncImage(
                                    model = res, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).aspectRatio(1f).clickable { nav.navigate(Routes.viewer(m.chatId, m.id)) },
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                } else {
                    items(messageResults, key = { "m" + it.id }) { m ->
                        val chat = chats.firstOrNull { it.id == m.chatId }
                        Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(m.chatId)) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(chat?.title.orEmpty(), style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
                                    Text(Format.chatListTime(m.timestamp), style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    previewIcon(m.content)?.let { Icon(it, null, tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)) }
                                    Text(
                                        highlight((if (m.isMine) "You: " else if (chat?.type == ChatType.GROUP) repo.displayName(m.senderId).substringBefore(" ") + ": " else "") + previewText(m.content), q, hl),
                                        style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if ((q.isNotEmpty() || filter != null) && chatResults.isEmpty() && contactResults.isEmpty() && messageResults.isEmpty()) {
                item {
                    EmptyState(R.drawable.empty_search, "No results", if (q.isNotEmpty()) "Nothing matched \"$q\". Try another word." else "No ${filter?.label?.lowercase()} found in your chats.", imageSize = 160.dp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun highlight(text: String, q: String, color: Color) = buildAnnotatedString {
    if (q.isEmpty()) { append(text); return@buildAnnotatedString }
    var i = 0
    while (i < text.length) {
        val idx = text.indexOf(q, i, ignoreCase = true)
        if (idx < 0) { append(text.substring(i)); break }
        append(text.substring(i, idx))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(text.substring(idx, idx + q.length)) }
        i = idx + q.length
    }
}
