package com.smartisland.app.island.ui

import android.graphics.Bitmap
import android.media.AudioManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisland.app.island.IslandActivity
import kotlinx.coroutines.delay

object IslandColors {
    const val GREEN_ARGB = 0xFF30D158.toInt()
    const val RED_ARGB = 0xFFFF453A.toInt()
    val Green = Color(GREEN_ARGB)
    val Red = Color(RED_ARGB)
    val Orange = Color(0xFFFF9F0A)
    val Gray = Color(0xFF8E8E93)
    val Track = Color(0x33FFFFFF)
}

private val White = Color.White
private val Title = TextStyle(color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
private val Sub = TextStyle(color = IslandColors.Gray, fontSize = 13.sp)
private val CompactLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

@Composable
internal fun IslandContent(activity: IslandActivity, mode: IslandMode, baseHeight: Dp, actions: IslandActions, accent: Color) {
    when (activity) {
        IslandActivity.Idle -> Box(Modifier.fillMaxSize())
        is IslandActivity.Media ->
            if (mode == IslandMode.EXPANDED) MediaExpanded(activity, actions) else MediaCompact(activity, baseHeight)
        is IslandActivity.Call ->
            if (mode == IslandMode.EXPANDED) CallExpanded(activity, actions) else CallCompact(activity, baseHeight)
        is IslandActivity.Charging ->
            if (mode == IslandMode.EXPANDED) ChargingExpanded(activity) else ChargingCompact(activity, baseHeight)
        is IslandActivity.LowBattery -> CompactRow(baseHeight) {
            Text("Low Battery", style = CompactLabel.copy(color = IslandColors.Red))
            BatteryWithLevel(activity.level, IslandColors.Red)
        }
        is IslandActivity.Volume -> VolumeCompact(activity, baseHeight, accent)
        is IslandActivity.Ringer -> RingerCompact(activity, baseHeight, accent)
        is IslandActivity.Notice -> NoticeExpanded(activity, accent)
        is IslandActivity.Info -> CompactRow(baseHeight) {
            Text(activity.label, style = CompactLabel.copy(color = accent))
            Text(activity.detail, style = CompactLabel.copy(color = IslandColors.Gray))
        }
    }
}

// Compact building blocks ----------------------------------------------------------------------

@Composable
private fun CompactRow(baseHeight: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = (baseHeight * 0.32f).coerceAtLeast(10.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun MediaCompact(m: IslandActivity.Media, baseHeight: Dp) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = (baseHeight * 0.18f).coerceAtLeast(6.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(m.art, (baseHeight - 10.dp).coerceAtLeast(16.dp), corner = 7.dp, accent = Color(m.accent))
        Waveform(Color(m.accent), m.isPlaying, barHeight = (baseHeight * 0.5f).coerceAtLeast(10.dp))
    }
}

@Composable
private fun CallCompact(c: IslandActivity.Call, baseHeight: Dp) {
    CompactRow(baseHeight) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Call, null, tint = IslandColors.Green, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(callTimer(c), style = CompactLabel.copy(color = IslandColors.Green))
        }
        Waveform(IslandColors.Green, playing = true, barHeight = (baseHeight * 0.45f).coerceAtLeast(10.dp))
    }
}

@Composable
private fun ChargingCompact(ch: IslandActivity.Charging, baseHeight: Dp) {
    val color = if (ch.isCharging) IslandColors.Green else White
    CompactRow(baseHeight) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ch.isCharging) Icon(Icons.Rounded.Bolt, null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                when {
                    !ch.isCharging -> "Unplugged"
                    ch.isFast -> "Fast Charging"
                    else -> "Charging"
                },
                style = CompactLabel.copy(color = color),
            )
        }
        BatteryWithLevel(ch.level, if (ch.isCharging) IslandColors.Green else levelColor(ch.level))
    }
}

