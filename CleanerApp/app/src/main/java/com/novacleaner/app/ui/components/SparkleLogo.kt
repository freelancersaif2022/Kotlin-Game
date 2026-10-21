package com.novacleaner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Four-point sparkle – the Nova brand mark – centred at [cx],[cy] with radius [r]. */
fun sparklePath(cx: Float, cy: Float, r: Float, pinch: Float = 0.08f, bulge: Float = 0.3f): Path = Path().apply {
    moveTo(cx, cy - r)
    cubicTo(cx + r * pinch, cy - r * bulge, cx + r * bulge, cy - r * pinch, cx + r, cy)
    cubicTo(cx + r * bulge, cy + r * pinch, cx + r * pinch, cy + r * bulge, cx, cy + r)
    cubicTo(cx - r * pinch, cy + r * bulge, cx - r * bulge, cy + r * pinch, cx - r, cy)
    cubicTo(cx - r * bulge, cy - r * pinch, cx - r * pinch, cy - r * bulge, cx, cy - r)
    close()
}

@Composable
fun SparkleLogo(modifier: Modifier = Modifier, glow: Color = Color.White, accent: Color = Color(0xFFB6F25C)) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), c, s / 2), s / 2, c)
        drawPath(
            sparklePath(c.x - s * 0.04f, c.y, s * 0.3f),
            Brush.linearGradient(listOf(Color.White, Color(0xFFD7FBEF)), Offset(c.x, c.y - s * 0.3f), Offset(c.x, c.y + s * 0.3f)),
        )
        drawPath(sparklePath(c.x + s * 0.2f, c.y - s * 0.2f, s * 0.1f), Color(0xFFD7FBEF))
        drawPath(sparklePath(c.x + s * 0.18f, c.y + s * 0.2f, s * 0.065f), accent)
    }
}
