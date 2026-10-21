package com.novacleaner.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.novacleaner.app.ui.components.GradientButton
import com.novacleaner.app.ui.theme.Emerald400
import com.novacleaner.app.ui.theme.Mint300
import kotlin.math.roundToInt

/** Collects the on-screen position of elements the guided tour points at. */
class TourState {
    internal val targets = mutableStateMapOf<String, LayoutCoordinates>()

    fun bounds(key: String): Rect? = targets[key]?.takeIf { it.isAttached }?.boundsInRoot()
}

fun Modifier.tourTarget(state: TourState, key: String): Modifier = onGloballyPositioned { state.targets[key] = it }

data class TourStep(val key: String, val title: String, val message: String)

/**
 * Spotlight walkthrough: dims the screen, cuts an animated hole around the current element,
 * pulses a ring around it and shows a tooltip card with Skip / Next.
 */
@Composable
fun TourOverlay(state: TourState, steps: List<TourStep>, onFinish: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val density = LocalDensity.current
    val pad = with(density) { 10.dp.toPx() }
    val raw = state.bounds(step.key) ?: Rect(Offset(0f, 0f), Size(0f, 0f))
    val hole = Rect(raw.left - pad, raw.top - pad, raw.right + pad, raw.bottom + pad)
    val animatedHole by animateRectAsState(hole, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "hole")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(450)) }
    val pulse by rememberInfiniteTransition(label = "tour").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing)), label = "pulse",
    )
    var tooltipHeight by remember { mutableStateOf(0) }

    fun next() {
        if (index < steps.lastIndex) index++ else onFinish()
    }
    BackHandler(onBack = onFinish)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .alpha(appear.value)
            .pointerInput(Unit) { detectTapGestures { next() } },
    ) {
        val screenHeight = constraints.maxHeight
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color(0xE0030806))
            val radius = CornerRadius(26.dp.toPx())
            drawRoundRect(Color.Black, animatedHole.topLeft, animatedHole.size, radius, blendMode = BlendMode.Clear)
            val grow = pulse * 12.dp.toPx()
            drawRoundRect(
                color = Emerald400.copy(alpha = (1f - pulse) * 0.9f),
                topLeft = Offset(animatedHole.left - grow, animatedHole.top - grow),
                size = Size(animatedHole.width + grow * 2, animatedHole.height + grow * 2),
                cornerRadius = CornerRadius(radius.x + grow),
                style = Stroke(2.dp.toPx()),
            )
            drawRoundRect(Mint300.copy(alpha = 0.9f), animatedHole.topLeft, animatedHole.size, radius, style = Stroke(1.5.dp.toPx()))
        }

        val gap = with(density) { 18.dp.toPx() }
        val below = animatedHole.center.y < screenHeight * 0.55f
        val targetY = if (below) animatedHole.bottom + gap else animatedHole.top - gap - tooltipHeight
        val y by animateIntAsState(targetY.roundToInt().coerceIn(0, (screenHeight - tooltipHeight).coerceAtLeast(0)), label = "tipY")

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .offset { IntOffset(0, y) }
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .onSizeChanged { tooltipHeight = it.height },
        ) {
            AnimatedContent(
                index,
                transitionSpec = { (fadeIn(tween(250)) + slideInHorizontally { it / 6 }) togetherWith fadeOut(tween(150)) },
                label = "tip",
            ) { i ->
                val s = steps[i]
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "STEP ${i + 1} OF ${steps.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(s.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(onClick = onFinish) {
                            Text("Skip tour", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            steps.indices.forEach { d ->
                                val w by animateDpAsState(if (d == i) 18.dp else 6.dp, label = "d")
                                Box(
                                    Modifier.height(6.dp).width(w).clip(RoundedCornerShape(50))
                                        .background(if (d == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        GradientButton(
                            text = if (i == steps.lastIndex) "Got it" else "Next",
                            onClick = ::next,
                            height = 44.dp,
                        )
                    }
                }
            }
        }
    }
}
