package com.eyescroll.app.ui.screens

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eyescroll.app.Readiness
import com.eyescroll.app.data.Calibration
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.data.TrackingMode
import com.eyescroll.app.service.EyeTrackingService
import com.eyescroll.app.tracking.CalibrationSample
import com.eyescroll.app.tracking.FaceFrame
import com.eyescroll.app.tracking.FaceTracker
import com.eyescroll.app.tracking.GazeController
import com.eyescroll.app.tracking.GazeModel
import com.eyescroll.app.tracking.GazeSnapshot
import com.eyescroll.app.tracking.GazeZone
import com.eyescroll.app.ui.components.GazeMeter
import com.eyescroll.app.ui.components.GhostButton
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.GradientButton
import com.eyescroll.app.ui.theme.EsColors
import kotlinx.coroutines.delay

private enum class CalPhase { Intro, Countdown, Running, Result, Test }

private class CalResult(val model: GazeModel?, val ok: Boolean, val title: String, val message: String)

private val TARGETS = floatArrayOf(GazeModel.TOP, GazeModel.MIDDLE, GazeModel.BOTTOM)
private val TARGET_NAMES = listOf("top", "middle", "bottom")
private const val MOVE_MS = 650L
private const val HOLD_MS = 2700

@Composable
fun CalibrationScreen(onFinished: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { SettingsStore.get(context) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    var phase by remember { mutableStateOf(CalPhase.Intro) }
    var attempt by remember { mutableIntStateOf(0) }
    var frame by remember { mutableStateOf<FaceFrame?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableIntStateOf(-1) }
    var collecting by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    var result by remember { mutableStateOf<CalResult?>(null) }
    var testController by remember { mutableStateOf<GazeController?>(null) }
    var testSnapshot by remember { mutableStateOf(GazeSnapshot()) }
    var testHits by remember { mutableIntStateOf(0) }
    val samples = remember { ArrayList<CalibrationSample>() }
    val stepProgress = remember { Animatable(0f) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Runs on the main thread for every analysed frame.
    val handleFrame: (FaceFrame) -> Unit = { f ->
        frame = f
        val feats = f.features
        if (collecting && target >= 0 && feats != null && f.blink < 0.4f) {
            samples += CalibrationSample(TARGETS[target], feats.copyOf())
        }
        testController?.let { c ->
            val (snap, fired) = c.process(f, SystemClock.uptimeMillis())
            testSnapshot = snap
            if (fired != null) testHits++
        }
    }

    DisposableEffect(Unit) {
        if (!Readiness.hasCamera(context)) {
            error = "Camera permission is needed for calibration."
            onDispose { }
        } else {
            EyeTrackingService.stop(context)
            val tracker = FaceTracker(context, lifecycleOwner, 30, object : FaceTracker.Listener {
                override fun onFrame(frame: FaceFrame) {
                    handler.post { handleFrame(frame) }
                }

                override fun onError(message: String) {
                    handler.post { error = message }
                }
            })
            // Give a running tracking service a moment to release the camera.
            handler.postDelayed({ tracker.start(previewView.surfaceProvider) }, 300)
            onDispose {
                handler.removeCallbacksAndMessages(null)
                tracker.close()
            }
        }
    }

    LaunchedEffect(phase, attempt) {
        when (phase) {
            CalPhase.Countdown -> {
                for (i in 3 downTo 1) {
                    countdown = i
                    delay(800)
                }
                phase = CalPhase.Running
            }
            CalPhase.Running -> {
                samples.clear()
                for (i in 0..2) {
                    target = i
                    collecting = false
                    stepProgress.snapTo(0f)
                    delay(MOVE_MS)
                    collecting = true
                    stepProgress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                    collecting = false
                }
                target = -1
                result = evaluate(store, Calibration(samples.toList(), System.currentTimeMillis()))
                phase = CalPhase.Result
            }
            CalPhase.Test -> {
                val model = result?.model
                val s = store.current
                testHits = 0
                testController = model?.let { GazeController(it, GazeController.Config(s.sensitivity, s.dwellMs, s.repeatMs)) }
            }
            else -> testController = null
        }
    }

    val previewVisible = phase == CalPhase.Intro || phase == CalPhase.Test
    val previewAlpha by animateFloatAsState(if (previewVisible) 1f else 0f, tween(300), label = "pv")

    Box(Modifier.fillMaxSize()) {
        // Camera preview stays in composition the whole time; it is only faded out while the dots run.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(top = 64.dp)
                .size(if (phase == CalPhase.Test) 120.dp else 190.dp)
                .graphicsLayer { alpha = previewAlpha }
                .clip(CircleShape)
                .border(
                    3.dp,
                    Brush.sweepGradient(listOf(EsColors.Primary, EsColors.Secondary, EsColors.Primary)),
                    CircleShape,
                ),
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(200))) },
            label = "cal",
        ) { p ->
            when (p) {
                CalPhase.Intro -> IntroContent(frame, error, onCancel) {
                    attempt++
                    phase = CalPhase.Countdown
                }
                CalPhase.Countdown -> CountdownContent(countdown)
                CalPhase.Running -> RunningContent(target, stepProgress.value, frame)
                CalPhase.Result -> ResultContent(
                    result = result,
                    onTest = { phase = CalPhase.Test },
                    onRetry = {
                        attempt++
                        phase = CalPhase.Countdown
                    },
                    onDone = onFinished,
                )
                CalPhase.Test -> TestContent(
                    snapshot = testSnapshot,
                    hits = testHits,
                    onDone = onFinished,
                    onRetry = {
                        attempt++
                        phase = CalPhase.Countdown
                    },
                )
            }
        }
    }
}

