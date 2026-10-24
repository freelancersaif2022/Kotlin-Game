package com.pulse.messenger.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulse.messenger.data.ME
import com.pulse.messenger.data.Message
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.MessageTicks
import com.pulse.messenger.ui.home.previewIcon
import com.pulse.messenger.ui.home.previewText
import com.pulse.messenger.ui.theme.LocalMessageTextStyle
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay
import kotlin.math.max

val TailWidth = 8.dp

/** Bubble outline with an optional pointed tail in the top corner on the sender's side. */
class BubbleShape(private val isMine: Boolean, private val hasTail: Boolean, private val radius: Dp = 12.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        val tw = with(density) { TailWidth.toPx() }
        val path = Path()
        if (!hasTail) {
            path.addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
            return Outline.Generic(path)
        }
        val mineSide = isMine == (layoutDirection == LayoutDirection.Ltr)
        if (mineSide) {
            path.addRoundRect(
                RoundRect(
                    left = 0f, top = 0f, right = size.width - tw, bottom = size.height,
                    topLeftCornerRadius = CornerRadius(r), topRightCornerRadius = CornerRadius.Zero,
                    bottomRightCornerRadius = CornerRadius(r), bottomLeftCornerRadius = CornerRadius(r),
                ),
            )
            val tail = Path().apply {
                moveTo(size.width - tw - 1f, 0f)
                lineTo(size.width - 2f, 0f)
                quadraticTo(size.width, 0f, size.width - 1.5f, 2.5f)
                lineTo(size.width - tw - 1f, tw * 1.35f)
                close()
            }
            path.addPath(tail)
        } else {
            path.addRoundRect(
                RoundRect(
                    left = tw, top = 0f, right = size.width, bottom = size.height,
                    topLeftCornerRadius = CornerRadius.Zero, topRightCornerRadius = CornerRadius(r),
                    bottomRightCornerRadius = CornerRadius(r), bottomLeftCornerRadius = CornerRadius(r),
                ),
            )
            val tail = Path().apply {
                moveTo(tw + 1f, 0f)
                lineTo(2f, 0f)
                quadraticTo(0f, 0f, 1.5f, 2.5f)
                lineTo(tw + 1f, tw * 1.35f)
                close()
            }
            path.addPath(tail)
        }
        return Outline.Generic(path)
    }
}

@Immutable
data class BubbleInfo(
    val message: Message,
    val replyTo: Message?,
    val replyToName: String?,
    val senderName: String?,
    val senderColor: Color,
    val senderAvatar: Int?,
    val isFirst: Boolean,
    val isLast: Boolean,
    val isGroup: Boolean,
)

private val nameColors = listOf(
    Color(0xFF1FA855), Color(0xFFE5793B), Color(0xFF5B6EE1), Color(0xFFD9475F), Color(0xFF2F9BD6),
    Color(0xFFB35BD1), Color(0xFF14A38B), Color(0xFFC79A1E), Color(0xFF7F66FF), Color(0xFFE0558C),
)

fun senderColor(id: String): Color = nameColors[(id.hashCode() and 0x7fffffff) % nameColors.size]

@Composable
fun SystemChip(text: String, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Box(modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PulseTheme.colors.systemBubble)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = PulseTheme.colors.systemBubbleText, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
                color = PulseTheme.colors.systemBubbleText, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
fun DayChip(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
            color = PulseTheme.colors.textSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (PulseTheme.colors.isDark) Color(0xFF182229) else Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun EncryptionBanner(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PulseTheme.colors.systemBubble)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Outlined.Lock, null, tint = PulseTheme.colors.systemBubbleText, modifier = Modifier.padding(top = 2.dp).size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Messages and calls are end-to-end encrypted. Only people in this chat can read, listen to, or share them. Tap to learn more.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
                color = PulseTheme.colors.systemBubbleText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private val urlRegex = Regex("""(https?://[^\s]+)""")

fun linkify(text: String, highlight: String?, linkColor: Color, highlightBg: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in urlRegex.findAll(text)) {
        appendHighlighted(text.substring(last, m.range.first), highlight, highlightBg)
        withLink(LinkAnnotation.Url(m.value, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)))) {
            append(m.value)
        }
        last = m.range.last + 1
    }
    appendHighlighted(text.substring(last), highlight, highlightBg)
}

