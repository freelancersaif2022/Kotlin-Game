package com.pulse.messenger.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MockData
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ChatInputBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    replyTo: Message?,
    replyName: String,
    onCancelReply: () -> Unit,
    editing: Message?,
    onCancelEdit: () -> Unit,
    panelOpen: Boolean,
    onTogglePanel: () -> Unit,
    onFieldFocused: () -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onCamera: () -> Unit,
    onVoice: (Int) -> Unit,
    onVoiceTooShort: () -> Unit,
    enterIsSend: Boolean,
    focusRequester: FocusRequester,
) {
    val haptics = LocalHapticFeedback.current
    var recording by remember { mutableStateOf(false) }
    var recordStart by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val cancelPx = with(density) { 120.dp.toPx() }
    val hasText = value.text.isNotBlank()
    val currentOnVoice by rememberUpdatedState(onVoice)
    val currentOnTooShort by rememberUpdatedState(onVoiceTooShort)

    LaunchedEffect(recording) {
        if (recording) {
            elapsed = 0
            while (recording) {
                delay(250)
                elapsed = ((System.currentTimeMillis() - recordStart) / 1000).toInt()
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = PulseTheme.colors.inputField,
                shadowElevation = 1.dp,
                modifier = Modifier.weight(1f),
            ) {
                Column {
                    AnimatedVisibility(replyTo != null || editing != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        val target = editing ?: replyTo
                        if (target != null) {
                            if (editing != null) {
                                Row(
                                    Modifier.padding(start = 8.dp, end = 4.dp, top = 6.dp).fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp)).background(PulseTheme.colors.quoteBackground)
                                        .padding(start = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                        Text("Edit message", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                        Text(com.pulse.messenger.ui.home.previewText(editing.content), maxLines = 1, color = PulseTheme.colors.textSecondary, fontSize = 13.sp)
                                    }
                                    IconButton(onClick = onCancelEdit) { Icon(Icons.Filled.Close, "Cancel edit", tint = PulseTheme.colors.textSecondary) }
                                }
                            } else {
                                ReplyQuote(
                                    target, replyName,
                                    Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp).fillMaxWidth(),
                                    onClose = onCancelReply,
                                )
                            }
                        }
                    }
                    AnimatedContent(recording, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) }, label = "rec") { rec ->
                        if (rec) {
                            RecordingRow(elapsed, dragX)
                        } else {
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.heightIn(min = 50.dp)) {
                                IconButton(onClick = onTogglePanel) {
                                    Icon(
                                        if (panelOpen) Icons.Filled.Keyboard else Icons.Outlined.EmojiEmotions,
                                        if (panelOpen) "Keyboard" else "Emoji and stickers",
                                        tint = PulseTheme.colors.textSecondary,
                                    )
                                }
                                BasicTextField(
                                    value = value,
                                    onValueChange = onValueChange,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.5.sp),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    maxLines = 6,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = if (enterIsSend) ImeAction.Send else ImeAction.Default,
                                    ),
                                    keyboardActions = KeyboardActions(onSend = { if (hasText) onSend() }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 14.dp)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { if (it.isFocused) onFieldFocused() },
                                    decorationBox = { inner ->
                                        Box {
                                            if (value.text.isEmpty()) {
                                                Text("Message", color = PulseTheme.colors.textSecondary, fontSize = 16.5.sp)
                                            }
                                            inner()
                                        }
                                    },
                                )
                                IconButton(onClick = onAttach) {
                                    Icon(Icons.Outlined.AttachFile, "Attach", tint = PulseTheme.colors.textSecondary, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f))
                                }
                                AnimatedVisibility(!hasText && editing == null, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                                    IconButton(onClick = onCamera) { Icon(Icons.Outlined.CameraAlt, "Camera", tint = PulseTheme.colors.textSecondary) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            val showSend = hasText || editing != null
            val micScale by animateFloatAsState(if (recording) 1.35f else 1f, label = "mic")
            Box(
                Modifier
                    .size(50.dp)
                    .scale(micScale)
                    .offset { IntOffset(if (recording) dragX.roundToInt().coerceAtMost(0) / 3 else 0, 0) }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .then(
                        if (showSend) {
                            Modifier.clickable(onClick = onSend)
                        } else {
                            Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    recordStart = System.currentTimeMillis()
                                    dragX = 0f
                                    recording = true
                                    var cancelled = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        dragX = change.position.x - down.position.x
                                        change.consume()
                                        if (dragX < -cancelPx) {
                                            cancelled = true
                                            break
                                        }
                                    }
                                    val seconds = ((System.currentTimeMillis() - recordStart) / 1000).toInt()
                                    recording = false
                                    dragX = 0f
                                    when {
                                        cancelled -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        seconds < 1 -> currentOnTooShort()
                                        else -> currentOnVoice(seconds)
                                    }
                                }
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    when {
                        editing != null -> 2
                        showSend -> 1
                        else -> 0
                    },
                    transitionSpec = { (scaleIn(tween(160)) + fadeIn()) togetherWith (scaleOut(tween(120)) + fadeOut()) },
                    label = "sendmic",
                ) { mode ->
                    when (mode) {
                        2 -> Icon(Icons.Filled.Check, "Save edit", tint = Color.White)
                        1 -> Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White, modifier = Modifier.padding(start = 3.dp))
                        else -> Icon(Icons.Filled.Mic, "Hold to record", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(elapsed: Int, dragX: Float) {
    val t = rememberInfiniteTransition(label = "recdot")
    val alpha by t.animateFloat(1f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "reca")
    Row(
        Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Mic, null, tint = PulseTheme.colors.danger.copy(alpha = alpha))
        Spacer(Modifier.width(10.dp))
        Text(Format.duration(elapsed), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.offset { IntOffset((dragX.coerceAtMost(0f) * 0.6f).roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dragX < -60f) {
                Icon(Icons.Filled.Delete, null, tint = PulseTheme.colors.danger, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Outlined.ChevronLeft, null, tint = PulseTheme.colors.textSecondary)
            }
            Text("Slide to cancel", color = PulseTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(8.dp))
    }
}

// ---------------------------------------------------------------- emoji & sticker panel

private val emojiCategories: List<Pair<String, List<String>>> = listOf(
    "😀" to "😀 😃 😄 😁 😆 😅 😂 🤣 🥲 ☺️ 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥸 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🫣 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕".split(" "),
    "👍" to "👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🫰 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 🫶 👐 🤲 🤝 🙏 ✍️ 💅 🤳 💪 🦾 🧠 👀 👁️ 👅 👄 💋 🫦 👶 🧒 👦 👧 🧑 👱 👨 🧔 👩 🧓 👴 👵 🙍 🙎 🙅 🙆 💁 🙋 🧏 🙇 🤦 🤷".split(" "),
    "❤️" to "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❤️‍🔥 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ☮️ ✝️ ☪️ 🕉️ ☯️ ✡️ 🔯 🪯 ♈ ♉ ♊ ♋ ♌ ♍ ♎ ♏ ♐ ♑ ♒ ♓ ⛎ 🆗 🆒 🆕 🆙 ✅ ☑️ ✔️ ❌ ❎ ➕ ➖ ➗ ✖️ 💯 💢 ♨️ 💤 🔥 ✨ ⭐ 🌟 💫 ⚡ 💥".split(" "),
    "🐶" to "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐻‍❄️ 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐒 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🪱 🐛 🦋 🐌 🐞 🐜 🐢 🐍 🦎 🐙 🦑 🦐 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🦍 🐘 🦒 🦘 🐪 🐫 🌵 🎄 🌲 🌳 🌴 🌱 🌿 🍀 🍁 🍂 🌺 🌸 🌼 🌻 🌞 🌝 🌚 🌈 ☀️ ⛅ ☁️ 🌧️ ⛈️ ❄️ ☃️ 🌊".split(" "),
    "🍔" to "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶️ 🫑 🌽 🥕 🧄 🧅 🥔 🍠 🥐 🥯 🍞 🥖 🥨 🧀 🥚 🍳 🧈 🥞 🧇 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🍤 🍙 🍚 🍘 🍥 🥮 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 ☕ 🍵 🧃 🥤 🧋 🍺 🍻 🥂 🍷".split(" "),
    "⚽" to "⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🥏 🎱 🪀 🏓 🏸 🏒 🏑 🥍 🏏 🪃 🥅 ⛳ 🪁 🏹 🎣 🤿 🥊 🥋 🎽 🛹 🛼 🛷 ⛸️ 🥌 🎿 ⛷️ 🏂 🏋️ 🤼 🤸 ⛹️ 🤺 🤾 🏌️ 🏇 🧘 🏄 🏊 🤽 🚣 🧗 🚵 🚴 🏆 🥇 🥈 🥉 🏅 🎖️ 🎗️ 🎫 🎟️ 🎪 🤹 🎭 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🪕 🎻 🎲 ♟️ 🎯 🎳 🎮 🎰 🧩".split(" "),
    "🚗" to "🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🛻 🚚 🚛 🚜 🏍️ 🛵 🚲 🛴 🚨 🚔 🚍 🚘 🚖 ✈️ 🛫 🛬 🚀 🛸 🚁 🛶 ⛵ 🚤 🛥️ 🛳️ ⛴️ 🚢 ⚓ ⛽ 🚧 🚦 🗺️ 🗿 🗽 🗼 🏰 🏯 🏟️ 🎡 🎢 🎠 ⛲ ⛱️ 🏖️ 🏝️ 🏜️ 🌋 ⛰️ 🏔️ 🗻 🏕️ ⛺ 🏠 🏡 🏘️ 🏗️ 🏭 🏢 🏬 🏣 🏥 🏦 🏨 🏪 🏫 🏩 💒 🏛️ ⛪ 🕌 🕍 🛕 🌅 🌄 🌠 🎇 🎆 🌇 🌆 🏙️ 🌃 🌌 🌉".split(" "),
    "💡" to "⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💽 💾 💿 📀 📷 📸 📹 🎥 📞 ☎️ 📺 📻 🎙️ ⏱️ ⏰ 🕰️ ⌛ ⏳ 📡 🔋 🔌 💡 🔦 🕯️ 🧯 🛢️ 💸 💵 💰 💳 💎 ⚖️ 🧰 🔧 🔨 🛠️ ⛏️ 🔩 ⚙️ 🧱 ⛓️ 🧲 🔫 💣 🧨 🪓 🔪 🗡️ 🛡️ 🚬 ⚰️ 🏺 🔮 📿 🧿 💈 ⚗️ 🔭 🔬 🩹 🩺 💊 💉 🧬 🦠 🧫 🧪 🌡️ 🧹 🧺 🧻 🚽 🚿 🛁 🧼 🪥 🧽 🛎️ 🔑 🗝️ 🚪 🛋️ 🛏️ 🧸 🖼️ 🛍️ 🛒 🎁 🎈 🎏 🎀 🎊 🎉 🪅 🪩 🏮 📩 📨 📧 💌 📦 📫 📮 📜 📃 📄 📑 📊 📈 📉 🗒️ 📆 📅 📇 🗃️ 📋 📁 📂 📰 📓 📔 📒 📕 📗 📘 📙 📚 📖 🔖 🔗 📎 📐 📏 📌 📍 ✂️ 🖊️ 🖋️ ✒️ 🖌️ 🖍️ 📝 ✏️ 🔍 🔎 🔏 🔐 🔒 🔓".split(" "),
)

@Composable
fun EmojiStickerPanel(
    onEmoji: (String) -> Unit,
    onSticker: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableIntStateOf(0) } // 0 emoji, 1 stickers
    var category by remember { mutableIntStateOf(0) }
    var pack by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // Flat index of each category header inside the emoji grid.
    val headerIndex = remember {
        var acc = 0
        emojiCategories.map { (_, list) -> acc.also { acc += list.size + 1 } }
    }

    Column(modifier.fillMaxWidth().height(310.dp).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        if (mode == 0) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                itemsIndexed(emojiCategories) { i, (icon, _) ->
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (i == category) PulseTheme.colors.chipSelected else Color.Transparent)
                            .clickable {
                                category = i
                                scope.launch { gridState.animateScrollToItem(headerIndex[i]) }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(icon, fontSize = 20.sp) }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                state = gridState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                emojiCategories.forEachIndexed { ci, (_, list) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h$ci") {
                        Text(
                            listOf("Smileys", "People", "Symbols", "Animals & nature", "Food & drink", "Activities", "Travel & places", "Objects")[ci],
                            style = MaterialTheme.typography.labelMedium, color = PulseTheme.colors.textSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    items(list.size, key = { "e$ci-$it" }) { idx ->
                        val e = list[idx]
                        Box(
                            Modifier.aspectRatio(1f).clip(CircleShape).clickable { onEmoji(e) },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 26.sp) }
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                itemsIndexed(MockData.stickerTrays) { i, tray ->
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (i == pack) PulseTheme.colors.chipSelected else Color.Transparent)
                            .clickable { pack = i }
                            .padding(5.dp),
                    ) { AsyncImage(model = tray, contentDescription = MockData.stickerPacks[i].first) }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(MockData.stickerPacks[pack].first, style = MaterialTheme.typography.labelMedium, color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(4.dp))
                }
                items(MockData.stickerPacks[pack].second) { res ->
                    AsyncImage(
                        model = res, contentDescription = "Sticker",
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable { onSticker(res) }.padding(4.dp),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelTab("Emoji", mode == 0) { mode = 0 }
            Spacer(Modifier.width(8.dp))
            PanelTab("Stickers", mode == 1) { mode = 1 }
            Spacer(Modifier.weight(1f))
            if (mode == 0) {
                IconButton(onClick = onBackspace) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, "Backspace", tint = PulseTheme.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PanelTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) PulseTheme.colors.onChipSelected else PulseTheme.colors.textSecondary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PulseTheme.colors.chipSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

fun TextFieldValue.insert(text: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val newText = this.text.substring(0, start) + text + this.text.substring(end)
    return TextFieldValue(newText, TextRange(start + text.length))
}

fun TextFieldValue.backspace(): TextFieldValue {
    if (selection.collapsed && selection.start == 0) return this
    if (!selection.collapsed) return TextFieldValue(text.removeRange(selection.min, selection.max), TextRange(selection.min))
    val before = text.substring(0, selection.start)
    // Remove a whole grapheme so multi-codepoint emoji vanish in one tap.
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(before)
    val prev = it.preceding(before.length).coerceAtLeast(0)
    return TextFieldValue(before.substring(0, prev) + text.substring(selection.start), TextRange(prev))
}
