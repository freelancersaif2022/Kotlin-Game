package com.spectral.ghostdetector.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectral.ghostdetector.audio.Sfx
import com.spectral.ghostdetector.ui.rememberApp
import com.spectral.ghostdetector.ui.theme.GColors
import com.spectral.ghostdetector.ui.theme.GFonts
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Click that plays the UI click sound + light haptic. */
@Composable
fun Modifier.soundClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val app = rememberApp()
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null, enabled = enabled) {
        app.sounds.play(Sfx.CLICK, 0.7f)
        app.haptics.tap()
        onClick()
    }
}

/** Frosted dark glass panel with neon hairline border. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    borderColor: Color = GColors.Stroke,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(GColors.PanelHi.copy(alpha = 0.86f), GColors.Panel.copy(alpha = 0.78f))
                )
            )
            .border(1.dp, Brush.verticalGradient(listOf(borderColor, borderColor.copy(alpha = 0.08f))), shape)
            .padding(padding),
        content = content,
    )
}

/** Primary neon button with glow and press animation. */
@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = GColors.Neon,
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    val app = rememberApp()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .scale(scale)
            .drawBehind {
                if (filled) {
                    for (i in 1..4) {
                        drawRoundRect(
                            color = color.copy(alpha = 0.07f),
                            topLeft = Offset(-i * 3f, -i * 3f),
                            size = Size(size.width + i * 6f, size.height + i * 6f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx() + i * 3f),
                        )
                    }
                }
            }
            .clip(shape)
            .background(
                if (filled) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.78f)))
                else Brush.verticalGradient(listOf(color.copy(alpha = 0.10f), color.copy(alpha = 0.04f)))
            )
            .border(1.dp, color.copy(alpha = if (filled) 1f else 0.55f), shape)
            .clickable(interactionSource = interaction, indication = null) {
                app.sounds.play(Sfx.CLICK, 0.8f)
                app.haptics.tap()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (filled) GColors.Void else color
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** Round glass icon button used in HUDs / top bars. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color = GColors.Neon,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (active) tint.copy(alpha = 0.22f) else GColors.Panel.copy(alpha = 0.7f))
            .border(1.dp, if (active) tint else GColors.Stroke, CircleShape)
            .soundClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (active) tint else GColors.TextPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun BlinkingDot(color: Color = GColors.Neon, size: Dp = 8.dp, periodMs: Int = 900) {
    val t = rememberInfiniteTransition(label = "blink")
    val a by t.animateFloat(
        initialValue = 1f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(periodMs / 2), RepeatMode.Reverse), label = "a",
    )
    Box(
        Modifier
            .size(size)
            .drawBehind {
                drawCircle(color.copy(alpha = a * 0.35f), radius = this.size.minDimension)
                drawCircle(color.copy(alpha = a))
            }
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = GColors.TextSecondary) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
        color = color,
    )
}

@Composable
fun Badge(text: String, color: Color = GColors.Neon, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** 5 segment threat meter. */
@Composable
fun ThreatBar(level: Int, modifier: Modifier = Modifier, segmentWidth: Dp = 16.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..5) {
            val c = threatColor(i)
            Box(
                Modifier
                    .width(segmentWidth)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= level) c else GColors.TextMuted.copy(alpha = 0.25f))
            )
        }
    }
}

fun threatColor(level: Int): Color = when (level) {
    1, 2 -> GColors.Neon
    3 -> GColors.Amber
    else -> GColors.Danger
}

/**
 * Animated full-screen "haunted lab" background: dark radial base, drifting fog,
 * floating dust particles, faint grid and CRT scanlines.
 */
@Composable
fun GhostBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "bg")
    val time by t.animateFloat(
        0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "time",
    )
    val particles = remember {
        val r = Random(42)
        List(46) { floatArrayOf(r.nextFloat(), r.nextFloat(), r.nextFloat() * 0.5f + 0.2f, r.nextFloat() * 6.28f, r.nextFloat()) }
    }
    Box(modifier.fillMaxSize().background(GColors.Void)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF0C2419), Color(0xFF04100A), GColors.Void),
                    center = Offset(w * 0.5f, h * 0.38f), radius = h * 0.75f,
                )
            )
            // drifting fog banks
            for (i in 0 until 5) {
                val fx = w * (0.5f + 0.45f * sin(time * 0.05f + i * 1.7f))
                val fy = h * (0.25f + i * 0.16f + 0.04f * cos(time * 0.07f + i))
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x1A7DFFB5), Color.Transparent),
                        center = Offset(fx, fy), radius = w * 0.7f,
                    ),
                    radius = w * 0.7f, center = Offset(fx, fy),
                )
            }
            // grid
            val step = 42.dp.toPx()
            var gx = (time * 3f) % step
            while (gx < w) {
                drawLine(GColors.Neon.copy(alpha = 0.035f), Offset(gx, 0f), Offset(gx, h), 1f); gx += step
            }
            var gy = (time * 3f) % step
            while (gy < h) {
                drawLine(GColors.Neon.copy(alpha = 0.035f), Offset(0f, gy), Offset(w, gy), 1f); gy += step
            }
            // dust particles
            particles.forEach { p ->
                val px = (p[0] * w + sin(time * 0.3f + p[3]) * 30f) % w
                val py = ((p[1] * h - time * p[2] * 14f) % h + h) % h
                val a = 0.15f + 0.35f * (0.5f + 0.5f * sin(time * 1.3f + p[3]))
                drawCircle(GColors.NeonSoft.copy(alpha = a * p[4]), radius = 1.2f + p[4] * 2.2f, center = Offset(px, py))
            }
            drawVignette()
        }
        content()
        ScanLines(Modifier.fillMaxSize())
    }
}

