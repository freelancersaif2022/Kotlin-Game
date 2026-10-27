package com.pdfstudio.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfstudio.app.Busy
import com.pdfstudio.app.BusyPhase
import com.pdfstudio.app.ui.components.AppLogo
import com.pdfstudio.app.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin

private val SuccessColors = listOf(Color(0xFF22C58B), Color(0xFF0FA37F))
private val ErrorColors = listOf(Color(0xFFFF6B6B), Color(0xFFE03E3E))

/** Modal progress card: animated gradient ring, live percentage, then a drawn check mark. */
@Composable
fun ProgressDialog(busy: Busy?) {
    AnimatedVisibility(visible = busy != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(260))) {
        // Keep the last state on screen while fading out.
        var last by remember { mutableStateOf(busy) }
        if (busy != null) last = busy
        val shown = last ?: return@AnimatedVisibility

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .animateEnterExit(
                        enter = scaleIn(spring(dampingRatio = 0.62f, stiffness = 420f), initialScale = 0.8f) + fadeIn(),
                        exit = scaleOut(tween(220), targetScale = 0.92f) + fadeOut(tween(220)),
                    )
                    .width(272.dp)
                    .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProgressRing(shown)
                Spacer(Modifier.height(22.dp))
                AnimatedContent(
                    targetState = shown.title,
                    transitionSpec = { (fadeIn(tween(220)) + slideInVertically { it / 3 }).togetherWith(fadeOut(tween(150))) },
                    label = "title",
                ) { title ->
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                val detail = shown.detail
                if (detail != null) {
                    Spacer(Modifier.height(6.dp))
                    AnimatedContent(
                        targetState = detail,
                        transitionSpec = {
                            (slideInVertically(tween(220)) { it } + fadeIn(tween(220)))
                                .togetherWith(slideOutVertically(tween(180)) { -it } + fadeOut(tween(180)))
                        },
                        label = "detail",
                    ) { text ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(busy: Busy) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress = busy.progress
    val determinate = progress != null || busy.phase != BusyPhase.Working
    val target = when (busy.phase) {
        BusyPhase.Working -> progress ?: 0f
        else -> 1f
    }
    val animatedProgress by animateFloatAsState(target, tween(420, easing = FastOutSlowInEasing), label = "ring")

    val infinite = rememberInfiniteTransition(label = "spin")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin")
    val breathe by infinite.animateFloat(
        0.94f, 1.04f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "breathe",
    )

    val colors = when (busy.phase) {
        BusyPhase.Working -> listOf(Brand.Coral, Brand.Magenta)
        BusyPhase.Done -> SuccessColors
        BusyPhase.Failed -> ErrorColors
    }
    val start by animateColorAsState(colors[0], tween(400), label = "c0")
    val end by animateColorAsState(colors[1], tween(400), label = "c1")

    Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        // Soft glow behind the ring.
        Box(
            Modifier
                .size(112.dp)
                .graphicsLayer {
                    val s = if (busy.phase == BusyPhase.Working) breathe else 1f
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(50))
                .background(Brush.radialGradient(listOf(start.copy(alpha = 0.18f), Color.Transparent)))
        )
        Canvas(Modifier.size(116.dp).rotate(if (determinate) -90f else spin - 90f)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val sweep = if (determinate) 360f * animatedProgress else 100f
            if (sweep > 0.5f) {
                val brush = Brush.sweepGradient(listOf(start, end, start), center)
                drawArc(brush, 0f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                // Bright head at the leading edge of the arc.
                val angle = Math.toRadians(sweep.toDouble())
                val r = (size.width - stroke) / 2
                val head = Offset(center.x + (r * cos(angle)).toFloat(), center.y + (r * sin(angle)).toFloat())
                drawCircle(Color.White, radius = stroke * 0.28f, center = head)
            }
        }
        AnimatedContent(
            targetState = busy.phase,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.5f, stiffness = 380f), initialScale = 0.4f) + fadeIn())
                    .togetherWith(scaleOut(targetScale = 0.6f) + fadeOut(tween(120)))
            },
            label = "center",
        ) { phase ->
            when (phase) {
                BusyPhase.Working -> if (progress != null) {
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    AppLogo(size = 52.dp, elevation = 6.dp)
                }
                BusyPhase.Done -> DrawnMark(SuccessColors, check = true)
                BusyPhase.Failed -> DrawnMark(ErrorColors, check = false)
            }
        }
    }
}

/** A filled circle whose check (or cross) is drawn stroke by stroke. */
@Composable
private fun DrawnMark(colors: List<Color>, check: Boolean) {
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(420, delayMillis = 120, easing = FastOutSlowInEasing)) }
    Box(
        Modifier
            .size(64.dp)
            .shadow(12.dp, RoundedCornerShape(50), ambientColor = colors[1], spotColor = colors[1])
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val path = Path().apply {
                if (check) {
                    moveTo(size.width * 0.12f, size.height * 0.54f)
                    lineTo(size.width * 0.40f, size.height * 0.80f)
                    lineTo(size.width * 0.90f, size.height * 0.24f)
                } else {
                    moveTo(size.width * 0.2f, size.height * 0.2f)
                    lineTo(size.width * 0.8f, size.height * 0.8f)
                    moveTo(size.width * 0.8f, size.height * 0.2f)
                    lineTo(size.width * 0.2f, size.height * 0.8f)
                }
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val segment = Path()
            if (check) {
                measure.getSegment(0f, measure.length * draw.value, segment, true)
            } else {
                segment.addPath(path)
            }
            drawPath(
                segment,
                Color.White,
                alpha = if (check) 1f else draw.value,
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
