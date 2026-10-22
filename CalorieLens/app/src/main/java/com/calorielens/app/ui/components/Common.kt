package com.calorielens.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand

/** Scales the element down slightly while pressed, giving a tactile feel. */
fun Modifier.pressScale(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "pressScale",
    )
    val haptics = LocalHapticFeedback.current
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** Animated shimmer highlight sweeping across the content. */
fun Modifier.shimmer(color: Color = Color.White.copy(alpha = 0.22f), durationMs: Int = 2200): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    drawWithContent {
        drawContent()
        val x = size.width * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, color, Color.Transparent),
                start = Offset(x - size.width * 0.4f, 0f),
                end = Offset(x + size.width * 0.1f, size.height),
            ),
        )
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .let { if (onClick != null) it.pressScale(scaleDown = 0.98f, onClick = onClick) else it }
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.stroke, shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = Brand.primaryGradient,
    contentColor: Color = Color(0xFF04140F),
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    shimmer: Boolean = false,
    height: Dp = 58.dp,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(height)
            .pressScale(enabled = enabled && !loading, onClick = onClick)
            .clip(shape)
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color(0xFF2A2F3A), Color(0xFF2A2F3A))))
            .let { if (shimmer && enabled) it.shimmer() else it },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = contentColor, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = if (enabled) contentColor else AppTheme.colors.muted, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) contentColor else AppTheme.colors.muted,
                )
            }
        }
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    height: Dp = 54.dp,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(height)
            .pressScale(onClick = onClick)
            .clip(shape)
            .border(1.dp, AppTheme.colors.stroke, shape)
            .background(AppTheme.colors.cardElevated)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = AppTheme.colors.cardElevated,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    bordered: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .pressScale(scaleDown = 0.9f, onClick = onClick)
            .clip(CircleShape)
            .background(background)
            .let { if (bordered) it.border(1.dp, AppTheme.colors.stroke, CircleShape) else it },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun AnimatedNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    durationMs: Int = 1100,
    startFromZero: Boolean = true,
    suffix: String = "",
) {
    var target by remember { mutableIntStateOf(if (startFromZero) 0 else value) }
    LaunchedEffect(value) { target = value }
    val animated by animateIntAsState(target, tween(durationMs), label = "number")
    Text("%,d".format(animated) + suffix, style = style, color = color, modifier = modifier)
}

@Composable
fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    track: Color = AppTheme.colors.stroke,
    delayMs: Int = 0,
) {
    var started by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { started = 1 }
    val animated by animateFloatAsState(
        targetValue = if (started == 1) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(1000, delayMillis = delayMs),
        label = "progressBar",
    )
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color))),
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (filled) Color.Black else color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.Black else color,
        )
    }
}

@Composable
fun GlowBox(
    color: Color,
    modifier: Modifier = Modifier,
    radiusFraction: Float = 0.6f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier.drawWithContent {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = 0.35f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * radiusFraction,
                ),
                radius = size.minDimension * radiusFraction,
            )
            drawContent()
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
