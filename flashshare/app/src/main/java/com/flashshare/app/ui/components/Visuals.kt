package com.flashshare.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// region liquid progress
/**
 * A glass sphere that fills with animated liquid up to [progress]; shows % and speed inside.
 * When [completed] it turns green and a check mark pops in.
 */
@Composable
fun LiquidProgress(progress: Float, speedText: String, completed: Boolean, failed: Boolean, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(if (completed) 1f else progress.coerceIn(0f, 1f), tween(500, easing = FastOutSlowInEasing), label = "lvl")
    val t = rememberInfiniteTransition(label = "liquid")
    val phase by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ph")
    val bubbles by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "b")
    val c1 by animateColorAsState(when { completed -> Color(0xFF2EE6A6); failed -> Color(0xFFFF8A80); else -> Color(0xFF5B8CFF) }, tween(600), label = "c1")
    val c2 by animateColorAsState(when { completed -> Color(0xFF00B386); failed -> Color(0xFFFF3B30); else -> Color(0xFF7B3DFF) }, tween(600), label = "c2")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val bubbleSeeds = remember { List(9) { Triple(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat() * 0.6f) } }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            // outer glow ring
            drawCircle(Brush.radialGradient(listOf(c1.copy(alpha = 0.25f), Color.Transparent), c, r * 1.05f), r * 1.05f, c)
            drawCircle(track, r * 0.94f, c)
            drawArc(Brush.sweepGradient(listOf(c1, c2, c1), c), -90f, 360f * level, false,
                Offset(c.x - r * 0.97f, c.y - r * 0.97f), Size(r * 1.94f, r * 1.94f), style = Stroke(r * 0.05f, cap = StrokeCap.Round))
            val inner = r * 0.86f
            val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, inner)) }
            clipPath(circle) {
                drawCircle(c1.copy(alpha = 0.08f), inner, c)
                val top = c.y + inner - level * inner * 2
                fun wave(amp: Float, shift: Float, color: Brush) {
                    val p = Path()
                    p.moveTo(c.x - inner, size.height)
                    var x = c.x - inner
                    while (x <= c.x + inner + 4) {
                        val y = top + amp * sin((x / inner) * 2.2f * PI.toFloat() + phase + shift)
                        p.lineTo(x, y); x += 4f
                    }
                    p.lineTo(c.x + inner, size.height); p.close()
                    drawPath(p, color)
                }
                val amp = if (level <= 0.001f || level >= 0.999f) 0f else inner * 0.05f
                wave(amp, PI.toFloat(), Brush.verticalGradient(listOf(c1.copy(alpha = 0.5f), c2.copy(alpha = 0.6f)), startY = top, endY = c.y + inner))
                wave(amp * 0.8f, 0f, Brush.verticalGradient(listOf(c1, c2), startY = top, endY = c.y + inner))
                // rising bubbles inside the liquid
                if (level > 0.08f) bubbleSeeds.forEach { (bx, off, s) ->
                    val f = (bubbles + off) % 1f
                    val y = c.y + inner - f * level * inner * 2
                    drawCircle(Color.White.copy(alpha = 0.35f * (1f - f)), inner * 0.025f * s + 2f, Offset(c.x - inner * 0.6f + bx * inner * 1.2f, y))
                }
            }
            // glass highlight
            drawArc(Color.White.copy(alpha = 0.35f), 200f, 60f, false, Offset(c.x - inner * 0.8f, c.y - inner * 0.8f), Size(inner * 1.6f, inner * 1.6f),
                style = Stroke(inner * 0.06f, cap = StrokeCap.Round))
        }
        AnimatedVisibility(completed, enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = 300f)) + fadeIn(), exit = fadeOut()) {
            Box(Modifier.size(96.dp).shadow(12.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color(0xFF00B386), modifier = Modifier.size(64.dp))
            }
        }
        if (!completed) {
            val onLiquid = level > 0.45f
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(progress * 100).toInt()}%", fontSize = 46.sp, fontWeight = FontWeight.Black,
                    color = if (onLiquid) Color.White else MaterialTheme.colorScheme.onSurface)
                Text(speedText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (onLiquid) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary)
            }
        }
    }
}
// endregion

// region QR
private fun qrMatrix(content: String): BitMatrix {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0, EncodeHintType.CHARACTER_SET to "UTF-8")
    // Ask for a 1-pixel-per-module matrix so we can draw our own shapes.
    return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
}

