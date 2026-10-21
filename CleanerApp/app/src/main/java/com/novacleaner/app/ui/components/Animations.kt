package com.novacleaner.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(val angle: Float, val offset: Float, val radius: Float, val speed: Float)

/**
 * Radar-style scanner: rotating sweep beam, pulse ring, particles drifting into the core and an
 * outer progress arc. Used by every scanning tool so the app feels consistent.
 */
@Composable
fun ScanRadar(
    progress: Float?,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    diameter: Dp = 250.dp,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1900, easing = LinearEasing)), label = "sweep")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2100, easing = LinearOutSlowInEasing)), label = "pulse")
    val time by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "time")
    val breathe by transition.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "breathe")
    val animatedProgress by animateFloatAsState(progress ?: 0f, tween(400), label = "progress")
    val particles = remember {
        List(22) { Particle(Random.nextFloat() * 360f, Random.nextFloat(), 1.5f + Random.nextFloat() * 2.5f, 0.6f + Random.nextFloat() * 0.8f) }
    }
    val main = colors.first()
    val deep = colors.last()

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(main.copy(alpha = 0.22f), Color.Transparent), c, r), r)
            for (i in 1..3) {
                drawCircle(main.copy(alpha = 0.16f), radius = r * (0.32f + 0.2f * i), center = c, style = Stroke(1.dp.toPx()))
            }
            drawCircle(main.copy(alpha = (1f - pulse) * 0.55f), radius = r * (0.3f + 0.62f * pulse), center = c, style = Stroke(2.dp.toPx()))

            rotate(sweep, c) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent, 0.72f to Color.Transparent, 1f to main.copy(alpha = 0.5f),
                        center = c,
                    ),
                    radius = r * 0.92f,
                    center = c,
                )
                drawLine(
                    Brush.linearGradient(listOf(main.copy(alpha = 0.9f), main.copy(alpha = 0f)), start = c, end = Offset(c.x + r * 0.92f, c.y)),
                    start = c, end = Offset(c.x + r * 0.92f, c.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
                )
            }

            particles.forEach { p ->
                val phase = (time * p.speed + p.offset) % 1f
                val dist = r * (0.92f - 0.62f * phase)
                val rad = (p.angle * PI / 180.0).toFloat()
                val alpha = sin(phase * PI).toFloat().coerceIn(0f, 1f)
                drawCircle(
                    main.copy(alpha = alpha * 0.9f),
                    radius = p.radius.dp.toPx() * (1f - phase * 0.5f),
                    center = Offset(c.x + cos(rad) * dist, c.y + sin(rad) * dist),
                )
            }

            if (progress != null) {
                val stroke = 6.dp.toPx()
                val arcR = r - stroke / 2
                drawArc(main.copy(alpha = 0.15f), 0f, 360f, false, Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2), style = Stroke(stroke))
                drawArc(
                    Brush.sweepGradient(listOf(main, deep, main), center = c),
                    -90f, 360f * animatedProgress, false,
                    Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        GlossyIcon(icon, colors, size = diameter * 0.34f, modifier = Modifier.scale(breathe))
    }
}

/** Cleaning animation: debris spirals into a spinning core while the ring fills. */
@Composable
fun CleaningOrb(
    progress: Float,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    diameter: Dp = 250.dp,
) {
    val transition = rememberInfiniteTransition(label = "clean")
    val time by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "t")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "spin")
    val wobble by transition.animateFloat(-8f, 8f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "wobble")
    val animatedProgress by animateFloatAsState(progress, tween(300), label = "p")
    val debris = remember {
        List(26) { Particle(Random.nextFloat() * 360f, Random.nextFloat(), 2f + Random.nextFloat() * 3f, 0.7f + Random.nextFloat() * 0.7f) }
    }
    val main = colors.first()
    val deep = colors.last()
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(main.copy(alpha = 0.28f), Color.Transparent), c, r), r)
            debris.forEach { p ->
                val phase = (time * p.speed + p.offset) % 1f
                val dist = r * (0.95f - 0.75f * phase)
                val angle = ((p.angle + spin * 0.6f + phase * 140f) * PI / 180.0).toFloat()
                drawRoundRect(
                    color = deep.copy(alpha = (1f - phase) * 0.85f),
                    topLeft = Offset(c.x + cos(angle) * dist, c.y + sin(angle) * dist),
                    size = Size(p.radius.dp.toPx() * (1.4f - phase), p.radius.dp.toPx() * (1.4f - phase)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            }
            val stroke = 8.dp.toPx()
            val arcR = r * 0.78f
            drawArc(main.copy(alpha = 0.15f), 0f, 360f, false, Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2), style = Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(main, deep, main), center = c),
                -90f, 360f * animatedProgress, false,
                Offset(c.x - arcR, c.y - arcR), Size(arcR * 2, arcR * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        GlossyIcon(icon, colors, size = diameter * 0.34f, modifier = Modifier.rotate(wobble))
    }
}

/** Shimmer placeholder block for loading lists. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(-1f, 2f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "x")
    val base = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val w = size.width
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(base, base.copy(alpha = 0.45f), base),
                start = Offset(w * x - w * 0.5f, 0f),
                end = Offset(w * x + w * 0.5f, size.height),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx()),
        )
    }
}