private fun evaluate(store: SettingsStore, calibration: Calibration): CalResult {
    val perTarget = TARGETS.map { t -> calibration.samples.count { it.targetY == t } }
    if (perTarget.any { it < 8 }) {
        val missing = TARGET_NAMES.filterIndexed { i, _ -> perTarget[i] < 8 }.joinToString(", ")
        return CalResult(
            null, false, "Face not visible enough",
            "We lost your face during the $missing dot. Hold the phone in front of you with your whole face in view, in good light, and try again.",
        )
    }
    val mode = store.current.mode
    var model = calibration.model(mode)
    var switched = false
    if (model?.isUsable != true && mode == TrackingMode.EYES_ONLY) {
        val smart = calibration.model(TrackingMode.EYES_AND_HEAD)
        if (smart?.isUsable == true) {
            model = smart
            switched = true
        }
    }
    if (model == null || !model.isUsable) {
        return CalResult(
            model, false, "Up and down looked too similar",
            "Try again and move your eyes clearly to each dot. Hold the phone at normal reading distance, slightly below eye level. A tiny nod with your eyes helps a lot.",
        )
    }
    store.saveCalibration(calibration)
    if (switched) store.update { it.copy(mode = TrackingMode.EYES_AND_HEAD) }
    val extra = if (switched) " Switched to Smart mode (eyes + tiny head movement) for better accuracy." else ""
    return CalResult(model, true, "Calibration ${model.quality.label.lowercase()}", "EyeScroll has learned your eyes.$extra")
}

@Composable
private fun IntroContent(frame: FaceFrame?, error: String?, onCancel: () -> Unit, onStart: () -> Unit) {
    val face = frame?.faceFound == true
    val lightOk = frame == null || frame.brightness < 0f || frame.brightness > 0.16f
    val distanceOk = !face || frame!!.eyeWidthPx > 16f
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Filled.ArrowBack, "Back", tint = EsColors.Text) }
            Text("Calibration", style = MaterialTheme.typography.titleLarge, color = EsColors.Text)
        }
        Spacer(Modifier.height(210.dp))
        Column(Modifier.padding(horizontal = 20.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Follow the dot with your eyes",
                style = MaterialTheme.typography.headlineSmall,
                color = EsColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "A dot appears at the top, middle and bottom of the screen — about 10 seconds in total. Hold the phone the way you normally read and keep looking at the dot.",
                style = MaterialTheme.typography.bodyMedium,
                color = EsColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                CheckRow(face, if (face) "Face detected" else "Looking for your face…")
                CheckRow(lightOk, if (lightOk) "Lighting OK" else "Too dark — turn on a light")
                CheckRow(distanceOk && face, if (distanceOk) "Distance OK" else "Bring the phone a little closer")
            }
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = EsColors.Danger)
            }
        }
        GradientButton(
            text = if (face) "Start · 10 seconds" else "Waiting for your face…",
            onClick = onStart,
            enabled = face,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        )
    }
}