private fun AnnotatedString.Builder.appendHighlighted(s: String, q: String?, bg: Color) {
    if (q.isNullOrBlank()) { append(s); return }
    var i = 0
    while (i < s.length) {
        val idx = s.indexOf(q, i, ignoreCase = true)
        if (idx < 0) { append(s.substring(i)); break }
        append(s.substring(i, idx))
        withStyle(SpanStyle(background = bg)) { append(s.substring(idx, idx + q.length)) }
        i = idx + q.length
    }
}

/** Text with the time/ticks placed inline on the last line when it fits, otherwise below. */
@Composable
fun TextWithMeta(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    meta: @Composable () -> Unit,
) {
    val holder = remember { arrayOfNulls<TextLayoutResult>(1) }
    Layout(
        content = {
            Text(text, style = style, color = color, onTextLayout = { holder[0] = it })
            meta()
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val textP = measurables[0].measure(constraints.copy(minWidth = 0))
        val metaP = measurables[1].measure(Constraints())
        val result = holder[0]
        val gap = 8.dp.roundToPx()
        val lastRight = result?.let { if (it.lineCount > 0) it.getLineRight(it.lineCount - 1).toInt() else 0 } ?: textP.width
        val inline = lastRight + gap + metaP.width <= constraints.maxWidth
        val width: Int
        val height: Int
        if (inline) {
            width = max(textP.width, lastRight + gap + metaP.width).coerceAtLeast(constraints.minWidth)
            height = max(textP.height, metaP.height)
        } else {
            width = max(textP.width, metaP.width).coerceAtLeast(constraints.minWidth)
            height = textP.height + metaP.height
        }
        layout(width, height) {
            textP.place(0, 0)
            metaP.place(width - metaP.width, height - metaP.height + 2.dp.roundToPx())
        }
    }
}

@Composable
fun MessageMeta(message: Message, color: Color, onMedia: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onMedia) Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 6.dp, vertical = 2.dp) else Modifier,
    ) {
        val c = if (onMedia) Color.White else color
        if (message.isStarred) {
            Icon(Icons.Filled.Star, "Starred", tint = c, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
        }
        if (message.isEdited) {
            Text("Edited", fontSize = 11.sp, color = c)
            Spacer(Modifier.width(4.dp))
        }
        Text(Format.clock(message.timestamp), fontSize = 11.sp, color = c)
        if (message.isMine) {
            Spacer(Modifier.width(3.dp))
            MessageTicks(message.status, size = 16.dp, baseColor = c)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    info: BubbleInfo,
    maxWidth: Dp,
    highlight: String?,
    onClickContent: () -> Unit,
    onLongClick: () -> Unit,
    onReplyClick: (String) -> Unit,
    onReactionsClick: () -> Unit,
    onVote: (Int) -> Unit,
    onContactMessage: (String) -> Unit,
    selectionMode: Boolean,
) {
    val m = info.message
    val colors = PulseTheme.colors
    val isMine = m.isMine
    val content = m.content
    val isSticker = content is MessageContent.Sticker
    val bg = if (isMine) colors.bubbleOut else colors.bubbleIn
    val textColor = if (isMine) colors.bubbleOutText else colors.bubbleInText
    val metaColor = colors.bubbleMeta
    val textStyle = LocalMessageTextStyle.current

    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        val shapeMod = if (isSticker) Modifier else {
            val shape = BubbleShape(isMine, info.isFirst)
            Modifier
                .then(if (!info.isFirst) Modifier.padding(start = if (isMine) 0.dp else TailWidth, end = if (isMine) TailWidth else 0.dp) else Modifier)
                .clip(shape)
                .background(bg)
                .then(if (info.isFirst) Modifier.padding(start = if (isMine) 0.dp else TailWidth, end = if (isMine) TailWidth else 0.dp) else Modifier)
        }
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .then(shapeMod),
        ) {
            Column(Modifier.width(IntrinsicSize.Max)) {
                if (info.isGroup && !isMine && info.isFirst && info.senderName != null && !isSticker) {
                    Text(
                        info.senderName,
                        color = info.senderColor,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 9.dp, end = 9.dp, top = 6.dp),
                    )
                }
                if (m.isForwarded && content !is MessageContent.Deleted) {
                    Row(Modifier.padding(start = 9.dp, end = 9.dp, top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Forward, null, tint = metaColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Forwarded", color = metaColor, fontSize = 12.5.sp, fontStyle = FontStyle.Italic)
                    }
                }
                if (info.replyTo != null && content !is MessageContent.Deleted) {
                    ReplyQuote(
                        info.replyTo, info.replyToName.orEmpty(),
                        Modifier
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                            .fillMaxWidth()
                            .clickable { onReplyClick(info.replyTo.id) },
                    )
                }
                Box(
                    Modifier.combinedClickable(
                        onClick = onClickContent,
                        onLongClick = onLongClick,
                        indication = null,
                        interactionSource = null,
                    ),
                ) {
                    BubbleContent(
                        m, textColor, metaColor, textStyle, highlight, maxWidth,
                        avatar = info.senderAvatar, name = info.senderName ?: "You",
                        onVote = onVote, onContactMessage = onContactMessage, selectionMode = selectionMode,
                    )
                }
            }
        }
        if (m.reactions.isNotEmpty()) {
            ReactionPill(
                m.reactions, onClick = onReactionsClick,
                modifier = Modifier
                    .padding(start = if (isMine) 0.dp else TailWidth + 6.dp, end = if (isMine) TailWidth + 6.dp else 0.dp)
                    .layoutOffset((-6).dp),
            )
        }
    }
}

