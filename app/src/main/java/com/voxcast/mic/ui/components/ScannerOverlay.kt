package com.voxcast.mic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.voxcast.mic.ui.theme.VoxColors
import kotlin.math.min

/** Window position of the scan frame, shared with the screen so labels line up with it. */
fun scanWindow(size: Size): Rect {
    val box = min(size.width, size.height) * 0.70f
    val left = (size.width - box) / 2
    val top = size.height * 0.40f - box / 2
    return Rect(left, top, left + box, top + box)
}

/**
 * Camera overlay: dimmed surround with a clear rounded window, breathing gradient corner
 * brackets, a sweeping laser with trailing glow, and a lock-on animation (brackets
 * tighten and turn green) once a valid code is found.
 */
@Composable
fun ScannerOverlay(detected: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "scan")
    val laser by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "laser")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_200), RepeatMode.Reverse), label = "breathe")
    val lock by animateFloatAsState(if (detected) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "lock")
    val bracketA by animateColorAsState(if (detected) VoxColors.Success else VoxColors.Cyan, tween(300), label = "ca")
    val bracketB by animateColorAsState(if (detected) Color(0xFF9BFFD6) else VoxColors.Violet, tween(300), label = "cb")

    Canvas(modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val window = scanWindow(size)
        val shrink = window.width * 0.06f * lock
        val r = Rect(window.left + shrink, window.top + shrink, window.right - shrink, window.bottom - shrink)
        val corner = 30f * density

        drawRect(Color(0xFF03050A).copy(alpha = 0.66f))
        drawRoundRect(Color.Transparent, r.topLeft, r.size, CornerRadius(corner), blendMode = BlendMode.Clear)
        drawRoundRect(
            if (detected) VoxColors.Success.copy(alpha = 0.10f * lock) else Color.Transparent,
            r.topLeft, r.size, CornerRadius(corner),
        )

        // Faint guide grid inside the window.
        if (!detected) {
            val step = r.width / 8
            for (i in 1 until 8) {
                drawLine(Color.White.copy(alpha = 0.04f), Offset(r.left + step * i, r.top), Offset(r.left + step * i, r.bottom), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.04f), Offset(r.left, r.top + step * i), Offset(r.right, r.top + step * i), strokeWidth = 1f)
            }
        }

        // Corner brackets.
        val len = r.width * (0.15f + 0.025f * breathe)
        val sw = 5.5f * density
        val brush = Brush.linearGradient(listOf(bracketA, bracketB), r.topLeft, r.bottomRight)
        val path = Path().apply {
            // top-left
            moveTo(r.left, r.top + len); lineTo(r.left, r.top + corner)
            quadraticTo(r.left, r.top, r.left + corner, r.top); lineTo(r.left + len, r.top)
            // top-right
            moveTo(r.right - len, r.top); lineTo(r.right - corner, r.top)
            quadraticTo(r.right, r.top, r.right, r.top + corner); lineTo(r.right, r.top + len)
            // bottom-right
            moveTo(r.right, r.bottom - len); lineTo(r.right, r.bottom - corner)
            quadraticTo(r.right, r.bottom, r.right - corner, r.bottom); lineTo(r.right - len, r.bottom)
            // bottom-left
            moveTo(r.left + len, r.bottom); lineTo(r.left + corner, r.bottom)
            quadraticTo(r.left, r.bottom, r.left, r.bottom - corner); lineTo(r.left, r.bottom - len)
        }
        drawPath(path, brush, alpha = 0.35f, style = Stroke(sw * 2.6f, cap = StrokeCap.Round))
        drawPath(path, brush, style = Stroke(sw, cap = StrokeCap.Round))

        // Laser.
        if (!detected) {
            val y = r.top + r.height * (0.06f + 0.88f * laser)
            val inset = r.width * 0.06f
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, VoxColors.Cyan.copy(alpha = 0.22f)), startY = y - r.height * 0.18f, endY = y),
                topLeft = Offset(r.left + inset, y - r.height * 0.18f), size = Size(r.width - inset * 2, r.height * 0.18f),
            )
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, VoxColors.Cyan, Color.White, VoxColors.Cyan, Color.Transparent), startX = r.left + inset, endX = r.right - inset),
                Offset(r.left + inset, y), Offset(r.right - inset, y), strokeWidth = 2.6f * density, cap = StrokeCap.Round,
            )
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, VoxColors.Cyan.copy(alpha = 0.5f), Color.Transparent), startX = r.left + inset, endX = r.right - inset),
                Offset(r.left + inset, y), Offset(r.right - inset, y), strokeWidth = 10f * density, cap = StrokeCap.Round,
            )
        }
    }
}
