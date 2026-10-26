package com.zyropay.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.zyropay.app.R
import com.zyropay.app.data.CardStatus
import com.zyropay.app.data.PaymentCard
import com.zyropay.app.ui.theme.Inter
import com.zyropay.app.ui.theme.Z

// ------------------------------------------------------------------ charts

@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier, fill: Boolean = true) {
    if (values.size < 2) return
    Canvas(modifier) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - (v - min) / range * size.height * 0.9f - size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (fill) {
            val area = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f))))
        }
        drawPath(path, color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Interactive price chart: long-press / drag to scrub, reveals price label. */
@Composable
fun PriceChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    onScrub: (Int?) -> Unit = {},
) {
    if (values.size < 2) return
    val c = Z.colors
    var touchX by remember { mutableStateOf<Float?>(null) }
    val progress = remember(values.firstOrNull(), values.size) { Animatable(0f) }
    LaunchedEffect(values.firstOrNull(), values.size) { progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Canvas(
        modifier
            .pointerInput(values.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { touchX = it.x; onScrub(((it.x / size.width) * (values.size - 1)).toInt().coerceIn(0, values.size - 1)) },
                    onDragEnd = { touchX = null; onScrub(null) },
                    onDragCancel = { touchX = null; onScrub(null) },
                    onDrag = { change, _ ->
                        touchX = change.position.x.coerceIn(0f, size.width.toFloat())
                        onScrub(((touchX!! / size.width) * (values.size - 1)).toInt().coerceIn(0, values.size - 1))
                    },
                )
            }
            .pointerInput(values.size) {
                detectTapGestures(onPress = {
                    touchX = it.x
                    onScrub(((it.x / size.width) * (values.size - 1)).toInt().coerceIn(0, values.size - 1))
                    tryAwaitRelease()
                    touchX = null
                    onScrub(null)
                })
            },
    ) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        fun yOf(v: Float) = size.height - (v - min) / range * size.height * 0.82f - size.height * 0.09f
        // grid
        repeat(4) { i ->
            val y = size.height * (i + 0.5f) / 4f
            drawLine(c.divider, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }
        val count = ((values.size - 1) * progress.value).toInt() + 1
        val path = Path()
        for (i in 0 until count) {
            val x = i * stepX
            val y = yOf(values[i])
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val endX = (count - 1) * stepX
        val area = Path().apply { addPath(path); lineTo(endX, size.height); lineTo(0f, size.height); close() }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f))))
        drawPath(path, color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val tx = touchX
        if (tx != null) {
            val idx = ((tx / size.width) * (values.size - 1)).toInt().coerceIn(0, values.size - 1)
            val px = idx * stepX
            val py = yOf(values[idx])
            drawLine(c.textTertiary, Offset(px, 0f), Offset(px, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(color.copy(alpha = 0.25f), 10.dp.toPx(), Offset(px, py))
            drawCircle(Color.White, 5.dp.toPx(), Offset(px, py))
            drawCircle(color, 3.5.dp.toPx(), Offset(px, py))
        } else if (progress.value >= 1f) {
            val py = yOf(values.last())
            drawCircle(color.copy(alpha = 0.22f), 8.dp.toPx(), Offset(size.width, py))
            drawCircle(color, 3.5.dp.toPx(), Offset(size.width, py))
        }
    }
}

/** Donut chart for portfolio allocation. */
@Composable
fun DonutChart(parts: List<Pair<Float, Color>>, modifier: Modifier = Modifier, thickness: Float = 0.18f) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val track = Z.colors.surfaceVariant
    Canvas(modifier) {
        val total = parts.sumOf { it.first.toDouble() }.toFloat().takeIf { it > 0 } ?: 1f
        val stroke = size.minDimension * thickness
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        var start = -90f
        parts.forEach { (v, col) ->
            val sweep = v / total * 360f * anim.value
            if (sweep > 0.5f) drawArc(col, start, (sweep - 1.2f).coerceAtLeast(0.5f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
            start += sweep
        }
    }
}

// ------------------------------------------------------------------ QR

private fun qrBitmap(content: String, sizePx: Int, dark: Int, light: Int): ImageBitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    val n = matrix.width
    val cell = (sizePx / n).coerceAtLeast(1)
    val px = cell * n
    val bmp = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(light)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = dark }
    val r = cell * 0.42f
    fun finder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
    for (y in 0 until n) for (x in 0 until n) {
        if (matrix[x, y] && !finder(x, y)) {
            canvas.drawRoundRect(x * cell + 0.6f, y * cell + 0.6f, (x + 1) * cell - 0.6f, (y + 1) * cell - 0.6f, r, r, paint)
        }
    }
    // rounded finder patterns
    val stroke = android.graphics.Paint(paint).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = cell.toFloat() }
    listOf(0 to 0, n - 7 to 0, 0 to n - 7).forEach { (fx, fy) ->
        val l = fx * cell.toFloat(); val t = fy * cell.toFloat()
        canvas.drawRoundRect(l + cell / 2f, t + cell / 2f, l + 6.5f * cell, t + 6.5f * cell, cell * 1.8f, cell * 1.8f, stroke)
        canvas.drawRoundRect(l + 2 * cell, t + 2 * cell, l + 5f * cell, t + 5f * cell, cell * 0.9f, cell * 0.9f, paint)
    }
    // clear centre for logo
    val clear = android.graphics.Paint().apply { color = light }
    val c0 = px * 0.38f; val c1 = px * 0.62f
    canvas.drawRoundRect(c0, c0, c1, c1, cell * 2f, cell * 2f, clear)
    return bmp.asImageBitmap()
}

