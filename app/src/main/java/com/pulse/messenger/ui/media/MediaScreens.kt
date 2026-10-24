package com.pulse.messenger.ui.media

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.components.EmptyState
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PulseTopBar
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.launch

private fun Message.mediaRes(): Int? = when (val c = content) {
    is MessageContent.Image -> c.res
    is MessageContent.Video -> c.thumb
    else -> null
}

@Composable
fun MediaViewerScreen(nav: NavController, chatId: String, messageId: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val messages by repo.messages.collectAsStateWithLifecycle()
    val media = remember(messages, chatId) {
        messages[chatId].orEmpty().filter { it.content is MessageContent.Image || it.content is MessageContent.Video }
    }
    if (media.isEmpty()) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val start = media.indexOfFirst { it.id == messageId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { media.size }
    var chrome by remember { mutableStateOf(true) }
    val current = media[pager.currentPage.coerceIn(0, media.lastIndex)]

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            val m = media[page]
            when (val c = m.content) {
                is MessageContent.Video -> VideoPage(c, active = pager.currentPage == page, onToggleChrome = { chrome = !chrome })
                else -> ZoomableImage(m.mediaRes() ?: R.drawable.media_photo_01, onTap = { chrome = !chrome })
            }
        }
        AnimatedVisibility(chrome, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(repo.displayName(current.senderId), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(Format.relative(current.timestamp), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = {
                    repo.setStarred(chatId, listOf(current.id), !current.isStarred)
                    snackbar.show(if (current.isStarred) "Message unstarred" else "Message starred", SnackType.SUCCESS)
                }) { Icon(if (current.isStarred) Icons.Filled.Star else Icons.Filled.StarOutline, "Star", tint = Color.White) }
                IconButton(onClick = { nav.navigate(Routes.forward(chatId, listOf(current.id))) }) {
                    Icon(Icons.AutoMirrored.Filled.Forward, "Forward", tint = Color.White)
                }
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Shared from Pulse")
                    }
                    context.startActivity(Intent.createChooser(send, "Share"))
                }) { Icon(Icons.Filled.Share, "Share", tint = Color.White) }
            }
        }
        AnimatedVisibility(chrome, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                val caption = when (val c = current.content) {
                    is MessageContent.Image -> c.caption
                    is MessageContent.Video -> c.caption
                    else -> ""
                }
                if (caption.isNotEmpty()) {
                    Text(caption, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${pager.currentPage + 1} of ${media.size}", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { nav.back() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reply", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(res: Int, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val anim = remember { Animatable(1f) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + pan * scale else Offset.Zero
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scope.launch {
                            val target = if (scale > 1.2f) 1f else 2.5f
                            anim.snapTo(scale)
                            anim.animateTo(target, tween(220)) { scale = value }
                            if (target == 1f) offset = Offset.Zero
                        }
                    },
                )
            }
            // Only claim pan gestures while zoomed so the pager can still swipe.
            .transformable(state, lockRotationOnZoomPan = true, canPan = { scale > 1f }),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = res, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offset.x; translationY = offset.y
            },
        )
    }
}

@Composable
private fun VideoPage(c: MessageContent.Video, active: Boolean, onToggleChrome: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    var seekKey by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) { if (!active) { playing = false; progress.snapTo(0f) } }
    LaunchedEffect(playing, seekKey) {
        if (playing) {
            if (progress.value >= 1f) progress.snapTo(0f)
            progress.animateTo(1f, tween(((1f - progress.value) * c.durationSec * 1000).toInt(), easing = LinearEasing))
            playing = false
        } else progress.stop()
    }
    Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { onToggleChrome() }, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = c.thumb, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                // Subtle Ken Burns motion stands in for real playback.
                val s = 1f + progress.value * 0.08f
                scaleX = s; scaleY = s
            },
        )
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable { playing = !playing },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Format.duration((progress.value * c.durationSec).toInt()), color = Color.White, fontSize = 12.sp)
            val scope = rememberCoroutineScope()
            Slider(
                value = progress.value,
                onValueChange = { v -> scope.launch { progress.snapTo(v) } },
                onValueChangeFinished = { seekKey++ },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = PulseTheme.colors.online, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text(Format.duration(c.durationSec), color = Color.White, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------- media, links and docs

@Composable
fun ChatMediaScreen(nav: NavController, chatId: String, initialTab: Int) {
    val repo = LocalRepo.current
    val uriHandler = LocalUriHandler.current
    val messages by repo.messages.collectAsStateWithLifecycle()
    val list = messages[chatId].orEmpty().asReversed()
    val media = list.filter { it.content is MessageContent.Image || it.content is MessageContent.Video }
    val docs = list.filter { it.content is MessageContent.Document }
    val links = list.filter { it.content is MessageContent.Link }
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val tabs = listOf("Media", "Docs", "Links")

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar(repo.chat(chatId)?.title ?: "Media", onBack = { nav.back() }) }
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.background,
            indicator = { pos ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = MaterialTheme.colorScheme.primary, height = 3.dp)
            },
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = tab == i, onClick = { tab = i },
                    text = { Text(t, fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Medium) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = PulseTheme.colors.textSecondary,
                )
            }
        }
        when (tab) {
            0 -> if (media.isEmpty()) {
                EmptyState(R.drawable.empty_media, "No media", "Photos and videos you share in this chat will appear here.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    items(media, key = { it.id }) { m ->
                        Box(Modifier.aspectRatio(1f).clickable { nav.navigate(Routes.viewer(chatId, m.id)) }) {
                            AsyncImage(model = m.mediaRes(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            (m.content as? MessageContent.Video)?.let { v ->
                                Row(Modifier.align(Alignment.BottomStart).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(Format.duration(v.durationSec), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            1 -> if (docs.isEmpty()) {
                EmptyState(R.drawable.empty_docs, "No documents", "Documents you share in this chat will appear here.")
            } else {
                LazyColumn(Modifier.navigationBarsPadding()) {
                    items(docs, key = { it.id }) { m ->
                        val d = m.content as MessageContent.Document
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(width = 44.dp, height = 52.dp).clip(RoundedCornerShape(8.dp)).background(PulseTheme.colors.chip),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (d.thumb != null) AsyncImage(model = d.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else Text(d.extension, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PulseTheme.colors.textSecondary)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${d.sizeLabel} · ${d.extension} · ${Format.chatListTime(m.timestamp)}", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }
            else -> if (links.isEmpty()) {
                EmptyState(R.drawable.empty_links, "No links", "Links you share in this chat will appear here.")
            } else {
                LazyColumn(Modifier.navigationBarsPadding()) {
                    items(links, key = { it.id }) { m ->
                        val l = m.content as MessageContent.Link
                        Row(
                            Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri(l.url) } }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(model = l.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(l.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(l.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
