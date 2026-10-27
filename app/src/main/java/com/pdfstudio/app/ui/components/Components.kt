package com.pdfstudio.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.pdfstudio.app.R
import com.pdfstudio.app.ui.theme.Brand

/** The app icon drawn in Compose: brand gradient tile with the document mark. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier, shine: Boolean = false, elevation: Dp = size * 0.16f) {
    val shape = RoundedCornerShape(size * 0.28f)
    val sweep = if (shine) {
        val transition = rememberInfiniteTransition(label = "shine")
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep",
        ).value
    } else null
    Box(
        modifier
            .size(size)
            .shadow(elevation, shape, ambientColor = Brand.Magenta, spotColor = Brand.Coral)
            .clip(shape)
            .background(Brand.gradient)
            .drawWithContent {
                drawContent()
                if (sweep != null) {
                    val x = sweep * this.size.width
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.35f),
                            1f to Color.Transparent,
                            start = Offset(x - this.size.width * 0.4f, 0f),
                            end = Offset(x + this.size.width * 0.4f, this.size.height),
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.5f),
        )
    }
}

/** Large tool button: glossy gradient tile with a line icon and a springy press animation. */
@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.45f, stiffness = 650f), label = "press")
    val shape = RoundedCornerShape(19.dp)
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(58.dp)
                .shadow(14.dp, shape, ambientColor = colors.last(), spotColor = colors.last())
                .clip(shape)
                .background(Brush.linearGradient(colors, start = Offset.Zero, end = Offset.Infinite))
                .drawWithContent {
                    // Soft top light and a thin inner rim for a glassy, tactile look.
                    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), endY = size.height * 0.6f))
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width * 0.25f, 0f), radius = size.width * 0.8f),
                        radius = size.width * 0.8f,
                        center = Offset(size.width * 0.25f, 0f),
                    )
                    drawContent()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.28f),
                        cornerRadius = CornerRadius(19.dp.toPx()),
                        style = Stroke(1.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Small rounded pill used for counters and badges. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