/** A designer QR code: round dots, gradient rounded finder eyes, space in the middle for the avatar. */
@Composable
fun StyledQr(content: String, modifier: Modifier = Modifier) {
    val matrix by produceState<BitMatrix?>(null, content) { value = withContext(Dispatchers.Default) { runCatching { qrMatrix(content) }.getOrNull() } }
    val m = matrix
    Box(modifier, contentAlignment = Alignment.Center) {
        if (m == null) { CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp); return@Box }
        Canvas(Modifier.fillMaxSize()) {
            val n = m.width
            val cell = size.minDimension / n
            val dark = Color(0xFF111827)
            val centerStart = n * 0.36f; val centerEnd = n * 0.64f
            fun inFinder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
            for (y in 0 until n) for (x in 0 until n) {
                if (!m.get(x, y) || inFinder(x, y)) continue
                if (x > centerStart && x < centerEnd && y > centerStart && y < centerEnd) continue
                drawCircle(dark, cell * 0.46f, Offset((x + 0.5f) * cell, (y + 0.5f) * cell))
            }
            val eye = Brush.linearGradient(listOf(Color(0xFF3D7BFF), Color(0xFF8A3DFF)))
            listOf(0 to 0, n - 7 to 0, 0 to n - 7).forEach { (fx, fy) ->
                val o = Offset(fx * cell, fy * cell)
                drawRoundRect(eye, o + Offset(cell * 0.5f, cell * 0.5f), Size(cell * 6f, cell * 6f), CornerRadius(cell * 2f), style = Stroke(cell))
                drawRoundRect(eye, o + Offset(cell * 2f, cell * 2f), Size(cell * 3f, cell * 3f), CornerRadius(cell * 1.1f))
            }
        }
    }
}

/** White QR card with a rotating gradient glow border and the user's avatar in the middle. */
@Composable
fun QrCard(content: String, center: @Composable () -> Unit, footer: @Composable ColumnScope.() -> Unit, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "qrcard")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rot")
    val breathe by t.animateFloat(0.97f, 1.0f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "br")
    Box(modifier.scale(breathe), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            rotate(rot) {
                drawCircle(
                    Brush.sweepGradient(listOf(Color(0xFF3D7BFF), Color(0xFF19C3FF), Color(0xFFB04DFF), Color(0xFFFF4D9D), Color(0xFF3D7BFF))),
                    radius = size.maxDimension * 0.75f, alpha = 0.9f,
                )
            }
        }
        Column(
            Modifier.padding(3.dp).clip(RoundedCornerShape(33.dp)).background(Color.White).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                StyledQr(content, Modifier.fillMaxSize())
                Box(Modifier.size(62.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White).padding(4.dp)) { center() }
            }
            footer()
        }
    }
}
// endregion

// region connection animations
/** "Creating ultra-fast connection": a radar sweep around a glowing device with signal rings and a live checklist. */
@Composable
fun HostingAnimation(modifier: Modifier = Modifier, steps: List<String> = listOf("Starting Wi-Fi Direct", "Locking to the 5 GHz band", "Generating secure QR code")) {
    val t = rememberInfiniteTransition(label = "host")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "sw")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "ring")
    val glow by t.animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (step < steps.size - 1) { delay(900); step++ } }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2
                // grid rings
                for (i in 1..4) drawCircle(primary.copy(alpha = 0.10f), r * i / 4f, c, style = Stroke(1.dp.toPx()))
                // expanding signal rings
                for (i in 0 until 3) {
                    val f = (ring + i / 3f) % 1f
                    drawCircle(secondary.copy(alpha = (1f - f) * 0.45f), r * (0.22f + 0.78f * f), c, style = Stroke((3f - 2f * f).dp.toPx()))
                }
                // radar sweep
                rotate(sweep, c) {
                    drawArc(
                        Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, primary.copy(alpha = 0.0f), primary.copy(alpha = 0.45f)), c),
                        -90f, 90f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2),
                    )
                    drawLine(primary.copy(alpha = 0.8f), c, Offset(c.x, c.y - r), 2.dp.toPx(), cap = StrokeCap.Round)
                }
                // orbiting particles
                for (i in 0 until 6) {
                    val a = Math.toRadians((sweep * (if (i % 2 == 0) 1 else -1) + i * 60).toDouble())
                    val rr = r * (0.55f + 0.08f * (i % 3))
                    drawCircle(Color.White, 3.dp.toPx(), Offset(c.x + rr * cos(a).toFloat(), c.y + rr * sin(a).toFloat()), alpha = 0.9f)
                    drawCircle(primary, 2.dp.toPx(), Offset(c.x + rr * cos(a).toFloat(), c.y + rr * sin(a).toFloat()))
                }
            }
            Box(Modifier.size(120.dp).scale(glow).clip(CircleShape).background(Brush.radialGradient(listOf(primary.copy(alpha = 0.35f), Color.Transparent))))
            GlossyIcon(Icons.Rounded.WifiTethering, Brand.main, 84.dp, corner = 42.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Creating ultra-fast connection", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        StepList(steps, step)
    }
}

