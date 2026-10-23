package com.voxcast.mic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.voxcast.mic.ui.theme.VoxColors

/**
 * Photorealistic broadcast microphone drawn entirely in Canvas: woven steel grille with
 * light falloff, machined centre band, knurled gold collar, satin-black tapered body,
 * ON-AIR LED and contact shadow. It glows and breathes with the live voice level.
 *
 * [level] is read only during drawing, so a 50 Hz level stream redraws without recomposing.
 */
@Composable
fun RealisticMicrophone(
    level: () -> Float,
    live: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = VoxColors.Cyan,
) {
    val t = rememberInfiniteTransition(label = "mic")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2_200, easing = LinearEasing)), label = "ring")
    val ledPulse by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "led")
    val liveAmount by animateFloatAsState(if (live) 1f else 0f, tween(450), label = "live")

    Canvas(modifier.aspectRatio(0.6f)) {
        val lv = level().coerceIn(0f, 1f) * liveAmount
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val headW = w * 0.58f
        val headH = h * 0.40f
        val headTop = h * 0.07f
        val headRect = Rect(cx - headW / 2, headTop, cx + headW / 2, headTop + headH)
        val headCenter = headRect.center

        // Sound rings and halo.
        if (liveAmount > 0.01f) {
            for (i in 0 until 3) {
                val p = (ring + i / 3f) % 1f
                val r = headW * 0.55f + p * w * 0.42f * (0.7f + lv * 0.6f)
                drawCircle(accent.copy(alpha = (1f - p) * 0.32f * liveAmount), r, headCenter, style = Stroke(width = 2.2f * density * (1f - p) + 0.5f))
            }
            val haloR = headW * (0.95f + lv * 0.55f)
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = (0.18f + lv * 0.45f) * liveAmount), Color.Transparent), headCenter, haloR),
                haloR, headCenter,
            )
        }

        val ringTop = headRect.bottom - headH * 0.05f
        val ringH = h * 0.052f
        val ringW = headW * 0.84f
        val bodyTop = ringTop + ringH - 1f
        val bodyTopW = headW * 0.70f
        val bodyBottomW = headW * 0.46f
        val bodyH = h * 0.38f
        val bodyBottom = bodyTop + bodyH

        // Contact shadow.
        val shadowC = Offset(cx, h * 0.965f)
        drawOval(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent), shadowC, w * 0.36f),
            topLeft = Offset(cx - w * 0.36f, shadowC.y - h * 0.025f),
            size = Size(w * 0.72f, h * 0.05f),
        )

        // XLR connector.
        val conW = bodyBottomW * 0.78f
        val conTop = bodyBottom - 2f
        val conH = h * 0.045f
        drawRoundRect(
            Brush.horizontalGradient(
                listOf(Color(0xFF2A2E35), Color(0xFF9AA2AE), Color(0xFF4C525C), Color(0xFF15171B)),
                startX = cx - conW / 2, endX = cx + conW / 2,
            ),
            topLeft = Offset(cx - conW / 2, conTop), size = Size(conW, conH), cornerRadius = CornerRadius(conH * 0.3f),
        )
        drawLine(Color.Black.copy(alpha = 0.6f), Offset(cx - conW / 2, conTop + conH * 0.45f), Offset(cx + conW / 2, conTop + conH * 0.45f), strokeWidth = 1.2f * density)

        drawBody(cx, bodyTop, bodyTopW, bodyBottomW, bodyBottom, liveAmount, ledPulse)
        drawCollar(cx, ringTop, ringW, ringH)

        withTransform({ scale(1f + lv * 0.025f, 1f + lv * 0.025f, headCenter) }) {
            drawGrilleHead(headRect, lv, accent)
        }
    }
}

