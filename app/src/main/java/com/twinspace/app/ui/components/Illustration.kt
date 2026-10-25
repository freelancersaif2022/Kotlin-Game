package com.twinspace.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinspace.app.ui.theme.Brand

/** Two floating, overlapping app cards: the visual identity of Twin Space. */
@Composable
fun TwinIllustration(modifier: Modifier = Modifier, size: Dp = 200.dp, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "twin")
    val float by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "float",
    )
    val t = if (animate) float else 0.5f
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val center = Offset(this.size.width / 2, this.size.height / 2)

        // Soft glow
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Brand.Violet.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = s * 0.55f,
            ),
            radius = s * 0.55f,
            center = center,
        )
        // Orbit ring
        drawCircle(
            color = Brand.Indigo.copy(alpha = 0.25f),
            radius = s * 0.44f,
            center = center,
            style = Stroke(width = s * 0.006f),
        )
        val dotAngle = Math.toRadians((t * 140.0) - 60.0)
        drawCircle(
            Brand.Mint,
            radius = s * 0.022f,
            center = Offset(
                center.x + (s * 0.44f * Math.cos(dotAngle)).toFloat(),
                center.y + (s * 0.44f * Math.sin(dotAngle)).toFloat(),
            ),
        )

        val card = s * 0.40f
        val corner = CornerRadius(card * 0.26f)
        val lift = (t - 0.5f) * s * 0.03f

        // Back card (the original)
        rotate(-10f, center) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Brand.Mint.copy(alpha = 0.85f), Brand.Indigo.copy(alpha = 0.85f)),
                    start = Offset(center.x - card, center.y - card),
                    end = Offset(center.x + card * 0.2f, center.y + card * 0.4f),
                ),
                topLeft = Offset(center.x - card * 0.95f, center.y - card * 0.78f - lift),
                size = Size(card, card),
                cornerRadius = corner,
            )
        }
        // Front card (the clone)
        rotate(8f, center) {
            val topLeft = Offset(center.x - card * 0.18f, center.y - card * 0.28f + lift)
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Brand.Indigo, Brand.Violet),
                    start = topLeft,
                    end = Offset(topLeft.x + card, topLeft.y + card),
                ),
                topLeft = topLeft,
                size = Size(card, card),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = topLeft,
                size = Size(card, card),
                cornerRadius = corner,
                style = Stroke(width = s * 0.008f),
            )
            // "Screen" lines on the clone card
            val lineX = topLeft.x + card * 0.2f
            val lineW = card * 0.6f
            listOf(0.32f, 0.5f, 0.68f).forEachIndexed { i, y ->
                drawRoundRect(
                    color = Color.White.copy(alpha = if (i == 0) 0.9f else 0.5f),
                    topLeft = Offset(lineX, topLeft.y + card * y - card * 0.035f),
                    size = Size(if (i == 0) lineW else lineW * (0.75f - i * 0.1f), card * 0.07f),
                    cornerRadius = CornerRadius(card * 0.035f),
                )
            }
        }
    }
}