@Composable
fun StepList(steps: List<String>, current: Int, modifier: Modifier = Modifier) {
    val extra = LocalExtra.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, s ->
            val done = i < current
            val active = i == current
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = if (i <= current) 1f else 0.45f }) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    when {
                        done -> Box(Modifier.size(22.dp).clip(CircleShape).background(extra.success), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                        active -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp, strokeCap = StrokeCap.Round)
                        else -> Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(s, style = MaterialTheme.typography.bodyMedium, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Two phones with a glowing particle stream travelling between them. */
@Composable
fun PhonesLink(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    active: Boolean,
    modifier: Modifier = Modifier,
    reverse: Boolean = false,
    phoneWidth: Dp = 96.dp,
) {
    val t = rememberInfiniteTransition(label = "link")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "p")
    val tilt by t.animateFloat(-3f, 3f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tilt")
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        PhoneMockup(Modifier.rotate(-6f + tilt), width = phoneWidth, screen = Brand.send) { left() }
        Canvas(Modifier.weight(1f).height(90.dp)) {
            val w = size.width; val h = size.height
            // dashed arc guide
            val guide = Path().apply { moveTo(0f, h * 0.62f); quadraticTo(w / 2, -h * 0.25f, w, h * 0.62f) }
            drawPath(guide, primary.copy(alpha = 0.18f), style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
            if (active) for (i in 0 until 7) {
                var f = (p + i / 7f) % 1f
                if (reverse) f = 1f - f
                val x = w * f
                val y = (1 - f) * (1 - f) * h * 0.62f + 2 * (1 - f) * f * (-h * 0.25f) + f * f * h * 0.62f
                val edge = sin(f * PI).toFloat()
                drawCircle(secondary.copy(alpha = 0.25f * edge), 9.dp.toPx() * edge, Offset(x, y))
                drawCircle(Color.White.copy(alpha = edge), 3.5.dp.toPx() * edge + 1f, Offset(x, y))
                drawCircle(primary.copy(alpha = edge), 2.dp.toPx() * edge + 1f, Offset(x, y))
            }
        }
        PhoneMockup(Modifier.rotate(6f - tilt), width = phoneWidth, screen = Brand.receive) { right() }
    }
}
// endregion

// region cleaner scenes
/** Rocket flying through a star field – used while the cleaner scans. */
@Composable
fun RocketScene(progress: Float, modifier: Modifier = Modifier, launching: Boolean = false, done: Boolean = false) {
    val t = rememberInfiniteTransition(label = "rocket")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "time")
    val shake by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse), label = "shake")
    val flame by t.animateFloat(0.75f, 1.15f, infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse), label = "flame")
    val lift = remember { Animatable(0f) }
    LaunchedEffect(launching) { if (launching) lift.animateTo(1f, tween(900, easing = FastOutLinearInEasing)) }
    val stars = remember { List(60) { Triple(Random.nextFloat(), Random.nextFloat(), 0.3f + Random.nextFloat() * 0.7f) } }
    Box(modifier.clip(RoundedCornerShape(32.dp)).background(Brush.verticalGradient(listOf(Color(0xFF0B1030), Color(0xFF1D1B5A), Color(0xFF3A1F73))))) {
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach { (sx, sy, sp) ->
                val y = ((sy + time * sp * 1.6f) % 1f) * size.height
                val len = 12.dp.toPx() * sp
                drawLine(Color.White.copy(alpha = 0.25f + 0.5f * sp), Offset(sx * size.width, y), Offset(sx * size.width, y + len), (1.2f * sp).dp.toPx(), cap = StrokeCap.Round)
            }
            // progress halo
            val c = Offset(size.width / 2, size.height * 0.5f)
            val r = size.minDimension * 0.36f
            drawCircle(Color.White.copy(alpha = 0.06f), r, c, style = Stroke(10.dp.toPx()))
            drawArc(
                if (done) Brush.sweepGradient(listOf(Color(0xFF2EE6A6), Color(0xFF00C48C), Color(0xFF2EE6A6)), c)
                else Brush.sweepGradient(listOf(Color(0xFF3DEBB0), Color(0xFF19C3FF), Color(0xFF3DEBB0)), c), -90f, 360f * progress, false,
                Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            translate(shake.dp.toPx() * 0.6f, -lift.value * size.height * 0.9f) { drawRocket(c, size.minDimension * 0.18f, flame) }
        }
        // Finished: green tick medal on the ring.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val r = minOf(maxWidth, maxHeight) * 0.36f
            androidx.compose.animation.AnimatedVisibility(
                done, Modifier.align(Alignment.Center).offset(x = r * 0.72f, y = r * 0.72f),
                enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = 300f)) + fadeIn(),
            ) {
                Box(
                    Modifier.size(56.dp).shadow(12.dp, CircleShape, spotColor = Color(0xFF00C48C)).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF2EE6A6), Color(0xFF00A877)))).border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
            }
        }
    }
}