@Composable
private fun CheckRow(ok: Boolean, text: String) {
    val color = if (ok) EsColors.Success else EsColors.Warning
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(if (ok) Icons.Filled.Check else Icons.Filled.Warning, null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (ok) EsColors.Text else color)
    }
}

@Composable
private fun CountdownContent(value: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Get ready — follow the dot", style = MaterialTheme.typography.titleMedium, color = EsColors.TextMuted)
            Spacer(Modifier.height(18.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (scaleIn(tween(350), initialScale = 1.6f) + fadeIn(tween(250)))
                        .togetherWith(scaleOut(tween(250), targetScale = 0.6f) + fadeOut(tween(200)))
                },
                label = "count",
            ) { v ->
                Text("$v", fontSize = 96.sp, fontWeight = FontWeight.Bold, color = EsColors.Primary)
            }
        }
    }
}

@Composable
private fun RunningContent(target: Int, progress: Float, frame: FaceFrame?) {
    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(vertical = 8.dp)) {
        val positions = listOf(0f, 0.5f, 1f)
        val dotSize = 76.dp
        val y by animateFloatAsState(
            if (target in 0..2) positions[target] else 0.5f,
            tween(MOVE_MS.toInt() - 50, easing = FastOutSlowInEasing),
            label = "dotY",
        )
        val overall = ((target.coerceAtLeast(0) + progress) / 3f).coerceIn(0f, 1f)

        // overall progress
        Box(
            Modifier.align(Alignment.Center).offset(y = if (target == 2) (-maxHeight * 0.18f) else maxHeight * 0.18f)
                .width(180.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (target in 0..2) "Look at the ${TARGET_NAMES[target]} dot" else "Look at the dot",
                    style = MaterialTheme.typography.titleMedium,
                    color = EsColors.Text,
                )
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
                    Box(Modifier.fillMaxWidth(overall).height(5.dp).clip(RoundedCornerShape(50)).background(EsColors.Accent))
                }
                Spacer(Modifier.height(8.dp))
                val faceLost = frame?.faceFound == false
                Text(
                    if (faceLost) "Face not visible — keep it in view" else "${target.coerceAtLeast(0) + 1} / 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (faceLost) EsColors.Warning else EsColors.TextMuted,
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (maxHeight - dotSize) * y)
                .size(dotSize),
        ) { CalibrationDot(progress) }
    }
}

@Composable
private fun CalibrationDot(progress: Float) {
    val t = rememberInfiniteTransition(label = "dot")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
    Canvas(Modifier.fillMaxSize()) {
        val c = center
        val r = size.minDimension / 2
        drawCircle(EsColors.Primary.copy(alpha = 0.30f * (1f - pulse)), r * (0.45f + 0.55f * pulse), c)
        drawCircle(Brush.radialGradient(listOf(EsColors.Primary.copy(alpha = 0.35f), Color.Transparent), c, r * 0.7f), r * 0.7f, c)
        drawCircle(Color.White.copy(alpha = 0.10f), r * 0.62f, c, style = Stroke(width = r * 0.08f))
        drawArc(
            EsColors.Primary,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f),
            size = Size(r * 1.24f, r * 1.24f),
            style = Stroke(width = r * 0.08f, cap = StrokeCap.Round),
        )
        drawCircle(Color.White, r * 0.2f, c)
        drawCircle(EsColors.Primary, r * 0.09f, c)
    }
}