/** Shifts content vertically and gives the overlapped space back to the parent. */
private fun Modifier.layoutOffset(y: Dp): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val p = measurable.measure(constraints)
        val dy = y.roundToPx()
        layout(p.width, (p.height + dy).coerceAtLeast(0)) { p.place(0, dy) }
    },
)

@Composable
private fun ReactionPill(reactions: Map<String, String>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val grouped = remember(reactions) { reactions.values.groupingBy { it }.eachCount().entries.sortedByDescending { it.value } }
    val mine = ME in reactions
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (PulseTheme.colors.isDark) Color(0xFF233138) else Color.White)
            .border(1.5.dp, PulseTheme.colors.chatBackground, RoundedCornerShape(14.dp))
            .then(if (mine) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        grouped.take(3).forEach { Text(it.key, fontSize = 14.sp) }
        if (reactions.size > 1) {
            Spacer(Modifier.width(3.dp))
            Text(reactions.size.toString(), fontSize = 12.sp, color = PulseTheme.colors.textSecondary)
        }
    }
}

@Composable
fun ReplyQuote(reply: Message, name: String, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    val color = if (reply.senderId == ME) MaterialTheme.colorScheme.primary else senderColor(reply.senderId)
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PulseTheme.colors.quoteBackground)
            .height(IntrinsicSize.Min),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(name, color = color, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                previewIcon(reply.content)?.let {
                    Icon(it, null, tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    previewText(reply.content), fontSize = 13.5.sp, color = PulseTheme.colors.textSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val thumb = when (val c = reply.content) {
            is MessageContent.Image -> c.res
            is MessageContent.Video -> c.thumb
            is MessageContent.Sticker -> c.res
            is MessageContent.Location -> c.map
            is MessageContent.Link -> c.image
            else -> null
        }
        if (thumb != null) {
            AsyncImage(
                model = thumb, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp),
            )
        }
        if (onClose != null) {
            Box(Modifier.size(36.dp).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Cancel reply", tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BubbleContent(
    m: Message,
    textColor: Color,
    metaColor: Color,
    textStyle: TextStyle,
    highlight: String?,
    maxWidth: Dp,
    avatar: Int?,
    name: String,
    onVote: (Int) -> Unit,
    onContactMessage: (String) -> Unit,
    selectionMode: Boolean,
) {
    val colors = PulseTheme.colors
    val linkColor = if (colors.isDark) Color(0xFF53BDEB) else Color(0xFF027EB5)
    val hlBg = Color(0x66FFD54F)
    when (val c = m.content) {
        is MessageContent.Text -> {
            val emojiOnly = remember(c.text) { isEmojiOnly(c.text) }
            val annotated = remember(c.text, highlight, linkColor) { linkify(c.text, highlight, linkColor, hlBg) }
            TextWithMeta(
                annotated,
                if (emojiOnly) textStyle.copy(fontSize = 38.sp, lineHeight = 46.sp) else textStyle,
                textColor,
                Modifier.padding(start = 9.dp, end = 8.dp, top = 6.dp, bottom = 5.dp),
            ) { MessageMeta(m, metaColor) }
        }
        is MessageContent.Image -> MediaBlock(c.res, c.caption, m, maxWidth, textColor, metaColor, textStyle, highlight, video = null)
        is MessageContent.Video -> MediaBlock(c.thumb, c.caption, m, maxWidth, textColor, metaColor, textStyle, highlight, video = c.durationSec)
        is MessageContent.Voice -> VoiceContent(m, c, metaColor, avatar, name)
        is MessageContent.Document -> DocumentContent(m, c, textColor, metaColor)
        is MessageContent.Location -> LocationContent(m, c, maxWidth, textColor, metaColor)
        is MessageContent.ContactCard -> ContactContent(m, c, textColor, metaColor, onContactMessage)
        is MessageContent.Sticker -> Column(horizontalAlignment = Alignment.End) {
            AsyncImage(model = c.res, contentDescription = "Sticker", modifier = Modifier.size(150.dp))
            MessageMeta(m, metaColor, onMedia = true)
        }
        is MessageContent.Link -> LinkContent(m, c, maxWidth, textColor, metaColor, textStyle, highlight, linkColor)
        is MessageContent.Poll -> PollContent(m, c, textColor, metaColor, onVote, selectionMode)
        is MessageContent.Deleted -> TextWithMeta(
            AnnotatedString(if (m.isMine) "You deleted this message" else "This message was deleted"),
            textStyle.copy(fontStyle = FontStyle.Italic), colors.textSecondary,
            Modifier.padding(start = 9.dp, end = 8.dp, top = 6.dp, bottom = 5.dp),
        ) { MessageMeta(m.copy(isStarred = false, isEdited = false), metaColor) }
        is MessageContent.System -> Unit
    }
}

private fun isEmojiOnly(text: String): Boolean {
    val t = text.trim()
    if (t.isEmpty() || t.length > 12) return false
    return t.codePoints().toArray().all { cp ->
        Character.getType(cp) == Character.OTHER_SYMBOL.toInt() || cp == 0x200D || cp == 0xFE0F || cp in 0x1F3FB..0x1F3FF || Character.isWhitespace(cp)
    }
}

@Composable
private fun MediaBlock(
    res: Int,
    caption: String,
    m: Message,
    maxWidth: Dp,
    textColor: Color,
    metaColor: Color,
    textStyle: TextStyle,
    highlight: String?,
    video: Int?,
) {
    Column(Modifier.width(maxWidth.coerceAtMost(290.dp))) {
        Box(
            Modifier
                .padding(3.dp)
                .fillMaxWidth()
                .heightIn(max = 340.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            AsyncImage(
                model = res, contentDescription = if (video != null) "Video" else "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(if (video != null) 16f / 10f else 4f / 4.2f),
            )
            if (video != null) {
                Box(
                    Modifier.align(Alignment.Center).size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Videocam, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Format.duration(video), color = Color.White, fontSize = 11.sp)
                }
            }
            if (caption.isEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth(0.6f)
                        .height(36.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)))),
                )
                Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) { MessageMeta(m, metaColor, onMedia = true) }
            }
        }
        if (caption.isNotEmpty()) {
            TextWithMeta(
                linkify(caption, highlight, Color(0xFF027EB5), Color(0x66FFD54F)), textStyle, textColor,
                Modifier.padding(start = 7.dp, end = 7.dp, top = 2.dp, bottom = 5.dp),
            ) { MessageMeta(m, metaColor) }
        }
    }
}

