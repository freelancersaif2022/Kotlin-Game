package com.novacleaner.app.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novacleaner.app.ads.BannerAd
import com.novacleaner.app.ads.NativeAdCard
import com.novacleaner.app.core.AppGraph

import com.novacleaner.app.core.PaywallManager
import com.novacleaner.app.ui.components.BytesHeadline
import com.novacleaner.app.ui.components.CircleIconButton
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.GradientButton
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ResultScreen(tool: Tool, bytes: Long, count: Int) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val counter = remember { Animatable(0f) }
    var showRest by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        counter.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        showRest = true
    }

    fun done() {
        scope.launch {
            val showPaywall = AppGraph.paywall.shouldShow(PaywallManager.Trigger.AFTER_RESULT)
            navigator.goHome()
            if (showPaywall) {
                AppGraph.paywall.markShown()
                navigator.openPremium()
            }
        }
    }
    BackHandler(onBack = ::done)

    val noun = when (tool) {
        Tool.APPS -> if (count == 1) "app uninstalled" else "apps uninstalled"
        Tool.DUPLICATES, Tool.SCREENSHOTS -> if (count == 1) "photo removed" else "photos removed"
        else -> if (count == 1) "item removed" else "items removed"
    }
    val others = Tool.entries.filter { it != tool && it != Tool.BATTERY && it != Tool.SPEED }.shuffled(Random(tool.ordinal + count)).take(3)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(420.dp)) {
                drawRect(Brush.verticalGradient(listOf(tool.colors.first().copy(alpha = 0.30f), Color.Transparent)))
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    CircleIconButton(Icons.Rounded.Close, "Close", ::done)
                }
                SuccessBurst(tool.colors, Modifier.size(200.dp))
                Spacer(Modifier.height(8.dp))
                Text(if (bytes > 0) "Freed up" else "All done!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (bytes > 0) BytesHeadline((bytes * counter.value).toLong(), numberSize = 50)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$count $noun",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                AnimatedVisibility(showRest, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        NativeAdCard(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(18.dp))
                        Text("Keep your phone tidy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                        others.forEach { next ->
                            NovaCard(
                                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                onClick = {
                                    navigator.goHome()
                                    navigator.openTool(next)
                                },
                                padding = PaddingValues(14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GlossyIcon(next.icon, next.colors, size = 44.dp)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(next.title, style = MaterialTheme.typography.titleSmall)
                                        Text(next.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        GradientButton("Done", ::done, Modifier.fillMaxWidth(), colors = tool.colors)
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
        BannerAd()
        Spacer(Modifier.navigationBarsPadding())
    }
}

private class Confetti(val angle: Float, val distance: Float, val size: Float, val color: Int)

/** Pop-in circle, hand-drawn check stroke and a confetti burst. */
@Composable
private fun SuccessBurst(colors: List<Color>, modifier: Modifier) {
    val circle = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val confetti = remember {
        List(26) { Confetti(Random.nextFloat() * 360f, 0.55f + Random.nextFloat() * 0.45f, 3f + Random.nextFloat() * 5f, Random.nextInt(4)) }
    }
    val palette = listOf(colors.first(), colors.last(), Color(0xFFFFC94D), Color(0xFFFF8FB1))
    LaunchedEffect(Unit) {
        circle.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(250)
        launch { burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        check.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2
        confetti.forEach { p ->
            val a = (p.angle * PI / 180).toFloat()
            val d = r * p.distance * burst.value
            drawCircle(
                palette[p.color].copy(alpha = (1f - burst.value).coerceIn(0f, 1f)),
                p.size.dp.toPx() * (1f - burst.value * 0.4f),
                Offset(c.x + cos(a) * d, c.y + sin(a) * d),
            )
        }
        drawCircle(colors.first().copy(alpha = 0.18f), r * 0.78f * circle.value, c)
        drawCircle(Brush.linearGradient(colors, Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)), r * 0.58f * circle.value, c)
        drawCircle(Color.White.copy(alpha = 0.18f), r * 0.58f * circle.value, c, style = Stroke(2.dp.toPx()))
        val path = Path().apply {
            moveTo(c.x - r * 0.24f, c.y + r * 0.0f)
            lineTo(c.x - r * 0.06f, c.y + r * 0.18f)
            lineTo(c.x + r * 0.26f, c.y - r * 0.16f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * check.value, partial, true)
        drawPath(partial, Color.White, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

