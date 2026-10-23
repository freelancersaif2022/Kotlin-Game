package com.voxcast.mic.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors

/** Two devices with data particles streaming between them, plus radar pulses. */
@Composable
fun ConnectingAnimation(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "connect")
    val flow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "flow")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_800, easing = LinearEasing)), label = "pulse")
    val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1_300), RepeatMode.Reverse), label = "bob")

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(VoxIcons.Microphone, VoxColors.PrimaryGradient, size = 72.dp, glow = VoxColors.Cyan, modifier = Modifier.padding(bottom = (4 + bob * 3).dp))
        Canvas(Modifier.weight(1f).height(72.dp)) {
            val y = size.height / 2
            drawLine(
                VoxColors.Outline, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f * density,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 6f * density), phase = -flow * 12f * density),
                cap = StrokeCap.Round,
            )
            val c = Offset(size.width / 2, y)
            for (i in 0 until 2) {
                val p = (pulse + i * 0.5f) % 1f
                drawCircle(VoxColors.Cyan.copy(alpha = (1 - p) * 0.5f), radius = 8f * density + p * size.height * 0.55f, center = c, style = Stroke(1.6f * density))
            }
            drawCircle(Brush.radialGradient(listOf(Color.White, VoxColors.Cyan, Color.Transparent), c, 9f * density), 9f * density, c)
            for (i in 0 until 4) {
                val p = (flow + i / 4f) % 1f
                val x = size.width * p
                val a = kotlin.math.sin(p * Math.PI).toFloat()
                drawCircle(
                    Brush.radialGradient(listOf(VoxColors.Cyan.copy(alpha = a), Color.Transparent), Offset(x, y), 7f * density),
                    7f * density, Offset(x, y),
                )
                drawCircle(Color.White.copy(alpha = a), 2.2f * density, Offset(x, y))
            }
        }
        IconBadge(
            VoxIcons.SpeakerBox,
            Brush.linearGradient(listOf(VoxColors.Violet, Color(0xFF4B3FD1))),
            size = 72.dp, glow = VoxColors.Violet,
            modifier = Modifier.padding(bottom = (4 - bob * 3).dp),
        )
    }
}

/** Vertical checklist: completed steps tick green, the current one spins. */
@Composable
fun ConnectionSteps(steps: List<String>, current: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        steps.forEachIndexed { index, label ->
            val stepNo = index + 1
            val done = stepNo < current
            val active = stepNo == current
            val color by animateColorAsState(
                when {
                    done -> VoxColors.Success
                    active -> VoxColors.Cyan
                    else -> VoxColors.TextFaint
                },
                tween(300), label = "step",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.14f))
                        .border(1.5.dp, color.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = when { done -> 2; active -> 1; else -> 0 },
                        transitionSpec = { (scaleIn(spring(dampingRatio = 0.45f), initialScale = 0.3f) + fadeIn()) togetherWith fadeOut() },
                        label = "icon",
                    ) { s ->
                        when (s) {
                            2 -> Icon(Icons.Rounded.Check, null, tint = VoxColors.Success, modifier = Modifier.size(16.dp))
                            1 -> CircularProgressIndicator(color = VoxColors.Cyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            else -> Text("$stepNo", style = MaterialTheme.typography.labelMedium, color = VoxColors.TextFaint)
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (done || active) VoxColors.Text else VoxColors.TextFaint,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