@Composable
private fun VoiceContent(m: Message, c: MessageContent.Voice, metaColor: Color, avatar: Int?, name: String) {
    var playing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            if (progress.value >= 1f) progress.snapTo(0f)
            val remaining = ((1f - progress.value) * c.durationSec * 1000).toInt()
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            playing = false
            delay(300)
            progress.snapTo(0f)
        } else {
            progress.stop()
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.width(260.dp).padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(avatar, name, 46.dp)
            Icon(
                Icons.Filled.Mic, null,
                tint = if (m.isMine && m.status == com.pulse.messenger.data.MessageStatus.READ) PulseTheme.colors.readTick else accent,
                modifier = Modifier.align(Alignment.BottomEnd).size(18.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(38.dp).clip(CircleShape).clickable { playing = !playing },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", tint = metaColor, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f)) {
            val played = PulseTheme.colors.readTick
            val idle = metaColor.copy(alpha = 0.45f)
            Canvas(Modifier.fillMaxWidth().height(30.dp)) {
                val bars = c.waveform.size
                val gap = 2.dp.toPx()
                val barW = (size.width - gap * (bars - 1)) / bars
                c.waveform.forEachIndexed { i, v ->
                    val h = (v * size.height).coerceAtLeast(3.dp.toPx())
                    val x = i * (barW + gap)
                    val p = (i + 0.5f) / bars
                    drawLine(
                        color = if (p <= progress.value) played else idle,
                        start = Offset(x + barW / 2, (size.height - h) / 2),
                        end = Offset(x + barW / 2, (size.height + h) / 2),
                        strokeWidth = barW, cap = StrokeCap.Round,
                    )
                }
                val dotX = progress.value * size.width
                drawCircle(played, radius = 6.dp.toPx(), center = Offset(dotX.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx()), size.height / 2))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val shown = if (playing || progress.value > 0f) (progress.value * c.durationSec).toInt() else c.durationSec
                Text(Format.duration(shown), fontSize = 11.sp, color = metaColor)
                Spacer(Modifier.weight(1f))
                MessageMeta(m, metaColor)
            }
        }
    }
}

