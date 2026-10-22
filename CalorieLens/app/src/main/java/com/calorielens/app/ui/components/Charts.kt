package com.calorielens.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand

/** Large animated progress ring with a gradient sweep and glowing tip. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    stroke: Dp = 16.dp,
    colors: List<Color> = listOf(Brand.Mint, Brand.Cyan, Brand.Mint),
    track: Color = AppTheme.colors.stroke,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        anim.animateTo(progress.coerceIn(0f, 1f), tween(1300, easing = FastOutSlowInEasing))
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
            val sweep = 360f * anim.value
            if (sweep > 0.5f) {
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(colors),
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(strokePx, cap = StrokeCap.Round),
                    )
                }
                val angle = Math.toRadians((sweep - 90f).toDouble())
                val r = arcSize.width / 2
                val tip = Offset(center.x + (r * kotlin.math.cos(angle)).toFloat(), center.y + (r * kotlin.math.sin(angle)).toFloat())
                drawCircle(Brush.radialGradient(listOf(colors.last().copy(alpha = 0.55f), Color.Transparent), tip, strokePx * 1.6f), strokePx * 1.6f, tip)
                drawCircle(Color.White, strokePx * 0.22f, tip)
            }
        }
        content()
    }
}

data class DonutSlice(val value: Float, val color: Color)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    stroke: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(slices) { anim.snapTo(0f); anim.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val track = AppTheme.colors.stroke
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(strokePx))
                return@Canvas
            }
            val gap = 3f
            var start = -90f
            slices.forEach { slice ->
                val sweep = 360f * slice.value / total * anim.value
                if (sweep > gap) {
                    drawArc(slice.color, start + gap / 2, sweep - gap, false, Offset(inset, inset), arcSize, style = Stroke(strokePx, cap = StrokeCap.Butt))
                }
                start += sweep
            }
        }
        content()
    }
}

data class BarPoint(val label: String, val value: Float, val highlight: Boolean = false)

/** Rounded bar chart with an optional dashed goal line. */
@Composable
fun BarChart(
    points: List<BarPoint>,
    modifier: Modifier = Modifier,
    goal: Float? = null,
    barBrush: Brush = Brand.primaryGradient,
    highlightBrush: Brush = Brand.calorieGradient,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(points) { anim.snapTo(0f); anim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    val measurer = rememberTextMeasurer()
    val labelColor = AppTheme.colors.muted
    val track = AppTheme.colors.stroke
    Canvas(modifier) {
        val labelSpace = 22.dp.toPx()
        val chartHeight = size.height - labelSpace
        val maxValue = maxOf(points.maxOfOrNull { it.value } ?: 0f, goal ?: 0f, 1f) * 1.15f
        val slot = size.width / points.size.coerceAtLeast(1)
        val barWidth = (slot * 0.5f).coerceAtMost(28.dp.toPx())
        points.forEachIndexed { i, p ->
            val x = slot * i + (slot - barWidth) / 2
            drawRoundRect(track.copy(alpha = 0.5f), Offset(x, 0f), Size(barWidth, chartHeight), CornerRadius(barWidth / 2))
            val h = chartHeight * (p.value / maxValue) * anim.value
            if (h > 0f) {
                drawRoundRect(
                    brush = if (p.highlight) highlightBrush else barBrush,
                    topLeft = Offset(x, chartHeight - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
            }
            drawLabel(measurer, p.label, Offset(slot * i + slot / 2, chartHeight + 6.dp.toPx()), labelColor)
        }
        if (goal != null && goal > 0f) {
            val y = chartHeight - chartHeight * (goal / maxValue)
            drawLine(
                color = Brand.Amber.copy(alpha = 0.8f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }
    }
}

/** Smooth area line chart. */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Brand.Mint,
    goal: Float? = null,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(values) { anim.snapTo(0f); anim.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val grid = AppTheme.colors.stroke
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxValue = maxOf(values.max(), goal ?: 0f, 1f) * 1.15f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(grid.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1f)
        }
        val pts = values.mapIndexed { i, v -> Offset(stepX * i, size.height - size.height * (v / maxValue) * anim.value) }
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val cur = pts[i]
                val midX = (prev.x + cur.x) / 2
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, size.height)
            lineTo(pts.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        if (goal != null && goal > 0f) {
            val y = size.height - size.height * (goal / maxValue)
            drawLine(
                Brand.Amber.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }
        drawCircle(color, 5.dp.toPx(), pts.last())
        drawCircle(Color.White, 2.dp.toPx(), pts.last())
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    topCenter: Offset,
    color: Color,
) {
    val layout = measurer.measure(text, TextStyle(fontSize = 11.sp, color = color))
    drawText(layout, topLeft = Offset(topCenter.x - layout.size.width / 2f, topCenter.y))
}