private fun DrawScope.drawRocket(c: Offset, s: Float, flame: Float) {
    // flame
    val f = Path().apply {
        moveTo(c.x - s * 0.22f, c.y + s * 0.62f)
        quadraticTo(c.x, c.y + s * (0.62f + 0.9f * flame), c.x + s * 0.22f, c.y + s * 0.62f); close()
    }
    drawPath(f, Brush.verticalGradient(listOf(Color(0xFFFFE066), Color(0xFFFF7A00), Color(0x00FF3D00)), startY = c.y + s * 0.5f, endY = c.y + s * 1.6f))
    // fins
    val finL = Path().apply { moveTo(c.x - s * 0.3f, c.y + s * 0.15f); lineTo(c.x - s * 0.62f, c.y + s * 0.62f); lineTo(c.x - s * 0.3f, c.y + s * 0.52f); close() }
    val finR = Path().apply { moveTo(c.x + s * 0.3f, c.y + s * 0.15f); lineTo(c.x + s * 0.62f, c.y + s * 0.62f); lineTo(c.x + s * 0.3f, c.y + s * 0.52f); close() }
    drawPath(finL, Color(0xFFFF4D6D)); drawPath(finR, Color(0xFFFF4D6D))
    // body
    val body = Path().apply {
        moveTo(c.x, c.y - s)
        cubicTo(c.x + s * 0.5f, c.y - s * 0.55f, c.x + s * 0.36f, c.y + s * 0.3f, c.x + s * 0.28f, c.y + s * 0.62f)
        lineTo(c.x - s * 0.28f, c.y + s * 0.62f)
        cubicTo(c.x - s * 0.36f, c.y + s * 0.3f, c.x - s * 0.5f, c.y - s * 0.55f, c.x, c.y - s); close()
    }
    drawPath(body, Brush.horizontalGradient(listOf(Color(0xFFE8ECF7), Color.White, Color(0xFFC9D2E8)), startX = c.x - s * 0.4f, endX = c.x + s * 0.4f))
    // nose
    val nose = Path().apply { moveTo(c.x, c.y - s); cubicTo(c.x + s * 0.25f, c.y - s * 0.8f, c.x + s * 0.32f, c.y - s * 0.62f, c.x + s * 0.33f, c.y - s * 0.55f); lineTo(c.x - s * 0.33f, c.y - s * 0.55f); cubicTo(c.x - s * 0.32f, c.y - s * 0.62f, c.x - s * 0.25f, c.y - s * 0.8f, c.x, c.y - s); close() }
    drawPath(nose, Color(0xFFFF4D6D))
    // window
    drawCircle(Color(0xFF2F5BFF), s * 0.17f, Offset(c.x, c.y - s * 0.18f))
    drawCircle(Color(0xFF8FD3FF), s * 0.12f, Offset(c.x, c.y - s * 0.18f))
    drawCircle(Color.White.copy(alpha = 0.7f), s * 0.04f, Offset(c.x - s * 0.05f, c.y - s * 0.23f))
}