@Composable
private fun DocumentContent(m: Message, c: MessageContent.Document, textColor: Color, metaColor: Color) {
    Column(Modifier.width(270.dp).padding(4.dp)) {
        if (c.thumb != null) {
            AsyncImage(
                model = c.thumb, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = if (c.thumb != null) 0.dp else 8.dp, topEnd = if (c.thumb != null) 0.dp else 8.dp,
                        bottomStart = 8.dp, bottomEnd = 8.dp,
                    ),
                )
                .background(PulseTheme.colors.quoteBackground)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val badgeColor = when (c.extension) {
                "PDF" -> Color(0xFFE5484D)
                "DOCX", "DOC" -> Color(0xFF2F6FDE)
                "XLSX" -> Color(0xFF1DAA61)
                "PPTX" -> Color(0xFFE5793B)
                else -> Color(0xFF667781)
            }
            Box(
                Modifier.size(width = 32.dp, height = 38.dp).clip(RoundedCornerShape(5.dp)).background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(c.extension.take(4), color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(if (c.pages > 0) "${c.pages} page${if (c.pages > 1) "s" else ""}" else null, c.extension, c.sizeLabel).joinToString(" · "),
                    color = metaColor, fontSize = 12.sp,
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp), contentAlignment = Alignment.CenterEnd) { MessageMeta(m, metaColor) }
    }
}

@Composable
private fun LocationContent(m: Message, c: MessageContent.Location, maxWidth: Dp, textColor: Color, metaColor: Color) {
    Column(Modifier.width(maxWidth.coerceAtMost(280.dp)).padding(3.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))) {
            AsyncImage(model = c.map, contentDescription = "Map", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Icon(
                if (c.isLive) Icons.Outlined.MyLocation else Icons.Filled.Place,
                null, tint = if (c.isLive) MaterialTheme.colorScheme.primary else Color(0xFFE5484D),
                modifier = Modifier.align(Alignment.Center).size(40.dp),
            )
            if (c.isLive) {
                val t = rememberInfiniteTransition(label = "live")
                val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "livea")
                Row(
                    Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34D1B6).copy(alpha = a)))
                    Spacer(Modifier.width(5.dp))
                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            Text(c.title, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(c.address, color = metaColor, fontSize = 12.5.sp)
        }
        Box(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 2.dp), contentAlignment = Alignment.CenterEnd) { MessageMeta(m, metaColor) }
    }
}

