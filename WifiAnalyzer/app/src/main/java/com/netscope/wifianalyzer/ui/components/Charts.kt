package com.netscope.wifianalyzer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netscope.wifianalyzer.ui.theme.AppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

val GaugeTicks = listOf(0f, 5f, 10f, 25f, 50f, 100f, 250f, 500f, 1000f)

/** Maps a speed in Mbps onto the non-linear gauge scale (0..1). */
fun gaugeFraction(mbps: Float): Float {
    if (mbps <= 0f) return 0f
    val last = GaugeTicks.last()
    if (mbps >= last) return 1f
    val idx = GaugeTicks.indexOfFirst { it > mbps }
    val lo = GaugeTicks[idx - 1]
    val hi = GaugeTicks[idx]
    val seg = 1f / (GaugeTicks.size - 1)
    return seg * (idx - 1) + seg * ((mbps - lo) / (hi - lo))
}

@Composable
fun SpeedGauge(
    mbps: Float,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    centerContent: @Composable BoxScope.() -> Unit,
) {
    val fraction by animateFloatAsState(gaugeFraction(mbps), spring(dampingRatio = 0.75f, stiffness = 60f), label = "gauge")
    val measurer = rememberTextMeasurer()
    val track = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = AppColors.ext.muted
    val needleColor = MaterialTheme.colorScheme.onSurface
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.065f
            val radius = size.minDimension / 2 - stroke
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            val startAngle = 135f
            val sweep = 270f
            drawArc(track, startAngle, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // Glow underlay
            drawArc(
                brush = Brush.sweepGradient(colors.map { it.copy(alpha = 0.25f) } + colors.first().copy(alpha = 0.25f), center),
                startAngle = startAngle,
                sweepAngle = sweep * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke * 2.1f, cap = StrokeCap.Round),
            )
            rotate(degrees = 90f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.125f to colors.first(),
                        0.5f to colors.getOrElse(1) { colors.first() },
                        0.875f to colors.last(),
                        center = center,
                    ),
                    startAngle = startAngle - 90f,
                    sweepAngle = sweep * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            // Tick labels
            val labelRadius = radius - stroke * 1.9f
            GaugeTicks.forEachIndexed { i, v ->
                val a = (startAngle + sweep * i / (GaugeTicks.size - 1)) * PI / 180.0
                val text = if (v >= 1000f) "1G" else v.toInt().toString()
                val layout = measurer.measure(text, TextStyle(fontSize = 11.sp, color = labelColor, fontWeight = FontWeight.Medium))
                val p = Offset(center.x + (labelRadius * cos(a)).toFloat(), center.y + (labelRadius * sin(a)).toFloat())
                drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
            }
            // Needle
            val angle = (startAngle + sweep * fraction) * PI / 180.0
            val needleLen = radius - stroke * 3.2f
            val tip = Offset(center.x + (needleLen * cos(angle)).toFloat(), center.y + (needleLen * sin(angle)).toFloat())
            val inner = Offset(center.x + (needleLen * 0.55f * cos(angle)).toFloat(), center.y + (needleLen * 0.55f * sin(angle)).toFloat())
            drawLine(
                brush = Brush.linearGradient(listOf(needleColor.copy(alpha = 0f), needleColor), inner, tip),
                start = inner,
                end = tip,
                strokeWidth = stroke * 0.32f,
                cap = StrokeCap.Round,
            )
        }
        centerContent()
    }
}

@Composable
fun LineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    secondValues: List<Float>? = null,
    secondColor: Color = Color.Unspecified,
    maxValue: Float? = null,
    minValue: Float = 0f,
    gridColor: Color = AppColors.ext.cardBorder,
) {
    Canvas(modifier) {
        val all = values + (secondValues ?: emptyList())
        val maxV = maxValue ?: max(1f, (all.maxOrNull() ?: 1f) * 1.15f)
        val range = (maxV - minValue).coerceAtLeast(0.0001f)
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        fun drawSeries(series: List<Float>, c: Color) {
            if (series.size < 2) return
            val stepX = size.width / (series.size - 1)
            val pts = series.mapIndexed { i, v ->
                Offset(i * stepX, size.height - ((v - minValue) / range).coerceIn(0f, 1f) * size.height)
            }
            val path = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (i in 1 until pts.size) {
                    val p0 = pts[i - 1]
                    val p1 = pts[i]
                    val mx = (p0.x + p1.x) / 2
                    cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
                }
            }
            val fill = Path().apply {
                addPath(path)
                lineTo(pts.last().x, size.height)
                lineTo(pts.first().x, size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(c.copy(alpha = 0.35f), c.copy(alpha = 0f))))
            drawPath(path, c, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(c, 4.dp.toPx(), pts.last())
            drawCircle(Color.White, 2.dp.toPx(), pts.last())
        }
        drawSeries(values, color)
        secondValues?.let { drawSeries(it, secondColor) }
    }
}

@Composable
fun BarChart(values: List<Float>, color: Color, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = max(1f, values.max())
        val gap = size.width * 0.04f
        val w = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val x = i * (w + gap)
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 3))
            val h = size.height * (v / maxV)
            val c = if (highlightLast && i == values.lastIndex) color else color.copy(alpha = 0.6f)
            drawRoundRect(
                Brush.verticalGradient(listOf(c, c.copy(alpha = 0.55f)), startY = size.height - h, endY = size.height),
                Offset(x, size.height - h),
                Size(w, h),
                CornerRadius(w / 3),
            )
        }
    }
}