@Composable
private fun VolumeCompact(v: IslandActivity.Volume, baseHeight: Dp, accent: Color) {
    val fraction by animateFloatAsState(v.percent / 100f, tween(140), label = "vol")
    CompactRow(baseHeight) {
        Icon(
            if (v.muted || v.percent == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
            null, tint = White, modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(IslandColors.Track),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("${v.percent}%", style = CompactLabel.copy(color = White))
    }
}

@Composable
private fun RingerCompact(r: IslandActivity.Ringer, baseHeight: Dp, accent: Color) {
    val (icon, label, color) = when (r.mode) {
        AudioManager.RINGER_MODE_SILENT -> Triple(Icons.Rounded.NotificationsOff, "Silent", IslandColors.Red)
        AudioManager.RINGER_MODE_VIBRATE -> Triple(Icons.Rounded.Vibration, "Vibrate", IslandColors.Orange)
        else -> Triple(Icons.Rounded.NotificationsActive, "Ring", accent)
    }
    CompactRow(baseHeight) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(label, style = CompactLabel.copy(color = color))
    }
}

// Expanded layouts -----------------------------------------------------------------------------

@Composable
private fun MediaExpanded(m: IslandActivity.Media, actions: IslandActions) {
    var tick by remember { mutableLongStateOf(now()) }
    LaunchedEffect(m.isPlaying, m.sampledAt) {
        while (m.isPlaying) {
            tick = now()
            delay(500)
        }
    }
    val position = m.livePosition(tick)
    val accent = Color(m.accent)
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(m.art, 54.dp, corner = 13.dp, accent = accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, style = Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(m.artist, style = Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Waveform(accent, m.isPlaying, barHeight = 22.dp)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatDuration(position), style = Sub.copy(fontSize = 11.sp))
            Spacer(Modifier.width(8.dp))
            val fraction = if (m.durationMs > 0) position.toFloat() / m.durationMs else 0f
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(IslandColors.Track),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(White),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (m.durationMs > 0) "-" + formatDuration(m.durationMs - position) else "--:--",
                style = Sub.copy(fontSize = 11.sp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(Icons.Rounded.SkipPrevious, 30.dp) { actions.media?.previous() }
            ControlButton(if (m.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 40.dp) { actions.media?.playPause() }
            ControlButton(Icons.Rounded.SkipNext, 30.dp) { actions.media?.next() }
        }
    }
}

@Composable
private fun CallExpanded(c: IslandActivity.Call, actions: IslandActions) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.avatar, c.caller, 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (c.isIncoming) "Incoming call" else callTimer(c),
                style = Sub.copy(fontSize = 12.sp, color = if (c.isIncoming) IslandColors.Gray else IslandColors.Green),
            )
            Text(c.caller, style = Title.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (c.isIncoming) {
            RoundAction(Icons.Rounded.CallEnd, IslandColors.Red) { actions.send(c.decline) }
            Spacer(Modifier.width(12.dp))
            RoundAction(Icons.Rounded.Call, IslandColors.Green) { actions.send(c.answer) }
        } else {
            RoundAction(Icons.Rounded.CallEnd, IslandColors.Red) { actions.send(c.hangUp) }
        }
    }
}

@Composable
private fun ChargingExpanded(ch: IslandActivity.Charging) {
    val color = if (ch.isCharging) IslandColors.Green else levelColor(ch.level)
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    !ch.isCharging -> "On battery"
                    ch.isFast -> "Fast charging"
                    else -> "Charging"
                },
                style = Sub.copy(color = color, fontWeight = FontWeight.SemiBold),
            )
            Text("${ch.level}%", style = Title.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold))
            if (ch.isCharging && ch.watts >= 1f) Text("%.1f W".format(ch.watts), style = Sub.copy(fontSize = 12.sp))
        }
        BatteryGlyph(ch.level, color, Modifier.size(width = 64.dp, height = 30.dp), charging = ch.isCharging)
    }
}

