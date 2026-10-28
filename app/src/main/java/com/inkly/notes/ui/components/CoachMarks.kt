package com.inkly.notes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

data class CoachStep(val target: String, val icon: ImageVector, val title: String, val body: String)

/**
 * First-run guided tour: dims the screen, cuts an animated spotlight around each target
 * (bounds supplied in root coordinates) and shows a tooltip card next to it.
 */
@Composable
fun CoachMarkOverlay(
    steps: List<CoachStep>,
    targets: Map<String, Rect>,
    onFinish: () -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableFloatStateOf(0f) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val density = LocalDensity.current
    val pad = with(density) { 10.dp.toPx() }
    val target = targets[step.target] ?: Rect(Offset.Zero, Size.Zero)
    val hole by animateRectAsState(
        Rect(target.left - pad, target.top - pad, target.right + pad, target.bottom + pad),
        spring(dampingRatio = 0.8f, stiffness = 260f),
        label = "hole",
    )
    val appear by animateFloatAsState(shown, tween(380), label = "appear")
    LaunchedEffect(Unit) { shown = 1f }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "ring")

    fun next() {
        if (index < steps.lastIndex) index++ else onFinish()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenH = constraints.maxHeight.toFloat()
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; alpha = appear }
                .pointerInput(index) { detectTapGestures { next() } }
        ) {
            drawRect(Color(0xFF0B0D1A).copy(alpha = 0.78f))
            val radius = minOf(hole.height / 2f, 28.dp.toPx())
            drawRoundRect(
                Color.Transparent, hole.topLeft, hole.size, CornerRadius(radius),
                blendMode = BlendMode.Clear,
            )
            val grow = 14.dp.toPx() * ring
            drawRoundRect(
                Color.White.copy(alpha = 0.55f * (1f - ring)),
                Offset(hole.left - grow, hole.top - grow),
                Size(hole.width + grow * 2, hole.height + grow * 2),
                CornerRadius(radius + grow),
                style = Stroke(2.dp.toPx()),
            )
        }

        val below = hole.center.y < screenH / 2f
        val gap = with(density) { 16.dp.toPx() }
        val topDp = with(density) { (hole.bottom + gap).toDp() }
        val bottomDp = with(density) { (screenH - hole.top + gap).toDp() }
        val animTop by animateDpAsState(topDp, spring(dampingRatio = 0.85f, stiffness = 300f), label = "top")
        val animBottom by animateDpAsState(bottomDp, spring(dampingRatio = 0.85f, stiffness = 300f), label = "bottom")

        Surface(
            modifier = Modifier
                .align(if (below) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(
                    top = if (below) animTop else 0.dp,
                    bottom = if (below) 0.dp else animBottom,
                    start = 20.dp, end = 20.dp,
                )
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer { alpha = appear; translationY = (1f - appear) * 40f },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = {
                        (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(160)))
                    },
                    label = "step",
                ) { i ->
                    val s = steps[i.coerceIn(0, steps.lastIndex)]
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandGradient),
                            contentAlignment = Alignment.Center,
                        ) { Icon(s.icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(s.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                s.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.indices.forEach { i ->
                            val w by animateDpAsState(if (i == index) 20.dp else 6.dp, label = "dot")
                            Box(
                                Modifier
                                    .height(6.dp)
                                    .width(w)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (index < steps.lastIndex) {
                        TextButton(onClick = onFinish) {
                            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { next() }, shape = RoundedCornerShape(14.dp)) {
                        Text(if (index < steps.lastIndex) "Next" else "Got it")
                    }
                }
            }
        }
    }
}