@Composable
private fun ContactContent(m: Message, c: MessageContent.ContactCard, textColor: Color, metaColor: Color, onMessage: (String) -> Unit) {
    Column(Modifier.width(250.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.avatar, c.name, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, color = textColor, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                Text(c.phone, color = metaColor, fontSize = 12.5.sp)
            }
        }
        Box(Modifier.fillMaxWidth().padding(end = 8.dp), contentAlignment = Alignment.CenterEnd) { MessageMeta(m, metaColor) }
        HorizontalDivider(color = metaColor.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
        Box(
            Modifier.fillMaxWidth().clickable { onMessage(c.phone) }.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Message", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun LinkContent(
    m: Message,
    c: MessageContent.Link,
    maxWidth: Dp,
    textColor: Color,
    metaColor: Color,
    textStyle: TextStyle,
    highlight: String?,
    linkColor: Color,
) {
    Column(Modifier.width(maxWidth.coerceAtMost(300.dp))) {
        Column(
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PulseTheme.colors.quoteBackground),
        ) {
            AsyncImage(
                model = c.image, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.91f),
            )
            Column(Modifier.padding(10.dp)) {
                Text(c.title, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(c.description, color = metaColor, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(c.url.removePrefix("https://").substringBefore("/"), color = metaColor, fontSize = 11.5.sp)
            }
        }
        TextWithMeta(
            linkify(c.text, highlight, linkColor, Color(0x66FFD54F)), textStyle, textColor,
            Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 5.dp),
        ) { MessageMeta(m, metaColor) }
    }
}

@Composable
private fun PollContent(m: Message, c: MessageContent.Poll, textColor: Color, metaColor: Color, onVote: (Int) -> Unit, selectionMode: Boolean) {
    val total = c.options.sumOf { it.voterIds.size }.coerceAtLeast(1)
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.width(280.dp).padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp)) {
        Text(c.question, color = textColor, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Text(if (c.allowMultiple) "Select one or more" else "Select one", color = metaColor, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        c.options.forEachIndexed { i, o ->
            val mine = ME in o.voterIds
            val fraction = o.voterIds.size / total.toFloat()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !selectionMode) { onVote(i) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(if (c.allowMultiple) RoundedCornerShape(5.dp) else CircleShape)
                        .then(if (mine) Modifier.background(accent) else Modifier.border(2.dp, metaColor, if (c.allowMultiple) RoundedCornerShape(5.dp) else CircleShape)),
                    contentAlignment = Alignment.Center,
                ) { if (mine) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(o.text, color = textColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(o.voterIds.size.toString(), color = metaColor, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(metaColor.copy(alpha = 0.2f))) {
                        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(accent))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${c.options.sumOf { it.voterIds.size }} votes", color = metaColor, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            MessageMeta(m, metaColor)
        }
    }
}

/** Animated "typing…" bubble shown at the bottom of the conversation. */
@Composable
fun TypingBubble(name: String?) {
    val t = rememberInfiniteTransition(label = "typing")
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        Box(Modifier.clip(BubbleShape(isMine = false, hasTail = true)).background(PulseTheme.colors.bubbleIn).padding(start = TailWidth)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (name != null) Text(name, fontSize = 12.sp, color = PulseTheme.colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(14.dp)) {
                    repeat(3) { i ->
                        val a by t.animateFloat(
                            0.25f, 1f,
                            infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse), label = "dot$i",
                        )
                        Box(Modifier.size(7.dp).clip(CircleShape).background(PulseTheme.colors.textSecondary.copy(alpha = a)))
                    }
                }
            }
        }
    }
}
