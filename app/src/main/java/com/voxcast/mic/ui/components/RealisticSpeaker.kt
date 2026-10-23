package com.voxcast.mic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.voxcast.mic.ui.theme.VoxColors

/**
 * Studio monitor cabinet drawn in Canvas: satin cabinet with bevel, corner screws,
 * silk-dome tweeter, rubber-surround woofer whose cone physically pumps with the
 * received voice, LED ring and bass port.
 */
@Composable
fun RealisticSpeaker(
    level: () -> Float,
    active: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = VoxColors.Cyan,
) {
    val activeAmount by animateFloatAsState(if (active) 1f else 0f, tween(500), label = "active")
    val measurer = rememberTextMeasurer()
    val brandStyle = TextStyle(color = Color(0xFFB8BFCB).copy(alpha = 0.55f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)

    Canvas(modifier.aspectRatio(0.70f)) {
        val lv = level().coerceIn(0f, 1f) * activeAmount
        val w = size.width
        val h = size.height
        val cx = w / 2
        val corner = CornerRadius(w * 0.09f)

        // Floor shadow.
        drawOval(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent), Offset(cx, h * 0.985f), w * 0.5f),
            topLeft = Offset(w * 0.04f, h * 0.955f), size = Size(w * 0.92f, h * 0.06f),
        )

        // Cabinet.
        val cabTl = Offset(w * 0.04f, h * 0.02f)
        val cabSize = Size(w * 0.92f, h * 0.94f)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF2A2F38), Color(0xFF171A20), Color(0xFF0C0E12)), startY = cabTl.y, endY = cabTl.y + cabSize.height),
            cabTl, cabSize, corner,
        )
        // Side light falloff.
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)), startX = cabTl.x, endX = cabTl.x + cabSize.width),
            cabTl, cabSize, corner,
        )
        // Bevel.
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.02f), Color.Black.copy(alpha = 0.6f)), startY = cabTl.y, endY = cabTl.y + cabSize.height),
            cabTl, cabSize, corner, style = Stroke(1.6f * density),
        )
        // Front baffle.
        val bTl = Offset(w * 0.09f, h * 0.05f)
        val bSize = Size(w * 0.82f, h * 0.88f)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF1C2027), Color(0xFF111318)), startY = bTl.y, endY = bTl.y + bSize.height),
            bTl, bSize, CornerRadius(w * 0.06f),
        )
        drawRoundRect(Color.Black.copy(alpha = 0.55f), bTl, bSize, CornerRadius(w * 0.06f), style = Stroke(1.2f * density))

        // Screws.
        val sInset = w * 0.135f
        listOf(
            Offset(sInset, h * 0.085f), Offset(w - sInset, h * 0.085f),
            Offset(sInset, h * 0.895f), Offset(w - sInset, h * 0.895f),
        ).forEach { drawScrew(it, w * 0.019f) }

        // Tweeter.
        drawTweeter(Offset(cx, h * 0.205f), w * 0.125f)

        // Brand.
        val brand = measurer.measure("VOXCAST", brandStyle)
        drawText(brand, topLeft = Offset(cx - brand.size.width / 2f, h * 0.322f))

        // Woofer.
        drawWoofer(Offset(cx, h * 0.6f), w * 0.36f, lv, activeAmount, accent)

        // Bass port.
        val portW = w * 0.34f
        val portH = h * 0.032f
        val portTl = Offset(cx - portW / 2, h * 0.865f)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF16181C)), startY = portTl.y, endY = portTl.y + portH), portTl, Size(portW, portH), CornerRadius(portH / 2))
        drawRoundRect(Color.White.copy(alpha = 0.08f), portTl, Size(portW, portH), CornerRadius(portH / 2), style = Stroke(1f * density))
    }
}

private fun DrawScope.drawScrew(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB9C0CB), Color(0xFF555B66), Color(0xFF1C1F24)), c + Offset(-r * 0.3f, -r * 0.3f), r * 1.4f), r, c)
    rotate(35f, c) {
        drawLine(Color.Black.copy(alpha = 0.75f), c + Offset(-r * 0.65f, 0f), c + Offset(r * 0.65f, 0f), strokeWidth = r * 0.32f)
    }
}

