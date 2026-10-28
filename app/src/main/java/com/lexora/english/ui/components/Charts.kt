package com.lexora.english.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex

data class BarDatum(val label: String, val value: Float, val highlight: Boolean = false)

/** Rounded vertical bar chart with a dashed goal line. */
@Composable
fun BarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    goal: Float? = null,
    height: Dp = 150.dp,
    valueLabel: (Float) -> String = { it.toInt().toString() },
) {
    val c = Lex.colors
    val anim = remember { Animatable(0f) }
    LaunchedEffect(data) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val max = maxOf(data.maxOfOrNull { it.value } ?: 0f, goal ?: 0f, 1f) * 1.15f
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            data.forEach { d ->
                Text(
                    if (d.value > 0f) valueLabel(d.value) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (d.highlight) c.primary else c.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val slot = size.width / data.size
            val barW = slot * 0.46f
            val radius = CornerRadius(barW / 2, barW / 2)
            if (goal != null && goal > 0f) {
                val y = size.height - size.height * (goal / max)
                drawLine(
                    Brand.Amber, Offset(0f, y), Offset(size.width, y), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)), cap = StrokeCap.Round,
                )
            }
            data.forEachIndexed { i, d ->
                val x = slot * i + (slot - barW) / 2
                drawRoundRect(c.track, Offset(x, 0f), Size(barW, size.height), radius)
                val h = (size.height * (d.value / max) * anim.value).coerceAtLeast(if (d.value > 0f) barW else 0f)
                if (h > 0f) {
                    val brush = if (d.highlight) Brush.verticalGradient(listOf(Brand.Indigo, Brand.Blue))
                    else Brush.verticalGradient(listOf(Brand.Blue.copy(alpha = 0.75f), Brand.Blue.copy(alpha = 0.45f)))
                    drawRoundRect(brush, Offset(x, size.height - h), Size(barW, h), radius)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach { d ->
                Text(
                    d.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (d.highlight) c.text else c.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

data class DonutSlice(val label: String, val value: Float, val color: Color)

/** Donut chart made of rounded segments with a centre label. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerTitle: String,
    centerSubtitle: String,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    stroke: Dp = 18.dp,
) {
    val c = Lex.colors
    val anim = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val d = this.size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2)
            drawArc(c.track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
            var start = -90f
            slices.forEach { s ->
                val sweep = 360f * (s.value / total) * anim.value
                if (sweep > 0.5f) {
                    drawArc(s.color, start + 1f, (sweep - 2f).coerceAtLeast(0.5f), false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Butt))
                }
                start += 360f * (s.value / total) * anim.value
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTitle, style = MaterialTheme.typography.headlineSmall, color = c.text)
            Text(centerSubtitle, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    val c = Lex.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = c.text)
    }
}
