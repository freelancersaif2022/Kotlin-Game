package com.pulse.messenger.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulse.messenger.ui.theme.PulseTheme

enum class AvatarFallback { PERSON, GROUP, BROADCAST, INITIALS }

private val initialColors = listOf(
    Color(0xFF0E9F8A), Color(0xFF5B6EE1), Color(0xFFE5793B), Color(0xFFB35BD1),
    Color(0xFF2F9BD6), Color(0xFFD9475F), Color(0xFF3FA34D), Color(0xFFC79A1E),
)

@Composable
fun Avatar(
    @DrawableRes res: Int?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    fallback: AvatarFallback = AvatarFallback.INITIALS,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (res != null) {
            AsyncImage(
                model = res,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            AvatarPlaceholder(name, size, fallback)
        }
    }
}

@Composable
private fun AvatarPlaceholder(name: String, size: Dp, fallback: AvatarFallback) {
    val muted = PulseTheme.colors.textSecondary
    when (fallback) {
        AvatarFallback.PERSON -> Icon(Icons.Filled.Person, null, tint = muted.copy(alpha = 0.7f), modifier = Modifier.size(size * 0.62f))
        AvatarFallback.GROUP -> Icon(Icons.Filled.Groups, null, tint = muted.copy(alpha = 0.7f), modifier = Modifier.size(size * 0.6f))
        AvatarFallback.BROADCAST -> Box(
            Modifier.size(size).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Campaign, null, tint = Color.White, modifier = Modifier.size(size * 0.52f)) }
        AvatarFallback.INITIALS -> {
            val initials = remember(name) {
                name.split(" ").filter { it.isNotBlank() && it.first().isLetter() }.take(2)
                    .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
            }
            val color = remember(name) { initialColors[(name.hashCode() and 0x7fffffff) % initialColors.size] }
            Box(Modifier.size(size).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(
                    initials,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * 0.38f).sp,
                )
            }
        }
    }
}

/** Avatar surrounded by a segmented ring, one segment per status update. */
@Composable
fun StatusAvatar(
    @DrawableRes res: Int?,
    name: String,
    size: Dp,
    segments: Int,
    viewed: Int,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 2.5.dp,
) {
    val unseen = MaterialTheme.colorScheme.primary
    val seen = PulseTheme.colors.textSecondary.copy(alpha = 0.45f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = ringWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            if (segments <= 1) {
                drawArc(
                    color = if (viewed >= 1) seen else unseen, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
                )
            } else {
                val gap = 6f
                val sweep = 360f / segments
                for (i in 0 until segments) {
                    drawArc(
                        color = if (i < viewed) seen else unseen,
                        startAngle = -90f + i * sweep + gap / 2, sweepAngle = sweep - gap, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Avatar(res, name, size - ringWidth * 2 - 5.dp)
    }
}

/** Small green dot shown on avatars of contacts that are online. */
@Composable
fun OnlineDot(modifier: Modifier = Modifier, size: Dp = 13.dp) {
    Box(
        modifier
            .size(size)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            .padding(2.dp)
            .background(PulseTheme.colors.online, CircleShape),
    )
}
