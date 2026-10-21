package com.netscope.wifianalyzer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlin.math.roundToInt

class TourState {
    val targets = mutableStateMapOf<String, Rect>()
}

val LocalTour = staticCompositionLocalOf { TourState() }

fun Modifier.tourTarget(state: TourState, key: String): Modifier =
    onGloballyPositioned { state.targets[key] = it.boundsInRoot() }

data class TourStep(val key: String, val title: String, val text: String)

/** Spotlight walkthrough that highlights real UI elements one by one. */
@Composable
fun TourOverlay(state: TourState, steps: List<TourStep>, onFinish: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val target = state.targets[step.key] ?: return
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val rect by animateRectAsState(
        Rect(target.left - pad, target.top - pad, target.right + pad, target.bottom + pad),
        spring(dampingRatio = 0.8f, stiffness = 220f),
        label = "spot",
    )
    val pulse = rememberInfiniteTransition(label = "spotPulse")
    val ring by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "ring")

    fun next() {
        if (index >= steps.lastIndex) onFinish() else index++
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(index) { detectTapGestures { next() } },
    ) {
        val heightPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color(0xFF02040C).copy(alpha = 0.78f))
            val r = CornerRadius(24.dp.toPx())
            drawRoundRect(Color.Transparent, rect.topLeft, rect.size, r, blendMode = BlendMode.Clear)
            val grow = 10.dp.toPx() * ring
            drawRoundRect(
                Palette.Cyan.copy(alpha = 1f - ring),
                Offset(rect.left - grow, rect.top - grow),
                androidx.compose.ui.geometry.Size(rect.width + grow * 2, rect.height + grow * 2),
                CornerRadius(r.x + grow),
                style = Stroke(2.dp.toPx()),
            )
            drawRoundRect(Palette.Cyan, rect.topLeft, rect.size, r, style = Stroke(2.dp.toPx()))
        }
        val below = rect.center.y < heightPx * 0.55f
        val cardY = if (below) rect.bottom + with(density) { 16.dp.toPx() } else rect.top - with(density) { 196.dp.toPx() }
        AnimatedContent(
            targetState = index,
            transitionSpec = { (fadeIn(tween(250)) + slideInVertically { it / 6 }) togetherWith fadeOut(tween(150)) },
            label = "tourCard",
            modifier = Modifier
                .offset { IntOffset(0, cardY.roundToInt().coerceAtLeast(0)) }
                .padding(horizontal = 20.dp),
        ) { i ->
            val s = steps[i]
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141C36))
                    .padding(20.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.indices.forEach { d ->
                        Box(
                            Modifier
                                .height(6.dp)
                                .width(if (d == i) 22.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (d == i) Palette.Cyan else Color.White.copy(alpha = 0.25f)),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(s.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(s.text, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Skip tour",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onFinish() }.padding(8.dp),
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.Cyan)
                            .clickable { next() }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        Text(
                            if (i == steps.lastIndex) "Got it" else "Next",
                            color = Color(0xFF060A17),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(0.dp))
    }
}
