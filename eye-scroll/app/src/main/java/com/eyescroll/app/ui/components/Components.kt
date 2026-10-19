package com.eyescroll.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eyescroll.app.tracking.GazeSnapshot
import com.eyescroll.app.tracking.GazeZone
import com.eyescroll.app.ui.theme.EsColors
import kotlin.math.cos
import kotlin.math.sin

/** Slowly drifting colour blobs behind every screen. */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val phase by t.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier.fillMaxSize().background(EsColors.Background)) {
        val w = size.width
        val h = size.height
        val c1 = Offset(w * (0.2f + 0.12f * cos(phase)), h * (0.12f + 0.05f * sin(phase)))
        val c2 = Offset(w * (0.85f + 0.08f * sin(phase * 1.3f)), h * (0.45f + 0.08f * cos(phase)))
        val c3 = Offset(w * (0.3f + 0.1f * sin(phase * 0.7f)), h * (0.95f + 0.04f * cos(phase * 1.1f)))
        drawCircle(Brush.radialGradient(listOf(EsColors.Primary.copy(alpha = 0.16f), Color.Transparent), c1, w * 0.75f), w * 0.75f, c1)
        drawCircle(Brush.radialGradient(listOf(EsColors.Secondary.copy(alpha = 0.16f), Color.Transparent), c2, w * 0.7f), w * 0.7f, c2)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF4F8CFF).copy(alpha = 0.10f), Color.Transparent), c3, w * 0.8f), w * 0.8f, c3)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    padding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val borderBrush = if (highlighted) {
        Brush.linearGradient(listOf(EsColors.Primary.copy(alpha = 0.7f), EsColors.Secondary.copy(alpha = 0.5f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f)))
    }
    var m = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(EsColors.SurfaceHigh.copy(alpha = 0.78f), EsColors.Surface.copy(alpha = 0.82f)),
            ),
        )
        .border(1.dp, borderBrush, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "btn")
    Box(
        modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) EsColors.Accent else Brush.linearGradient(listOf(EsColors.SurfaceHigh, EsColors.SurfaceHigh)),
            )
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (enabled) Color(0xFF041019) else EsColors.TextFaint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color(0xFF041019) else EsColors.TextFaint,
            )
        }
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "ghost")
    Box(
        modifier
            .scale(scale)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = EsColors.Text, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = EsColors.Text)
        }
    }
}

/**
 * Realistic animated eye. With [lookY] = null it plays a demo loop (look up, look down, blink);
 * otherwise the iris follows [lookY] (-1 = up, 1 = down).
 */
@Composable
fun AnimatedEye(modifier: Modifier = Modifier, lookY: Float? = null, glowColor: Color = EsColors.Primary) {
    val t = rememberInfiniteTransition(label = "eye")
    val demoLook by t.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 5200
                0f at 0
                -1f at 700 using FastOutSlowInEasing
                -1f at 1700
                0f at 2300 using FastOutSlowInEasing
                1f at 3000 using FastOutSlowInEasing
                1f at 4000
                0f at 4600 using FastOutSlowInEasing
            },
        ),
        label = "look",
    )
    val blink by t.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 5200
                1f at 0
                1f at 4720
                0.06f at 4820
                1f at 4960
            },
        ),
        label = "blink",
    )
    val glow by t.animateFloat(0.25f, 0.45f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")
    val target = lookY ?: demoLook
    val look by animateFloatAsState(target, spring(dampingRatio = 0.8f, stiffness = 300f), label = "follow")
    val open = if (lookY == null) blink else 1f

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val ew = w * 0.44f
        val eh = h * 0.30f * open.coerceAtLeast(0.04f)

        drawCircle(
            Brush.radialGradient(listOf(glowColor.copy(alpha = glow), Color.Transparent), Offset(cx, cy), w * 0.62f),
            w * 0.62f,
            Offset(cx, cy),
        )

        val eye = Path().apply {
            moveTo(cx - ew, cy)
            quadraticTo(cx, cy - eh * 2f, cx + ew, cy)
            quadraticTo(cx, cy + eh * 2f, cx - ew, cy)
            close()
        }
        drawPath(eye, Brush.verticalGradient(listOf(Color(0xFFF6F8FF), Color(0xFFC6D2EE)), cy - eh, cy + eh))

        clipPath(eye) {
            val ir = h * 0.21f
            val ic = Offset(cx, cy + look * h * 0.13f)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF8FF0FF), EsColors.Primary, Color(0xFF2A6BFF), EsColors.Secondary),
                    ic, ir,
                ),
                ir, ic,
            )
            // iris fibres
            for (i in 0 until 24) {
                val a = (i / 24f) * 2f * Math.PI.toFloat()
                drawLine(
                    Color.White.copy(alpha = 0.10f),
                    Offset(ic.x + cos(a) * ir * 0.5f, ic.y + sin(a) * ir * 0.5f),
                    Offset(ic.x + cos(a) * ir * 0.95f, ic.y + sin(a) * ir * 0.95f),
                    strokeWidth = 1.2f,
                )
            }
            drawCircle(Color(0xFF050912), ir * 0.44f, ic)
            drawCircle(Color.White.copy(alpha = 0.9f), ir * 0.14f, Offset(ic.x - ir * 0.32f, ic.y - ir * 0.34f))
            drawCircle(Color.White.copy(alpha = 0.5f), ir * 0.07f, Offset(ic.x + ir * 0.28f, ic.y + ir * 0.22f))
            // upper-lid shadow
            drawRect(
                Brush.verticalGradient(listOf(Color(0x66000814), Color.Transparent), cy - eh * 1.2f, cy - eh * 0.2f),
            )
        }
        drawPath(eye, EsColors.Accent, style = Stroke(width = w * 0.016f))
    }
}