@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, dark: Color = Color(0xFF0B0F19)) {
    val bitmap = remember(content, dark) { qrBitmap(content, 720, android.graphics.Color.argb(255, (dark.red * 255).toInt(), (dark.green * 255).toInt(), (dark.blue * 255).toInt()), android.graphics.Color.WHITE) }
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Image(bitmap, contentDescription = "QR code", modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxWidth(0.2f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF5B3DF5)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.brand_logo_symbol_white), null, Modifier.fillMaxWidth(0.62f))
        }
    }
}

// ------------------------------------------------------------------ payment card

/**
 * Renders a Zyro Pay card with the brand artwork. Flips to the back side when [showBack] is true
 * and reveals the full number/CVV when [revealed] is true.
 */
@Composable
fun PaymentCardView(
    card: PaymentCard,
    modifier: Modifier = Modifier,
    revealed: Boolean = false,
    showBack: Boolean = false,
    elevation: Boolean = true,
) {
    val rotation by animateFloatAsState(if (showBack) 180f else 0f, spring(dampingRatio = 0.8f, stiffness = 220f), label = "flip")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(1.585f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .then(if (elevation) Modifier.shadow(16.dp, shape, ambientColor = Color(0x553E25C9), spotColor = Color(0x663E25C9)) else Modifier)
            .clip(shape),
    ) {
        if (rotation <= 90f) CardFront(card, revealed) else Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { CardBack(card, revealed) }
    }
}

@Composable
private fun CardFront(card: PaymentCard, revealed: Boolean) {
    val text = if (card.design.lightText) Color.White else Color(0xFF15161C)
    val sub = text.copy(alpha = 0.72f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(painterResource(card.design.art), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.offset(x = maxWidth * 0.812f, y = maxHeight * 0.792f).size(width = maxWidth * 0.131f, height = maxHeight * 0.106f),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (card.kind.name == "VIRTUAL") "DEBIT" else "PLATINUM", style = Z.type.tiny.copy(fontSize = 7.sp, letterSpacing = 1.2.sp), color = sub, maxLines = 1)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(if (card.kind.name == "VIRTUAL") "VIRTUAL" else "PHYSICAL", style = Z.type.tiny.copy(letterSpacing = 1.5.sp), color = sub)
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (revealed) card.formattedNumber else "••••  ••••  ••••  ${card.last4}",
                style = Z.type.number.copy(fontSize = 17.sp, letterSpacing = 1.6.sp, fontFamily = Inter, fontWeight = FontWeight.SemiBold),
                color = text,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("CARD HOLDER", style = Z.type.tiny.copy(fontSize = 8.sp, letterSpacing = 1.sp), color = sub)
                    Text(card.holder, style = Z.type.captionMedium.copy(letterSpacing = 0.8.sp), color = text, maxLines = 1)
                }
                Column(Modifier.padding(end = 70.dp)) {
                    Text("EXPIRES", style = Z.type.tiny.copy(fontSize = 8.sp, letterSpacing = 1.sp), color = sub)
                    Text(if (revealed) card.expiry else "••/••", style = Z.type.captionMedium.copy(fontFamily = FontFamily.Monospace), color = text)
                }
            }
        }
        if (card.frozen) {
            Image(painterResource(R.drawable.card_frozen_overlay), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x332E90FA)))
            Row(
                Modifier.align(Alignment.Center).background(Color(0xCC0B0F19), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZIcon(R.drawable.ic_card_freeze, size = 16.dp, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Frozen", style = Z.type.label, color = Color.White)
            }
        }
        if (card.status == CardStatus.SHIPPING) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)))
            Row(
                Modifier.align(Alignment.Center).background(Color(0xE6FFFFFF), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZIcon(R.drawable.ic_card_physical, size = 16.dp, tint = Color(0xFF0B0F19))
                Spacer(Modifier.width(6.dp))
                Text("In production · Shipping soon", style = Z.type.label, color = Color(0xFF0B0F19))
            }
        }
    }
}

@Composable
private fun CardBack(card: PaymentCard, revealed: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.card_back), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.offset(x = maxWidth * 0.672f, y = maxHeight * 0.418f).size(width = maxWidth * 0.145f, height = maxHeight * 0.174f),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (revealed) card.cvv else "•••", style = Z.type.number.copy(fontFamily = FontFamily.Monospace), color = Color(0xFF0B0F19))
        }
        Text("CVV", style = Z.type.tiny, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.offset(x = maxWidth * 0.72f, y = maxHeight * 0.62f))
        Text(
            "Issued by Zyro Pay Ltd. Use of this card is subject to the cardholder agreement.",
            style = Z.type.tiny.copy(fontSize = 7.sp, fontWeight = FontWeight.Normal),
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}

/** Small card thumbnail used in lists. */
@Composable
fun CardThumb(card: PaymentCard, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 48.dp, height = 31.dp).clip(RoundedCornerShape(5.dp))) {
        Image(painterResource(card.design.art), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Z.colors.primary) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "progress")
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Z.colors.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(p).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
    }
}

@Composable
fun StepIndicator(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        repeat(total) { i ->
            val active = i <= step
            val w by animateFloatAsState(if (active) 1f else 0f, tween(300), label = "step")
            Box(Modifier.weight(1f).padding(horizontal = 2.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Z.colors.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(w).height(4.dp).background(Z.colors.primary))
            }
        }
    }
}