@Composable
private fun NoticeExpanded(n: IslandActivity.Notice, accent: Color) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (n.icon != null) {
            Image(
                n.icon.asImageBitmap(), null,
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Avatar(null, n.appName, 42.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.appName.uppercase(), style = Sub.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = accent), maxLines = 1, modifier = Modifier.weight(1f))
                Text("now", style = Sub.copy(fontSize = 11.sp))
            }
            Text(n.title, style = Title.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (n.text.isNotEmpty()) Text(n.text, style = Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Pieces -----------------------------------------------------------------------------------------

@Composable
private fun callTimer(c: IslandActivity.Call): String {
    if (c.startedAt <= 0L) return if (c.isIncoming) "Incoming" else "Calling"
    var wall by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(c.startedAt) {
        while (true) {
            wall = System.currentTimeMillis()
            delay(1000)
        }
    }
    return formatDuration(wall - c.startedAt)
}

@Composable
internal fun Waveform(color: Color, playing: Boolean, barHeight: Dp, modifier: Modifier = Modifier) {
    val bars = 5
    Row(
        modifier.height(barHeight),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playing) {
            val transition = rememberInfiniteTransition(label = "wave")
            repeat(bars) { i ->
                val f by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(380 + i * 90, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "bar$i",
                )
                Bar(color, barHeight * f)
            }
        } else {
            repeat(bars) { Bar(color.copy(alpha = 0.7f), barHeight * 0.22f) }
        }
    }
}

@Composable
private fun Bar(color: Color, height: Dp) {
    Box(
        Modifier
            .width(3.dp)
            .height(height)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
internal fun Artwork(art: Bitmap?, size: Dp, corner: Dp, accent: Color) {
    if (art != null) {
        Image(
            art.asImageBitmap(), null,
            Modifier
                .size(size)
                .clip(RoundedCornerShape(corner)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.35f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
private fun Avatar(bitmap: Bitmap?, name: String, size: Dp) {
    if (bitmap != null) {
        Image(
            bitmap.asImageBitmap(), null,
            Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFF48484A)))),
            contentAlignment = Alignment.Center,
        ) {
            val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
            Text(initials.ifEmpty { "?" }, style = Title.copy(fontSize = (size.value * 0.36f).sp))
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ControlButton(icon: ImageVector, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size + 14.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = White, modifier = Modifier.size(size))
    }
}

@Composable
private fun BatteryWithLevel(level: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$level%", style = CompactLabel.copy(color = color))
        Spacer(Modifier.width(6.dp))
        BatteryGlyph(level, color, Modifier.size(width = 25.dp, height = 12.dp))
    }
}

/** iOS-style battery: outlined body, rounded fill and a small terminal nub. */
@Composable
internal fun BatteryGlyph(level: Int, color: Color, modifier: Modifier, charging: Boolean = false) {
    Canvas(modifier) {
        val nubW = size.width * 0.07f
        val bodyW = size.width - nubW - 1f
        val stroke = (size.height * 0.09f).coerceAtLeast(1.5f)
        val r = size.height * 0.28f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyW - stroke, size.height - stroke),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(stroke),
        )
        val inset = stroke * 1.8f
        val fillW = (bodyW - inset * 2) * (level.coerceIn(0, 100) / 100f)
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(fillW.coerceAtLeast(1f), size.height - inset * 2),
            cornerRadius = CornerRadius(r * 0.6f, r * 0.6f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(bodyW + 1f, size.height * 0.32f),
            size = Size(nubW, size.height * 0.36f),
            cornerRadius = CornerRadius(nubW, nubW),
        )
        if (charging) {
            val cx = bodyW / 2
            val h = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx + h * 0.08f, h * 0.12f)
                lineTo(cx - h * 0.22f, h * 0.56f)
                lineTo(cx - h * 0.01f, h * 0.56f)
                lineTo(cx - h * 0.08f, h * 0.88f)
                lineTo(cx + h * 0.22f, h * 0.44f)
                lineTo(cx + h * 0.01f, h * 0.44f)
                close()
            }
            drawPath(path, Color.Black.copy(alpha = 0.85f))
        }
    }
}

internal fun levelColor(level: Int): Color = when {
    level <= 20 -> IslandColors.Red
    level <= 40 -> IslandColors.Orange
    else -> IslandColors.Green
}
