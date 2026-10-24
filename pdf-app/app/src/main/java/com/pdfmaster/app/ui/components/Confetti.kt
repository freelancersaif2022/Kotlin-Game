package com.pdfmaster.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.pdfmaster.app.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(val angle: Float, val speed: Float, val size: Float, val color: Color, val spin: Float, val drift: Float)

/** One-shot confetti burst from the top centre. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, count: Int = 70) {
    val colors = listOf(Brand.Red, Brand.RedBright, Brand.Gold, Color(0xFF22C55E), Color(0xFF60A5FA), Color(0xFFFFC2C4))
    val particles = remember {
        List(count) {
            Particle(
                angle = Random.nextFloat() * 140f + 20f,
                speed = Random.nextFloat() * 0.7f + 0.5f,
                size = Random.nextFloat() * 8f + 6f,
                color = colors.random(),
                spin = Random.nextFloat() * 720f - 360f,
                drift = Random.nextFloat() * 2f - 1f,
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    Canvas(modifier) {
        val time = t.value
        if (time >= 1f) return@Canvas
        val origin = Offset(size.width / 2f, size.height * 0.18f)
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val dist = p.speed * size.width * 0.75f * time
            val gravity = size.height * 0.9f * time * time
            val x = origin.x + (cos(rad) * dist).toFloat() * (if (p.angle > 90) 1f else 1f) + p.drift * 40f * time
            val y = origin.y - (sin(rad) * dist).toFloat() * 0.9f + gravity
            val alpha = (1f - time).coerceIn(0f, 1f)
            rotate(p.spin * time, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}
