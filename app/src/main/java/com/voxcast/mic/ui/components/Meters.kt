package com.voxcast.mic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxcast.mic.ui.theme.VoxColors

/** Segmented studio VU meter (green / amber / red). */
@Composable
fun VuMeter(level: () -> Float, modifier: Modifier = Modifier, segments: Int = 30, height: Dp = 10.dp) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val lv = level().coerceIn(0f, 1f)
        val gap = 2.5f * density
        val segW = (size.width - gap * (segments - 1)) / segments
        val lit = (lv * segments).toInt()
        for (i in 0 until segments) {
            val frac = i / segments.toFloat()
            val base = when {
                frac < 0.6f -> VoxColors.Success
                frac < 0.85f -> VoxColors.Warning
                else -> VoxColors.Error
            }
            drawRoundRect(
                if (i < lit) base else base.copy(alpha = 0.10f),
                topLeft = Offset(i * (segW + gap), 0f),
                size = Size(segW, size.height),
                cornerRadius = CornerRadius(size.height / 3),
            )
        }
    }
}

/** Scrolling mirrored waveform of the recent level history. */
@Composable
fun LiveWaveform(
    level: () -> Float,
    modifier: Modifier = Modifier,
    bars: Int = 46,
    brush: Brush = VoxColors.PrimaryGradient,
    height: Dp = 64.dp,
) {
    val history = remember { FloatArray(bars) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameMillis { now ->
                if (now - last >= 45) {
                    last = now
                    System.arraycopy(history, 1, history, 0, bars - 1)
                    history[bars - 1] = level().coerceIn(0f, 1f)
                    tick++
                }
            }
        }
    }
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (tick < 0) return@Canvas // read so each new sample triggers a redraw
        val gap = 3f * density
        val barW = (size.width - gap * (bars - 1)) / bars
        val mid = size.height / 2
        for (i in 0 until bars) {
            val v = history[i]
            val fade = 0.35f + 0.65f * (i / bars.toFloat())
            val hgt = (0.06f + v * 0.94f) * size.height
            drawRoundRect(
                brush,
                topLeft = Offset(i * (barW + gap), mid - hgt / 2),
                size = Size(barW, hgt),
                cornerRadius = CornerRadius(barW / 2),
                alpha = fade,
            )
        }
        drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, mid), Offset(size.width, mid), strokeWidth = 1f)
    }
}