private fun DrawScope.drawBody(
    cx: Float, top: Float, topW: Float, bottomW: Float, bottom: Float,
    liveAmount: Float, ledPulse: Float,
) {
    val r = bottomW * 0.22f
    val body = Path().apply {
        moveTo(cx - topW / 2, top)
        lineTo(cx + topW / 2, top)
        lineTo(cx + bottomW / 2, bottom - r)
        quadraticTo(cx + bottomW / 2, bottom, cx + bottomW / 2 - r, bottom)
        lineTo(cx - bottomW / 2 + r, bottom)
        quadraticTo(cx - bottomW / 2, bottom, cx - bottomW / 2, bottom - r)
        close()
    }
    // Satin cylinder: dark edges, soft key light, sharp specular stripe.
    drawPath(
        body,
        Brush.horizontalGradient(
            0f to Color(0xFF050608),
            0.16f to Color(0xFF1B1E25),
            0.34f to Color(0xFF474D59),
            0.42f to Color(0xFF9AA3B3),
            0.47f to Color(0xFF545B68),
            0.70f to Color(0xFF22262E),
            0.90f to Color(0xFF0D0F13),
            1f to Color(0xFF040506),
            startX = cx - topW / 2, endX = cx + topW / 2,
        ),
    )
    drawPath(body, Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.45f)), startY = top, endY = bottom))

    // Machined grooves near the base.
    for (k in 0 until 2) {
        val y = bottom - (bottom - top) * (0.16f + k * 0.05f)
        val frac = (y - top) / (bottom - top)
        val halfW = (topW + (bottomW - topW) * frac) / 2
        drawLine(Color.Black.copy(alpha = 0.7f), Offset(cx - halfW, y), Offset(cx + halfW, y), strokeWidth = 1.4f * density)
        drawLine(Color.White.copy(alpha = 0.10f), Offset(cx - halfW, y + 1.4f * density), Offset(cx + halfW, y + 1.4f * density), strokeWidth = 0.8f * density)
    }

    // ON-AIR plate with LED.
    val plateW = topW * 0.36f
    val plateH = (bottom - top) * 0.13f
    val plateTop = top + (bottom - top) * 0.16f
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF07080A), Color(0xFF1A1D22)), startY = plateTop, endY = plateTop + plateH),
        topLeft = Offset(cx - plateW / 2, plateTop), size = Size(plateW, plateH), cornerRadius = CornerRadius(plateH * 0.35f),
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.08f),
        topLeft = Offset(cx - plateW / 2, plateTop), size = Size(plateW, plateH), cornerRadius = CornerRadius(plateH * 0.35f),
        style = Stroke(0.8f * density),
    )
    val ledC = Offset(cx, plateTop + plateH / 2)
    val ledR = plateH * 0.24f
    val ledColor = lerp(Color(0xFF3A1E22), VoxColors.Live, liveAmount)
    if (liveAmount > 0.01f) {
        drawCircle(
            Brush.radialGradient(listOf(VoxColors.Live.copy(alpha = 0.7f * ledPulse * liveAmount), Color.Transparent), ledC, ledR * 4.5f),
            ledR * 4.5f, ledC,
        )
    }
    drawCircle(ledColor, ledR, ledC)
    drawCircle(Color.White.copy(alpha = 0.55f), ledR * 0.35f, ledC + Offset(-ledR * 0.3f, -ledR * 0.3f))
}

private fun DrawScope.drawCollar(cx: Float, top: Float, width: Float, height: Float) {
    val tl = Offset(cx - width / 2, top)
    val sz = Size(width, height)
    val corner = CornerRadius(height * 0.28f)
    drawRoundRect(
        Brush.horizontalGradient(
            0f to Color(0xFF3E2C10),
            0.18f to Color(0xFF8E6A2E),
            0.38f to Color(0xFFFFE7AA),
            0.46f to Color(0xFFE2B862),
            0.72f to Color(0xFF8A6427),
            1f to Color(0xFF32230C),
            startX = tl.x, endX = tl.x + width,
        ),
        topLeft = tl, size = sz, cornerRadius = corner,
    )
    // Knurling.
    val path = Path().apply { addRoundRect(RoundRect(Rect(tl, sz), corner)) }
    clipPath(path) {
        val step = 3.2f * density
        var x = tl.x
        while (x < tl.x + width) {
            drawLine(Color.Black.copy(alpha = 0.16f), Offset(x, top + height * 0.22f), Offset(x, top + height * 0.78f), strokeWidth = 0.9f * density)
            x += step
        }
    }
    drawLine(Color.Black.copy(alpha = 0.55f), Offset(tl.x, top + 0.6f), Offset(tl.x + width, top + 0.6f), strokeWidth = 1.2f * density)
    drawLine(Color.White.copy(alpha = 0.35f), Offset(tl.x + width * 0.08f, top + height * 0.16f), Offset(tl.x + width * 0.92f, top + height * 0.16f), strokeWidth = 0.7f * density)
}

