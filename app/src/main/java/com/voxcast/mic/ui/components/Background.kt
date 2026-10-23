package com.voxcast.mic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.voxcast.mic.ui.theme.VoxColors
import kotlin.math.cos
import kotlin.math.sin

/** Slow-moving aurora backdrop used behind every screen. */
@Composable
fun VoxBackground(
    modifier: Modifier = Modifier,
    accent: Color = VoxColors.Cyan,
    secondary: Color = VoxColors.Violet,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = rememberInfiniteTransition(label = "aurora")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(VoxColors.BackgroundTop, VoxColors.Background, VoxColors.Background)))
            val w = size.width
            val h = size.height
            val c1 = Offset(w * (0.2f + 0.12f * cos(phase)), h * (0.12f + 0.05f * sin(phase)))
            val c2 = Offset(w * (0.85f + 0.1f * sin(phase * 1.3f)), h * (0.38f + 0.06f * cos(phase)))
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.20f), Color.Transparent), c1, w * 0.75f),
                radius = w * 0.75f, center = c1,
            )
            drawCircle(
                Brush.radialGradient(listOf(secondary.copy(alpha = 0.16f), Color.Transparent), c2, w * 0.7f),
                radius = w * 0.7f, center = c2,
            )
        }
        // Static layer: never invalidated by the animation above.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Fine dot grid for depth.
            val step = 26f * density
            val dot = Color.White.copy(alpha = 0.025f)
            var y = step / 2
            while (y < h) {
                var x = step / 2
                while (x < w) {
                    drawCircle(dot, radius = 1.1f * density, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        content()
    }
}