private fun DrawScope.drawTweeter(c: Offset, r: Float) {
    // Flange.
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3F48), Color(0xFF1A1D22), Color(0xFF0B0C0F)), c, r), r, c)
    drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.5f)), startY = c.y - r, endY = c.y + r), r, c, style = Stroke(1.2f * density))
    // Waveguide.
    drawCircle(Brush.radialGradient(listOf(Color(0xFF050608), Color(0xFF22252B)), c, r * 0.78f), r * 0.78f, c)
    // Silk dome.
    val domeR = r * 0.46f
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFEDF1F6), Color(0xFF8A929E), Color(0xFF30353D)), c + Offset(-domeR * 0.35f, -domeR * 0.4f), domeR * 1.3f),
        domeR, c,
    )
    drawCircle(Color.Black.copy(alpha = 0.6f), domeR, c, style = Stroke(1.4f * density))
}

private fun DrawScope.drawWoofer(c: Offset, r: Float, level: Float, active: Float, accent: Color) {
    // LED ring glow behind the flange.
    if (active > 0.01f) {
        val glowR = r * (1.08f + level * 0.05f)
        drawCircle(
            Brush.radialGradient(listOf(Color.Transparent, accent.copy(alpha = (0.15f + level * 0.55f) * active), Color.Transparent), c, glowR * 1.18f),
            glowR * 1.18f, c,
        )
        drawCircle(
            Brush.sweepGradient(listOf(accent, VoxColors.Violet, accent), c),
            r * 1.03f, c, alpha = (0.35f + level * 0.65f) * active, style = Stroke(2.6f * density),
        )
    }
    // Flange.
    drawCircle(Brush.radialGradient(listOf(Color(0xFF2E333B), Color(0xFF1A1D22), Color(0xFF0A0B0E)), c, r), r, c)
    drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.6f)), startY = c.y - r, endY = c.y + r), r, c, style = Stroke(1.5f * density))
    for (i in 0 until 4) {
        val a = Math.toRadians(45.0 + i * 90.0)
        drawScrew(c + Offset((kotlin.math.cos(a) * r * 0.93f).toFloat(), (kotlin.math.sin(a) * r * 0.93f).toFloat()), r * 0.035f)
    }

    val excursion = 1f + level * 0.075f
    // Rubber surround (half-roll).
    val surroundR = r * 0.86f
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFF0A0B0D), 0.80f to Color(0xFF0E1013), 0.88f to Color(0xFF41454E),
            0.94f to Color(0xFF1D2025), 1f to Color(0xFF07080A),
            center = c, radius = surroundR,
        ),
        surroundR, c,
    )
    // Cone.
    val coneR = r * 0.74f * excursion
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFF3C414B), 0.35f to Color(0xFF1C1F24), 0.85f to Color(0xFF15171B), 1f to Color(0xFF26292F),
            center = c + Offset(0f, -coneR * 0.05f), radius = coneR,
        ),
        coneR, c,
    )
    for (k in 1..5) {
        drawCircle(Color.White.copy(alpha = 0.035f), coneR * (0.42f + k * 0.11f), c, style = Stroke(0.8f * density))
    }
    // Key light across the cone.
    drawCircle(
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.25f)), c + Offset(-coneR, -coneR), c + Offset(coneR, coneR)),
        coneR, c,
    )
    // Dust cap.
    val capR = r * 0.27f * (1f + level * 0.11f)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF6A7282), Color(0xFF22262D), Color(0xFF0A0B0E)), c + Offset(-capR * 0.35f, -capR * 0.45f), capR * 1.35f),
        capR, c,
    )
    drawOval(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), c + Offset(-capR * 0.3f, -capR * 0.45f), capR * 0.5f),
        topLeft = c + Offset(-capR * 0.75f, -capR * 0.8f), size = Size(capR * 0.9f, capR * 0.6f),
    )
    if (active > 0.01f && level > 0.02f) {
        drawCircle(accent.copy(alpha = level * 0.25f), capR * 1.05f, c, style = Stroke(1.5f * density))
    }
}