private fun DrawScope.drawGrilleHead(rect: Rect, level: Float, accent: Color) {
    val headW = rect.width
    val corner = CornerRadius(headW / 2)
    val path = Path().apply { addRoundRect(RoundRect(rect, corner)) }
    val light = Offset(rect.left + headW * 0.32f, rect.top + rect.height * 0.26f)

    clipPath(path) {
        // Cavity behind the mesh, with the capsule faintly visible.
        drawRect(Brush.radialGradient(listOf(Color(0xFF262A32), Color(0xFF07080A)), rect.center, headW * 0.75f), rect.topLeft, rect.size)
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFF3A3F49), Color(0xFF101216)), rect.center, headW * 0.36f),
            headW * 0.36f, rect.center,
        )
        if (level > 0.01f) {
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.55f * level), Color.Transparent), rect.center, headW * 0.6f),
                headW * 0.6f, rect.center,
            )
        }

        // Woven steel wires: two diagonal sets lit by the same key light.
        val wire = Brush.radialGradient(
            0f to Color(0xFFFFFFFF),
            0.25f to Color(0xFFD5DBE4),
            0.6f to Color(0xFF7E8693),
            1f to Color(0xFF2B3038),
            center = light, radius = headW * 1.05f,
        )
        val step = 4.6f * density
        val stroke = 1.35f * density
        val span = rect.width + rect.height
        var d = -span
        while (d < span) {
            drawLine(wire, Offset(rect.left + d, rect.top), Offset(rect.left + d + rect.height, rect.bottom), strokeWidth = stroke)
            drawLine(wire, Offset(rect.right - d, rect.top), Offset(rect.right - d - rect.height, rect.bottom), strokeWidth = stroke)
            d += step
        }

        // Machined centre band.
        val bandH = rect.height * 0.075f
        val bandTop = rect.top + rect.height * 0.5f - bandH / 2
        drawRect(
            Brush.horizontalGradient(
                0f to Color(0xFF2B3038), 0.3f to Color(0xFFBFC6D1), 0.4f to Color(0xFFFFFFFF),
                0.55f to Color(0xFF8C94A1), 1f to Color(0xFF1E2228),
                startX = rect.left, endX = rect.right,
            ),
            topLeft = Offset(rect.left, bandTop), size = Size(rect.width, bandH),
        )
        drawLine(Color.Black.copy(alpha = 0.5f), Offset(rect.left, bandTop), Offset(rect.right, bandTop), strokeWidth = 1f * density)
        drawLine(Color.Black.copy(alpha = 0.6f), Offset(rect.left, bandTop + bandH), Offset(rect.right, bandTop + bandH), strokeWidth = 1f * density)

        // Spherical shading: darken towards the far edges.
        drawRect(
            Brush.radialGradient(
                0f to Color.Transparent, 0.55f to Color.Black.copy(alpha = 0.12f), 1f to Color.Black.copy(alpha = 0.72f),
                center = Offset(rect.center.x - headW * 0.08f, rect.center.y - rect.height * 0.08f), radius = headW * 0.82f,
            ),
            rect.topLeft, rect.size,
        )
        // Specular highlight.
        drawOval(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.42f), Color.Transparent), light, headW * 0.26f),
            topLeft = Offset(light.x - headW * 0.24f, light.y - rect.height * 0.12f),
            size = Size(headW * 0.48f, rect.height * 0.24f),
        )
    }
    // Rim.
    drawPath(
        path,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.7f)), startY = rect.top, endY = rect.bottom),
        style = Stroke(width = 2.2f * density),
    )
}