fun DrawScope.drawVignette(strength: Float = 0.85f) {
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = strength),
            center = center, radius = size.maxDimension * 0.72f,
        )
    )
}

/** CRT style scanlines + a slow moving refresh bar. */
@Composable
fun ScanLines(modifier: Modifier = Modifier, alpha: Float = 0.05f) {
    val t = rememberInfiniteTransition(label = "scan")
    val bar by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "bar")
    Canvas(modifier) {
        val step = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(Color.Black.copy(alpha = alpha * 3f), Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        val by = bar * (size.height + 200f) - 100f
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, GColors.Neon.copy(alpha = alpha), Color.Transparent),
                startY = by - 90f, endY = by + 90f,
            ),
            topLeft = Offset(0f, by - 90f), size = Size(size.width, 180f),
        )
    }
}

/**
 * Animated radar scope: rings, rotating sweep with trailing glow and fading blips.
 * [blips] are polar points (angleRad, radius 0..1).
 */
@Composable
fun RadarScope(
    modifier: Modifier = Modifier,
    speedMs: Int = 3200,
    color: Color = GColors.Neon,
    blips: List<Pair<Float, Float>> = emptyList(),
    showRandomBlips: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "radar")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(speedMs, easing = LinearEasing)), label = "ang")
    val randomBlips = remember {
        val r = Random(7)
        List(5) { Pair(r.nextFloat() * 2f * PI.toFloat(), 0.25f + r.nextFloat() * 0.65f) }
    }
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val c = center
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.10f), Color(0xF0020604)), center = c, radius = radius),
            radius = radius,
        )
        for (i in 1..4) drawCircle(color.copy(alpha = 0.22f), radius = radius * i / 4f, style = Stroke(1.dp.toPx()))
        drawLine(color.copy(alpha = 0.18f), Offset(c.x - radius, c.y), Offset(c.x + radius, c.y), 1.dp.toPx())
        drawLine(color.copy(alpha = 0.18f), Offset(c.x, c.y - radius), Offset(c.x, c.y + radius), 1.dp.toPx())
        // tick marks
        for (i in 0 until 72) {
            val a = i * 5f * PI.toFloat() / 180f
            val inner = if (i % 6 == 0) radius * 0.9f else radius * 0.95f
            drawLine(
                color.copy(alpha = 0.35f),
                Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                Offset(c.x + cos(a) * radius, c.y + sin(a) * radius),
                1.dp.toPx(),
            )
        }
        rotate(angle, c) {
            drawCircle(
                Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.80f to Color.Transparent,
                    0.999f to color.copy(alpha = 0.55f),
                    1f to Color.Transparent,
                    center = c,
                ),
                radius = radius,
            )
            drawLine(color, c, Offset(c.x + radius, c.y), 2.dp.toPx(), cap = StrokeCap.Round)
        }
        val sweepRad = angle * PI.toFloat() / 180f
        val all = if (showRandomBlips) randomBlips + blips else blips
        all.forEach { (a, r) ->
            var diff = (sweepRad - a) % (2f * PI.toFloat())
            if (diff < 0) diff += 2f * PI.toFloat()
            val fade = (1f - diff / (2f * PI.toFloat())).coerceIn(0f, 1f)
            val p = Offset(c.x + cos(a) * r * radius, c.y + sin(a) * r * radius)
            drawCircle(color.copy(alpha = fade * 0.35f), radius = 7.dp.toPx() * fade, center = p)
            drawCircle(color.copy(alpha = 0.2f + fade * 0.8f), radius = 2.5.dp.toPx(), center = p)
        }
        drawCircle(color, radius = 3.dp.toPx(), center = c)
    }
}

/** Expanding pulse rings (sonar). */
@Composable
fun PulseRings(modifier: Modifier = Modifier, color: Color = GColors.Neon, rings: Int = 3, periodMs: Int = 2400) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing)), label = "p")
    Canvas(modifier) {
        val maxR = size.minDimension / 2f
        for (i in 0 until rings) {
            val f = (p + i.toFloat() / rings) % 1f
            drawCircle(color.copy(alpha = (1f - f) * 0.45f), radius = maxR * (0.55f + 0.45f * f), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
fun StatTile(label: String, value: String, unit: String, modifier: Modifier = Modifier, color: Color = GColors.Neon) {
    GlassPanel(modifier, corner = 14.dp, padding = 10.dp) {
        SectionLabel(label)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = color, fontFamily = GFonts.Mono, fontSize = 20.sp)
            Spacer(Modifier.width(3.dp))
            Text(unit, color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
fun DisclaimerText(modifier: Modifier = Modifier) {
    Text(
        "PRANK APP · FOR ENTERTAINMENT ONLY · ALL READINGS ARE SIMULATED",
        modifier = modifier,
        color = GColors.TextMuted,
        fontFamily = GFonts.Mono,
        fontSize = 9.sp,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Normal,
    )
}