/** Vertical meter: where the tracker thinks you look, with the up/down trigger zones. */
@Composable
fun GazeMeter(snapshot: GazeSnapshot, modifier: Modifier = Modifier) {
    val y by animateFloatAsState(snapshot.gazeY, spring(stiffness = 900f), label = "gy")
    val dotColor by animateColorAsState(
        when {
            !snapshot.faceFound -> EsColors.TextFaint
            snapshot.zone == GazeZone.UP -> Color(0xFF8FF0FF)
            snapshot.zone == GazeZone.DOWN -> Color(0xFFA48CFF)
            else -> Color.White
        },
        label = "dot",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val lo = -0.2f
        val hi = 1.2f
        fun map(v: Float) = ((v - lo) / (hi - lo)).coerceIn(0f, 1f) * h
        val r = CornerRadius(w / 2, w / 2)
        drawRoundRect(Color.White.copy(alpha = 0.06f), cornerRadius = r)
        val up = map(snapshot.upThreshold)
        val down = map(snapshot.downThreshold)
        drawRoundRect(
            Brush.verticalGradient(listOf(EsColors.Primary.copy(alpha = 0.55f), EsColors.Primary.copy(alpha = 0.12f)), 0f, up),
            size = Size(w, up), cornerRadius = r,
        )
        drawRoundRect(
            Brush.verticalGradient(listOf(EsColors.Secondary.copy(alpha = 0.12f), EsColors.Secondary.copy(alpha = 0.55f)), down, h),
            topLeft = Offset(0f, down), size = Size(w, h - down), cornerRadius = r,
        )
        val dy = map(y)
        drawCircle(dotColor.copy(alpha = 0.25f), w * 0.95f, Offset(w / 2, dy))
        drawCircle(dotColor, w * 0.55f, Offset(w / 2, dy))
    }
}

/** Large circular power button with a rotating gradient ring while active. */
@Composable
fun PowerOrb(active: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 168.dp) {
    val t = rememberInfiniteTransition(label = "orb")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "angle")
    val breathe by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    val ringAlpha by animateFloatAsState(if (active) 1f else 0.35f, tween(400), label = "ring")

    Box(
        modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            if (active) {
                drawCircle(
                    Brush.radialGradient(listOf(EsColors.Primary.copy(alpha = 0.35f), Color.Transparent), center, r * breathe),
                    r * breathe,
                )
            }
            rotate(if (active) angle else 0f) {
                drawCircle(
                    Brush.sweepGradient(
                        listOf(EsColors.Primary, Color(0xFF4F8CFF), EsColors.Secondary, EsColors.Primary.copy(alpha = 0.2f), EsColors.Primary),
                    ),
                    r * 0.86f,
                    style = Stroke(width = r * 0.06f, cap = StrokeCap.Round),
                    alpha = ringAlpha,
                )
            }
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF1B2747), Color(0xFF0A0F1E)), center, r * 0.78f),
                r * 0.78f,
            )
        }
        AnimatedEye(Modifier.size(size * 0.5f), lookY = if (active) null else 0f, glowColor = if (active) EsColors.Primary else Color.Transparent)
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, pulsing: Boolean = false) {
    val t = rememberInfiniteTransition(label = "pill")
    val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = if (pulsing) a else 1f)))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Numbered step badge that morphs into an animated check mark once done. */
@Composable
fun StepBadge(number: Int, done: Boolean, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (done) EsColors.Success else Color.White.copy(alpha = 0.08f), tween(350), label = "bg")
    Box(
        modifier.size(40.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(done, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Filled.Check, null, tint = Color(0xFF032016), modifier = Modifier.size(22.dp))
        }
        AnimatedVisibility(!done, enter = fadeIn(), exit = fadeOut()) {
            Text("$number", style = MaterialTheme.typography.titleMedium, color = EsColors.Text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EsColors.TextMuted,
        modifier = modifier.padding(start = 4.dp, bottom = 10.dp, top = 6.dp),
    )
}

@Composable
fun BulletRow(icon: ImageVector, title: String, body: String, tint: Color = EsColors.Primary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = EsColors.TextMuted)
        }
    }
}
