package com.voxcast.mic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors

private fun encodeQr(payload: String): BitMatrix? = runCatching {
    QRCodeWriter().encode(
        payload, BarcodeFormat.QR_CODE, 0, 0,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
}.getOrNull()

/**
 * Branded QR code: rounded "liquid" modules with a navy-to-violet gradient, custom finder
 * eyes and the VoxCast badge in the centre (error correction H keeps it scannable).
 * Every new payload plays a diagonal reveal animation; a light ring orbits the card.
 */
@Composable
fun StyledQrCode(payload: String, modifier: Modifier = Modifier) {
    val matrix = remember(payload) { encodeQr(payload) }
    val reveal = remember(payload) { Animatable(0f) }
    LaunchedEffect(payload) { reveal.animateTo(1f, tween(1_200, easing = FastOutSlowInEasing)) }

    val t = rememberInfiniteTransition(label = "qr")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(5_000, easing = LinearEasing)), label = "angle")

    val cardShape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawBehind {
                // Orbiting light around the card.
                val inset = -3.dp.toPx()
                drawRoundRect(
                    Brush.radialGradient(listOf(VoxColors.Cyan.copy(alpha = 0.35f), Color.Transparent), center, size.maxDimension * 0.75f),
                    topLeft = Offset(inset * 8, inset * 8), size = Size(size.width - inset * 16, size.height - inset * 16),
                    cornerRadius = CornerRadius(40.dp.toPx()),
                )
                // Rotate the gradient itself (not the shape) so the light travels around the border.
                val rotatingSweep = object : ShaderBrush() {
                    override fun createShader(size: Size): Shader {
                        val c = Offset(size.width / 2, size.height / 2)
                        return SweepGradientShader(
                            c, listOf(VoxColors.Cyan, Color.Transparent, VoxColors.Violet, Color.Transparent, VoxColors.Cyan),
                        ).apply { setLocalMatrix(android.graphics.Matrix().apply { setRotate(angle, c.x, c.y) }) }
                    }
                }
                drawRoundRect(
                    rotatingSweep,
                    topLeft = Offset(inset, inset), size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    style = Stroke(3.dp.toPx()),
                )
            }
            .clip(cardShape)
            .background(Color.White)
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (matrix != null) {
            // Own layer: the orbiting border redraws every frame without repainting the modules.
            Canvas(Modifier.fillMaxSize().graphicsLayer()) {
                val n = matrix.width
                val cell = size.width / n
                val p = reveal.value
                val brush = Brush.linearGradient(listOf(Color(0xFF0A1020), Color(0xFF1C2A6B), Color(0xFF3A1F7A)), Offset.Zero, Offset(size.width, size.height))
                val logoHalf = (n * 0.13f)
                val mid = n / 2f
                fun inFinder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
                fun inLogo(x: Int, y: Int) = x + 0.5f > mid - logoHalf && x + 0.5f < mid + logoHalf && y + 0.5f > mid - logoHalf && y + 0.5f < mid + logoHalf

                for (y in 0 until n) {
                    for (x in 0 until n) {
                        if (!matrix[x, y] || inFinder(x, y) || inLogo(x, y)) continue
                        val d = (x + y) / (2f * n)
                        val a = ((p * 1.35f - d) / 0.35f).coerceIn(0f, 1f)
                        if (a <= 0f) continue
                        drawCircle(brush, radius = cell * 0.47f * a, center = Offset((x + 0.5f) * cell, (y + 0.5f) * cell))
                        // Bridge horizontally/vertically adjacent modules for a liquid look.
                        if (a >= 1f) {
                            if (x + 1 < n && matrix[x + 1, y] && !inFinder(x + 1, y) && !inLogo(x + 1, y)) {
                                drawRect(brush, Offset((x + 0.5f) * cell, (y + 0.03f) * cell), Size(cell, cell * 0.94f))
                            }
                            if (y + 1 < n && matrix[x, y + 1] && !inFinder(x, y + 1) && !inLogo(x, y + 1)) {
                                drawRect(brush, Offset((x + 0.03f) * cell, (y + 0.5f) * cell), Size(cell * 0.94f, cell))
                            }
                        }
                    }
                }
                // Finder eyes.
                val eyeScale = ((p - 0.05f) / 0.4f).coerceIn(0f, 1f)
                listOf(Offset(0f, 0f), Offset((n - 7) * cell, 0f), Offset(0f, (n - 7) * cell)).forEach { o ->
                    val s = 7 * cell
                    val c = o + Offset(s / 2, s / 2)
                    val k = eyeScale
                    drawRoundRect(
                        brush,
                        topLeft = c - Offset(s / 2 * k - cell / 2, s / 2 * k - cell / 2),
                        size = Size((s - cell) * k, (s - cell) * k),
                        cornerRadius = CornerRadius(cell * 2.2f * k),
                        style = Stroke(cell),
                    )
                    val inner = 3 * cell * k
                    drawRoundRect(
                        Brush.linearGradient(listOf(VoxColors.CyanDeep, VoxColors.Violet), c - Offset(inner / 2, inner / 2), c + Offset(inner / 2, inner / 2)),
                        topLeft = c - Offset(inner / 2, inner / 2), size = Size(inner, inner),
                        cornerRadius = CornerRadius(cell * 1.1f),
                    )
                }
            }
            // Centre badge.
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val badge = maxWidth * 0.2f
                Box(
                    Modifier
                        .scale(((reveal.value - 0.5f) / 0.4f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(badge * 0.34f))
                        .background(Color.White)
                        .padding(badge * 0.08f),
                ) {
                    IconBadge(icon = VoxIcons.Microphone, brush = VoxColors.PrimaryGradient, size = badge * 0.84f)
                }
            }
        }
    }
}