@Composable
private fun ResultContent(result: CalResult?, onTest: () -> Unit, onRetry: () -> Unit, onDone: () -> Unit) {
    val ok = result?.ok == true
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val scale = remember { Animatable(0.4f) }
        LaunchedEffect(Unit) { scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 300f)) }
        Box(
            Modifier
                .size(110.dp)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .clip(CircleShape)
                .background((if (ok) EsColors.Success else EsColors.Warning).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                null,
                tint = if (ok) EsColors.Success else EsColors.Warning,
                modifier = Modifier.size(60.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(result?.title ?: "", style = MaterialTheme.typography.headlineSmall, color = EsColors.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(result?.message ?: "", style = MaterialTheme.typography.bodyMedium, color = EsColors.TextMuted, textAlign = TextAlign.Center)
        val model = result?.model
        if (ok && model != null) {
            Spacer(Modifier.height(18.dp))
            GlassCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                QualityBar(label = "Up / down separation", value = (model.separation / 2.0).toFloat().coerceIn(0f, 1f))
            }
        }
        Spacer(Modifier.height(28.dp))
        if (ok) {
            GradientButton("Test it live", onTest, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton("Redo", onRetry, Modifier.weight(1f), icon = Icons.Filled.Refresh)
                GhostButton("Done", onDone, Modifier.weight(1f), icon = Icons.Filled.Check)
            }
        } else {
            GradientButton("Try again", onRetry, Modifier.fillMaxWidth(), icon = Icons.Filled.Refresh)
        }
    }
}

@Composable
private fun QualityBar(label: String, value: Float) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value, tween(900, easing = FastOutSlowInEasing)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = EsColors.TextMuted, modifier = Modifier.weight(1f))
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = EsColors.Text)
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth(anim.value).height(8.dp).clip(RoundedCornerShape(50)).background(EsColors.Accent))
    }
}

@Composable
private fun TestContent(snapshot: GazeSnapshot, hits: Int, onDone: () -> Unit, onRetry: () -> Unit) {
    Row(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Column(Modifier.weight(1f).padding(end = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(150.dp))
            Text("Live test", style = MaterialTheme.typography.headlineSmall, color = EsColors.Text)
            Spacer(Modifier.height(6.dp))
            Text(
                "Look at the top edge, then the bottom edge of the screen. The bar on the right shows where EyeScroll thinks you look.",
                style = MaterialTheme.typography.bodyMedium,
                color = EsColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            val (icon, text, color) = when {
                !snapshot.faceFound -> Triple(Icons.Filled.Warning, "Face not visible", EsColors.Warning)
                snapshot.zone == GazeZone.UP -> Triple(Icons.Filled.KeyboardArrowUp, "Scroll UP zone", EsColors.Primary)
                snapshot.zone == GazeZone.DOWN -> Triple(Icons.Filled.KeyboardArrowDown, "Scroll DOWN zone", EsColors.Secondary)
                else -> Triple(Icons.Filled.Check, "Reading — no scroll", EsColors.Success)
            }
            AnimatedContent(targetState = Triple(icon, text, color), label = "zone") { (i, tx, c) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(84.dp).clip(CircleShape).background(c.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(i, null, tint = c, modifier = Modifier.size(52.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(tx, style = MaterialTheme.typography.titleMedium, color = c)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Scroll triggers: $hits", style = MaterialTheme.typography.labelMedium, color = EsColors.TextMuted)
            Spacer(Modifier.weight(1f))
            GradientButton("Looks good", onDone, Modifier.fillMaxWidth(), icon = Icons.Filled.Check)
            Spacer(Modifier.height(10.dp))
            GhostButton("Recalibrate", onRetry, Modifier.fillMaxWidth(), icon = Icons.Filled.Refresh)
        }
        GazeMeter(snapshot, Modifier.width(18.dp).fillMaxSize())
    }
}
