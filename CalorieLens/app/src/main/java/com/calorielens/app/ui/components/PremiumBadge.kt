package com.calorielens.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorielens.app.ui.theme.Brand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Premium emblem: a faceted hexagon shield in deep obsidian with a gold rim, holding a jewelled crown.
 * When [animated], a light sweep glints across it and a soft halo pulses behind it.
 */
@Composable
fun PremiumEmblem(modifier: Modifier = Modifier, size: Dp = 64.dp, animated: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "emblem")
    val sweep by transition.animateFloat(
        initialValue = -0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2600, delayMillis = 600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val halo by transition.animateFloat(
        initialValue = 0.75f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "halo",
    )
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val haloScale = if (animated) halo else 0.85f
        drawCircle(
            Brush.radialGradient(listOf(Brand.Gold.copy(alpha = 0.35f * haloScale), Color.Transparent), c, w * 0.62f),
            radius = w * 0.62f,
            center = c,
        )
        val hex = hexagon(c, w * 0.44f)
        drawPath(hex, Brush.linearGradient(listOf(Color(0xFF2A2318), Color(0xFF0B0A08)), Offset(0f, 0f), Offset(w, w)))
        drawPath(
            hex,
            Brush.linearGradient(listOf(Brand.GoldLight, Brand.Gold, Brand.GoldDark, Brand.Gold), Offset(0f, 0f), Offset(w, w)),
            style = Stroke(width = w * 0.045f, join = StrokeJoin.Round),
        )
        val inner = hexagon(c, w * 0.37f)
        drawPath(inner, Brand.Gold.copy(alpha = 0.18f), style = Stroke(width = w * 0.012f))
        drawCrown(c, w * 0.5f)
        if (animated) {
            clipPath(hex) {
                val x = w * sweep
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                        start = Offset(x - w * 0.25f, 0f),
                        end = Offset(x + w * 0.05f, w),
                    ),
                )
            }
        }
    }
}

private fun hexagon(center: Offset, radius: Float): Path = Path().apply {
    for (i in 0 until 6) {
        val angle = PI / 3 * i - PI / 2
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawCrown(center: Offset, size: Float) {
    val left = center.x - size / 2
    val top = center.y - size / 2
    fun p(x: Float, y: Float) = Offset(left + size * x, top + size * y)

    val body = Path().apply {
        moveTo(p(0.16f, 0.72f).x, p(0.16f, 0.72f).y)
        lineTo(p(0.10f, 0.32f).x, p(0.10f, 0.32f).y)
        lineTo(p(0.33f, 0.52f).x, p(0.33f, 0.52f).y)
        lineTo(p(0.50f, 0.20f).x, p(0.50f, 0.20f).y)
        lineTo(p(0.67f, 0.52f).x, p(0.67f, 0.52f).y)
        lineTo(p(0.90f, 0.32f).x, p(0.90f, 0.32f).y)
        lineTo(p(0.84f, 0.72f).x, p(0.84f, 0.72f).y)
        close()
    }
    val gold = Brush.verticalGradient(listOf(Brand.GoldLight, Brand.Gold, Brand.GoldDeep), p(0f, 0.2f).y, p(0f, 0.86f).y)
    drawPath(body, gold)
    // Facet shading on the right half for depth.
    val facet = Path().apply {
        moveTo(p(0.50f, 0.20f).x, p(0.50f, 0.20f).y)
        lineTo(p(0.67f, 0.52f).x, p(0.67f, 0.52f).y)
        lineTo(p(0.90f, 0.32f).x, p(0.90f, 0.32f).y)
        lineTo(p(0.84f, 0.72f).x, p(0.84f, 0.72f).y)
        lineTo(p(0.50f, 0.72f).x, p(0.50f, 0.72f).y)
        close()
    }
    drawPath(facet, Brand.GoldDark.copy(alpha = 0.28f))
    // Band
    val bandTop = p(0f, 0.74f).y
    drawRoundRect(
        gold,
        topLeft = Offset(p(0.15f, 0f).x, bandTop),
        size = androidx.compose.ui.geometry.Size(size * 0.70f, size * 0.12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.03f),
    )
    // Jewels
    listOf(p(0.10f, 0.29f), p(0.50f, 0.16f), p(0.90f, 0.29f)).forEach {
        drawCircle(Brand.GoldLight, size * 0.06f, it)
        drawCircle(Color.White.copy(alpha = 0.8f), size * 0.022f, Offset(it.x - size * 0.015f, it.y - size * 0.015f))
    }
    val gemCenter = p(0.5f, 0.80f)
    val gem = Path().apply {
        moveTo(gemCenter.x, gemCenter.y - size * 0.05f)
        lineTo(gemCenter.x + size * 0.05f, gemCenter.y)
        lineTo(gemCenter.x, gemCenter.y + size * 0.05f)
        lineTo(gemCenter.x - size * 0.05f, gemCenter.y)
        close()
    }
    drawPath(gem, Brush.linearGradient(listOf(Color(0xFF7CF5D0), Brand.Cyan)))
    listOf(0.30f, 0.70f).forEach { x -> drawCircle(Color(0xFFFF6A8A), size * 0.028f, p(x, 0.80f)) }
}

/** Compact gold "PRO" pill used in headers and list rows. */
@Composable
fun ProChip(modifier: Modifier = Modifier, text: String = "PRO") {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2A2318), Color(0xFF14110B))))
            .border(1.dp, Brush.linearGradient(listOf(Brand.GoldLight, Brand.GoldDeep)), shape)
            .shimmer(Color.White.copy(alpha = 0.18f), 2600)
            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremiumEmblem(size = 20.dp, animated = false)
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(
                brush = Brand.goldGradient,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            ),
        )
    }
}
