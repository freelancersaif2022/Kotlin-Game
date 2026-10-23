package com.voxcast.mic.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxcast.mic.ui.theme.VoxColors

/** Primary call-to-action: gradient fill, gloss, and a light sweep every few seconds. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = VoxColors.PrimaryGradient,
    contentColor: Color = VoxColors.Background,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    shimmer: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "shine")
    val sweep by t.animateFloat(
        initialValue = -0.6f, targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(2_600, delayMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .bounceClick(enabled = enabled && !loading, onClick = onClick)
            .clip(shape)
            .background(brush)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), endY = size.height * 0.6f),
                )
                if (shimmer && enabled) {
                    val x = size.width * sweep
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                            start = Offset(x - size.height * 1.2f, 0f),
                            end = Offset(x, size.height),
                        ),
                    )
                }
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)), shape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() },
            label = "btn",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(color = contentColor, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
                }
            }
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = VoxColors.Text,
    height: Dp = 52.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .height(height)
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, VoxColors.OutlineSoft, shape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** Round frosted icon button used in top bars and control rows. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = VoxColors.Text,
    background: Color = Color.White.copy(alpha = 0.06f),
    active: Boolean = false,
) {
    Box(
        modifier
            .size(size)
            .bounceClick(scaleDown = 0.9f, onClick = onClick)
            .clip(CircleShape)
            .background(if (active) VoxColors.Cyan.copy(alpha = 0.18f) else background)
            .border(1.dp, if (active) VoxColors.Cyan.copy(alpha = 0.5f) else VoxColors.OutlineSoft, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (active) VoxColors.Cyan else tint, modifier = Modifier.size(size * 0.48f))
    }
}
