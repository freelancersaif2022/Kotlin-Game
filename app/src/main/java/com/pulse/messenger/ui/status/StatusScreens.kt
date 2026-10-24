package com.pulse.messenger.ui.status

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.data.MockData
import com.pulse.messenger.data.StatusItem
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PulseBottomSheet
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val ITEM_MS = 5500

@Composable
fun StatusViewerScreen(nav: NavController, startContactId: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val settings by LocalSettingsRepo.current.settings.collectAsStateWithLifecycle()
    val statuses by repo.statuses.collectAsStateWithLifecycle()
    val myStatus by repo.myStatus.collectAsStateWithLifecycle()
    val isMine = startContactId == "me"

    // Queue of people to play through, in the same order as the Updates tab.
    val queue = remember {
        if (isMine) listOf("me") else {
            val ordered = statuses.filter { !it.isMuted }.sortedWith(compareBy<com.pulse.messenger.data.StatusUpdate> { it.allViewed }.thenByDescending { it.latest.timestamp }).map { it.contactId }
            val start = ordered.indexOf(startContactId)
            if (start >= 0) ordered.drop(start) else listOf(startContactId)
        }
    }
    var personIndex by remember { mutableIntStateOf(0) }
    val personId = queue[personIndex.coerceIn(0, queue.lastIndex)]
    val items: List<StatusItem> = if (personId == "me") myStatus else statuses.firstOrNull { it.contactId == personId }?.items.orEmpty()
    var itemIndex by remember(personId) {
        val viewed = statuses.firstOrNull { it.contactId == personId }?.viewedCount ?: 0
        mutableIntStateOf(if (viewed in 1 until items.size) viewed else 0)
    }
    val progress = remember(personId) { Animatable(0f) }
    var paused by remember { mutableStateOf(false) }
    var replyFocused by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var viewersSheet by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val replyFocus = remember { FocusRequester() }

    if (items.isEmpty()) {
        LaunchedEffect(Unit) { nav.back() }
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val item = items[itemIndex.coerceIn(0, items.lastIndex)]
    val contact = repo.contact(personId)
    val name = if (personId == "me") "My status" else contact?.name.orEmpty()
    val avatar = if (personId == "me") settings.avatar else contact?.avatar

    fun next() {
        if (itemIndex < items.lastIndex) itemIndex++
        else if (personIndex < queue.lastIndex) personIndex++
        else nav.back()
    }

    fun prev() {
        if (itemIndex > 0) itemIndex--
        else if (personIndex > 0) personIndex--
        else scope.launch { progress.snapTo(0f) }
    }

    LaunchedEffect(personId, itemIndex) {
        progress.snapTo(0f)
        if (personId != "me") repo.markStatusViewed(personId, itemIndex + 1)
    }
    LaunchedEffect(personId, itemIndex, paused, replyFocused, viewersSheet, menu, deleteDialog) {
        if (paused || replyFocused || viewersSheet || menu || deleteDialog) return@LaunchedEffect
        val remaining = ((1f - progress.value) * ITEM_MS).toInt()
        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        next()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .offset { IntOffset(0, dragY.coerceAtLeast(0f).roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { if (dragY > 220f) nav.back() else dragY = 0f },
                    onDragCancel = { dragY = 0f },
                ) { _, amount -> dragY += amount }
            }
            .pointerInput(personId) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                    },
                    onTap = { pos -> if (pos.x < size.width * 0.3f) prev() else next() },
                )
            },
    ) {
        AnimatedContent(item.id, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) }, label = "status") { _ ->
            StatusContent(item)
        }
        // Top chrome
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                .statusBarsPadding()
                .padding(top = 6.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                items.forEachIndexed { i, _ ->
                    val fraction = when {
                        i < itemIndex -> 1f
                        i == itemIndex -> progress.value
                        else -> 0f
                    }
                    Box(Modifier.weight(1f).height(2.5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Color.White))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Avatar(avatar, name, 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(Format.relative(item.timestamp), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                        if (personId == "me") {
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; deleteDialog = true })
                            DropdownMenuItem(text = { Text("Status privacy") }, onClick = { menu = false; nav.navigate(Routes.PRIVACY) })
                        } else {
                            DropdownMenuItem(text = { Text("Mute") }, onClick = {
                                menu = false
                                repo.setStatusMuted(personId, true)
                                snackbar.show("$name's status updates muted", SnackType.SUCCESS)
                                next()
                            })
                            DropdownMenuItem(text = { Text("View contact") }, onClick = { menu = false; nav.navigate(Routes.contact(personId)) })
                            DropdownMenuItem(text = { Text("Message") }, onClick = { menu = false; nav.navigate(Routes.chat(repo.directChatFor(personId).id)) })
                        }
                    }
                }
            }
        }
        // Bottom chrome
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (item.caption.isNotEmpty()) {
                Text(item.caption, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 14.dp))
            }
            if (personId == "me") {
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).clickable { viewersSheet = true }.padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.KeyboardArrowUp, null, tint = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${item.viewerIds.size}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                if (!replyFocused) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        listOf("😍", "😂", "😮", "👏", "🔥", "🙏").forEach { e ->
                            Text(
                                e, fontSize = 26.sp,
                                modifier = Modifier.clip(CircleShape).clickable {
                                    sendStatusReply(repo, personId, item, e)
                                    snackbar.show("Reaction sent to $name", SnackType.SUCCESS)
                                }.padding(4.dp),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        BasicTextField(
                            reply, { reply = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth().focusRequester(replyFocus).onFocusChanged { replyFocused = it.isFocused },
                            decorationBox = { inner ->
                                if (reply.isEmpty()) Text("Reply…", color = Color.White.copy(alpha = 0.7f))
                                inner()
                            },
                        )
                    }
                    if (reply.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                sendStatusReply(repo, personId, item, reply.trim())
                                reply = ""
                                snackbar.show("Reply sent to $name", SnackType.SUCCESS)
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp),
                        ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                    }
                }
            }
        }
    }

    if (viewersSheet) {
        PulseBottomSheet(onDismiss = { viewersSheet = false }) {
            Text("Viewed by ${item.viewerIds.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            if (item.viewerIds.isEmpty()) {
                Text("No views yet", color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(24.dp))
            }
            item.viewerIds.asReversed().forEachIndexed { i, id ->
                val c = repo.contact(id)
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c?.avatar, c?.name.orEmpty(), 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c?.name ?: id, style = MaterialTheme.typography.bodyLarge)
                        Text(Format.relative(item.timestamp + (i + 1) * 4 * 60_000L), style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (deleteDialog) {
        ConfirmDialog(
            title = "Delete status update?",
            message = "This status update will be deleted for everyone who received it.",
            confirmText = "Delete", destructive = true,
            onConfirm = {
                repo.deleteMyStatus(item.id)
                snackbar.show("Status deleted", SnackType.SUCCESS)
                if (items.size <= 1) nav.back() else if (itemIndex >= items.lastIndex) itemIndex--
            },
            onDismiss = { deleteDialog = false },
        )
    }
}

private fun sendStatusReply(repo: com.pulse.messenger.data.PulseRepository, contactId: String, item: StatusItem, text: String) {
    val chat = repo.directChatFor(contactId)
    val context = item.caption.ifEmpty { item.text }.take(40)
    val prefix = if (context.isNotEmpty()) "Replying to your status “$context”\n" else "Replying to your status\n"
    repo.send(chat.id, MessageContent.Text(prefix + text))
}

@Composable
private fun StatusContent(item: StatusItem) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (item.image != null) {
            AsyncImage(model = item.image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            if (item.background != null) {
                AsyncImage(model = item.background, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary))
            }
            Text(
                item.text,
                color = Color.White,
                fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
    }
}

private val fonts = listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace, FontFamily.Cursive)

@Composable
fun TextStatusScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    var text by remember { mutableStateOf("") }
    var bg by remember { mutableIntStateOf(0) }
    var font by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(bg, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) }, label = "bg") { i ->
            AsyncImage(model = MockData.statusBackgrounds[i], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.back() }) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { font = (font + 1) % fonts.size }) { Icon(Icons.Filled.TextFields, "Font", tint = Color.White) }
            IconButton(onClick = { bg = (bg + 1) % MockData.statusBackgrounds.size }) { Icon(Icons.Filled.Palette, "Background", tint = Color.White) }
        }
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= 700) text = it },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = Color.White, textAlign = TextAlign.Center, fontFamily = fonts[font],
                fontSize = if (text.length > 120) 24.sp else 32.sp, lineHeight = if (text.length > 120) 32.sp else 42.sp,
            ),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(28.dp).focusRequester(focus),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) {
                        Text("Type a status", color = Color.White.copy(alpha = 0.6f), fontSize = 32.sp, fontFamily = fonts[font], textAlign = TextAlign.Center)
                    }
                    inner()
                }
            },
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Status (My contacts)",
                color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.3f)).clickable { nav.navigate(Routes.PRIVACY) }.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            if (text.isNotBlank()) {
                FloatingActionButton(
                    onClick = {
                        repo.postStatus(StatusItem("", System.currentTimeMillis(), text = text.trim(), background = MockData.statusBackgrounds[bg]))
                        snackbar.show("Status shared with your contacts", SnackType.SUCCESS)
                        nav.back()
                    },
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape,
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send status") }
            }
        }
    }
}
