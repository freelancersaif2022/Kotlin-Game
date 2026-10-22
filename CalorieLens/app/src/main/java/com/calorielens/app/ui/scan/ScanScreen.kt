package com.calorielens.app.ui.scan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.ui.components.IconCircleButton
import com.calorielens.app.ui.components.MealPhoto
import com.calorielens.app.ui.components.MessageDialog
import com.calorielens.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlin.math.sin

private val steps = listOf(
    "Detecting food in photo",
    "Identifying the dish",
    "Estimating the portion",
    "Calculating nutrition",
)

@Composable
fun ScanScreen(
    imagePath: String,
    onDone: (Long) -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    vm: ScanViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(imagePath) { vm.start(imagePath) }

    val progress = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progress.animateTo(0.92f, tween(2500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(state) {
        val s = state
        if (s is ScanState.Done && !finished) {
            progress.animateTo(1f, tween(350))
            finished = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(900)
            onDone(s.mealId)
        }
    }
    BackHandler(enabled = state is ScanState.Analyzing) { /* analysis can't be interrupted mid-save */ }

    val photoScale = remember { Animatable(1.12f) }
    LaunchedEffect(Unit) { photoScale.animateTo(1f, tween(3000, easing = FastOutSlowInEasing)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        MealPhoto(
            imagePath,
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = photoScale.value; scaleY = photoScale.value },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (finished) 0.25f else 0.4f)),
        )
        ScanOverlay(active = !finished, modifier = Modifier.fillMaxSize())

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state !is ScanState.Analyzing) {
                IconCircleButton(Icons.Rounded.Close, "Close", onClose, background = Color.Black.copy(alpha = 0.45f), tint = Color.White, bordered = false)
            }
        }

        AnalysisPanel(
            progress = progress.value,
            finished = finished,
            doneState = state as? ScanState.Done,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }

    when (val s = state) {
        ScanState.NoFood -> MessageDialog(
            icon = Icons.Rounded.NoFood,
            iconColor = Brand.Amber,
            title = "No food detected",
            message = "We couldn't recognise a dish in this photo. Fit the whole meal in the frame with good lighting and try again.",
            confirmText = "Retake photo",
            onConfirm = onRetake,
            dismissText = "Back to home",
            onDismiss = onClose,
        )
        is ScanState.Error -> MessageDialog(
            icon = Icons.Rounded.ErrorOutline,
            iconColor = Brand.Danger,
            title = "Scan failed",
            message = s.message,
            confirmText = "Try again",
            onConfirm = onRetake,
            dismissText = "Back to home",
            onDismiss = onClose,
        )
        else -> Unit
    }
}

@Composable
private fun ScanOverlay(active: Boolean, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "scanOverlay")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "sweep")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "pulse")
    val bracketIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) { bracketIn.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)) }
    val bracketColor by animateColorAsState(if (active) Brand.Mint else Brand.Amber, tween(400), label = "bracketColor")

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Mesh of nodes that ripple outwards from the centre
        val cols = 9
        val rows = 15
        val stepX = w / cols
        val stepY = h / rows
        if (active) {
            for (i in 0..cols) for (j in 0..rows) {
                val p = Offset(stepX * i, stepY * j)
                val d = (p - center).getDistance() / (h * 0.6f)
                val wave = ((sin((d * 8f - pulse * 6.283f).toDouble()) + 1) / 2).toFloat()
                drawCircle(Brand.Mint.copy(alpha = 0.05f + 0.25f * wave * (1f - d).coerceIn(0f, 1f)), 1.8.dp.toPx(), p)
            }
            // Laser sweep
            val y = h * (0.1f + 0.62f * sweep)
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Brand.Mint.copy(alpha = 0.25f), Color.Transparent), y - h * 0.08f, y + h * 0.08f),
                topLeft = Offset(0f, y - h * 0.08f),
                size = Size(w, h * 0.16f),
            )
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, Brand.Mint, Brand.Cyan, Brand.Mint, Color.Transparent)),
                Offset(0f, y), Offset(w, y), 2.5.dp.toPx(), StrokeCap.Round,
            )
        }
        // Detection brackets closing in on the dish
        val target = Size(w * 0.62f, w * 0.62f)
        val k = bracketIn.value
        val bw = w * (1f - k) + target.width * k
        val bh = h * 0.6f * (1f - k) + target.height * k
        val left = center.x - bw / 2
        val top = h * 0.36f - bh / 2
        val corner = 34.dp.toPx()
        val stroke = 4.dp.toPx()
        val c = bracketColor
        listOf(
            Triple(Offset(left, top + corner), Offset(left, top), Offset(left + corner, top)),
            Triple(Offset(left + bw - corner, top), Offset(left + bw, top), Offset(left + bw, top + corner)),
            Triple(Offset(left, top + bh - corner), Offset(left, top + bh), Offset(left + corner, top + bh)),
            Triple(Offset(left + bw - corner, top + bh), Offset(left + bw, top + bh), Offset(left + bw, top + bh - corner)),
        ).forEach { (a, m, b) ->
            drawLine(c, a, m, stroke, StrokeCap.Round)
            drawLine(c, m, b, stroke, StrokeCap.Round)
        }
        if (active) {
            drawCircle(c.copy(alpha = 0.6f * (1f - pulse)), radius = 12.dp.toPx() + 40.dp.toPx() * pulse, center = Offset(center.x, top + bh / 2), style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun AnalysisPanel(progress: Float, finished: Boolean, doneState: ScanState.Done?, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xE6101318))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val s = 5.dp.toPx()
                    drawArc(Color.White.copy(alpha = 0.1f), 0f, 360f, false, Offset(s / 2, s / 2), Size(size.width - s, size.height - s), style = Stroke(s))
                    drawArc(
                        Brush.sweepGradient(listOf(Brand.Mint, Brand.Cyan, Brand.Mint)),
                        -90f, 360f * progress, false, Offset(s / 2, s / 2), Size(size.width - s, size.height - s),
                        style = Stroke(s, cap = StrokeCap.Round),
                    )
                }
                if (finished) {
                    CheckBadge()
                } else {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (finished) "Analysis complete" else "Analyzing your meal…",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    if (finished && doneState != null) "${doneState.name} · ${doneState.calories} kcal" else "On-device AI · nothing leaves your phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (finished) Brand.Mint else Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        steps.forEachIndexed { i, label ->
            val threshold = (i + 1) / steps.size.toFloat() * 0.92f
            val done = progress >= threshold || finished
            val activeStep = !done && progress >= i / steps.size.toFloat() * 0.92f
            StepRow(label, done, activeStep, i)
        }
    }
}

@Composable
private fun StepRow(label: String, done: Boolean, active: Boolean, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120L * index); visible = true }
    AnimatedVisibility(visible, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color by animateColorAsState(
                when {
                    done -> Brand.Mint
                    active -> Color.White
                    else -> Color.White.copy(alpha = 0.3f)
                },
                tween(300),
                label = "stepColor",
            )
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (done) Brand.Mint.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(Icons.Rounded.Check, null, tint = Brand.Mint, modifier = Modifier.size(14.dp))
                } else if (active) {
                    PulsingDot()
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun PulsingDot() {
    val t = rememberInfiniteTransition(label = "dot")
    val s by t.animateFloat(0.6f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "dotScale")
    Box(
        Modifier
            .size(8.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .clip(CircleShape)
            .background(Color.White),
    )
}

@Composable
private fun CheckBadge() {
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = appear, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brand.primaryGradient),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Check, null, tint = Color(0xFF04140F)) }
    }
}