/** Junk files spiral into a glowing vortex while cleaning. */
@Composable
fun CleanVortex(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "vortex")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin")
    val suck by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "suck")
    val icons = listOf(Icons.Rounded.Description, Icons.Rounded.Image, Icons.Rounded.Movie, Icons.Rounded.Android, Icons.Rounded.MusicNote, Icons.Rounded.FolderZip)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2
            rotate(spin, c) {
                for (i in 0 until 5) drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF3DEBB0).copy(alpha = 0.7f), Color.Transparent), c),
                    i * 72f, 120f, false, Offset(c.x - r * (0.3f + i * 0.14f), c.y - r * (0.3f + i * 0.14f)),
                    Size(r * 2 * (0.3f + i * 0.14f), r * 2 * (0.3f + i * 0.14f)), style = Stroke((6 - i).dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFF3DEBB0), Color(0xFF00A88A).copy(alpha = 0.4f), Color.Transparent), c, r * 0.32f), r * 0.32f, c)
        }
        icons.forEachIndexed { i, icon ->
            val f = (suck + i / icons.size.toFloat()) % 1f
            val ang = Math.toRadians((i * 60 + f * 300).toDouble())
            val rr = (1f - f) * 120f
            Icon(icon, null, tint = Color.White, modifier = Modifier
                .graphicsLayer {
                    translationX = (rr * cos(ang)).toFloat() * density; translationY = (rr * sin(ang)).toFloat() * density
                    scaleX = 1f - f * 0.8f; scaleY = scaleX; alpha = 1f - f; rotationZ = f * 360f
                }
                .size(28.dp).clip(RoundedCornerShape(8.dp)).background(categoryBrushFor(i)).padding(5.dp))
        }
    }
}

private fun categoryBrushFor(i: Int): Brush = listOf(IconBrushes.violet, IconBrushes.cyan, IconBrushes.pink, IconBrushes.mint, IconBrushes.orange, IconBrushes.blue)[i % 6]

/** One-shot burst of colorful confetti. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(1800, easing = LinearOutSlowInEasing)) }
    val parts = remember {
        val colors = listOf(Color(0xFF3D7BFF), Color(0xFFFF4D9D), Color(0xFFFFC233), Color(0xFF3DEBB0), Color(0xFFB04DFF), Color(0xFF19C3FF))
        List(70) { Pair(Random.nextFloat() * 2 * PI.toFloat(), 0.4f + Random.nextFloat()) to colors[it % colors.size] }
    }
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height * 0.4f)
        val p = a.value
        parts.forEachIndexed { i, (ap, col) ->
            val (ang, sp) = ap
            val d = p * size.minDimension * 0.6f * sp
            val x = c.x + cos(ang) * d
            val y = c.y + sin(ang) * d + p * p * size.height * 0.35f
            rotate(p * 720f * (if (i % 2 == 0) 1 else -1), Offset(x, y)) {
                drawRect(col.copy(alpha = 1f - p), Offset(x - 4f, y - 7f), Size(8f, 14f))
            }
        }
    }
}
// endregion

// region gauge
/** Speedometer with ticks, gradient arc and needle. [value] 0..1. */
@Composable
fun SpeedGauge(value: Float, label: String, modifier: Modifier = Modifier, onDark: Boolean = true) {
    val v by animateFloatAsState(value.coerceIn(0f, 1f), spring(dampingRatio = 0.5f, stiffness = 60f), label = "gauge")
    val fg = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2 * 0.9f
            val c = Offset(size.width / 2, size.height / 2)
            val start = 140f; val sweep = 260f
            drawArc(fg.copy(alpha = 0.15f), start, sweep, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(r * 0.12f, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(Color(0xFF3DEBB0), Color(0xFF19C3FF), Color(0xFFFFC233), Color(0xFFFF4D6D), Color(0xFF3DEBB0)), c),
                start, sweep * v, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(r * 0.12f, cap = StrokeCap.Round))
            for (i in 0..10) {
                val a = Math.toRadians((start + sweep * i / 10f).toDouble())
                val o = r * 0.8f; val inn = r * (if (i % 5 == 0) 0.62f else 0.7f)
                drawLine(fg.copy(alpha = 0.5f), Offset(c.x + inn * cos(a).toFloat(), c.y + inn * sin(a).toFloat()),
                    Offset(c.x + o * cos(a).toFloat(), c.y + o * sin(a).toFloat()), 2f, cap = StrokeCap.Round)
            }
            val a = Math.toRadians((start + sweep * v).toDouble())
            drawLine(fg, c, Offset(c.x + r * 0.72f * cos(a).toFloat(), c.y + r * 0.72f * sin(a).toFloat()), r * 0.05f, cap = StrokeCap.Round)
            drawCircle(fg, r * 0.09f, c)
        }
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
// endregion

@Suppress("unused")
private val keepBorder = Modifier.border(0.dp, Color.Transparent)
