package com.voxcast.mic.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxcast.mic.ui.theme.VoxColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    border: Brush? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .glass(RoundedCornerShape(corner), border)
            .padding(padding),
        content = content,
    )
}

/**
 * Premium app-icon style badge: gradient squircle, glossy top highlight, soft glow
 * and a duotone glyph.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    brush: Brush,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    glow: Color = Color.Transparent,
    iconTint: Color = Color.White,
) {
    val corner = size * 0.32f
    Box(
        modifier
            .size(size)
            .drawBehind {
                if (glow != Color.Transparent) {
                    drawCircle(
                        Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), center, this.size.minDimension),
                        radius = this.size.minDimension,
                    )
                }
            }
            .clip(RoundedCornerShape(corner))
            .background(brush)
            .drawBehind {
                // Gloss highlight across the top half.
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.04f)),
                        endY = this.size.height * 0.55f,
                    ),
                    size = Size(this.size.width, this.size.height * 0.55f),
                    cornerRadius = CornerRadius(corner.toPx()),
                )
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)), RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Small capsule with an optional pulsing status dot. */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    icon: ImageVector? = null,
) {
    val t = rememberInfiniteTransition(label = "pill")
    val pulse by t.animateFloat(1f, if (pulsing) 2.2f else 1f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "p")
    val pulseAlpha by t.animateFloat(0.6f, 0f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "a")
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        } else {
            Canvas(Modifier.size(8.dp)) {
                if (pulsing) drawCircle(color.copy(alpha = pulseAlpha), radius = size.minDimension / 2 * pulse)
                drawCircle(color, radius = size.minDimension / 2)
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = VoxColors.TextFaint,
        modifier = modifier.padding(start = 4.dp, bottom = 10.dp),
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = VoxColors.Cyan) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VoxColors.Surface.copy(alpha = 0.8f))
            .border(1.dp, VoxColors.OutlineSoft, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim, maxLines = 1)
    }
}

/** Thin divider that fades out at both ends. */
@Composable
fun FadeDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp)) {
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, VoxColors.Outline, Color.Transparent)),
            Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = size.height,
        )
    }
}
