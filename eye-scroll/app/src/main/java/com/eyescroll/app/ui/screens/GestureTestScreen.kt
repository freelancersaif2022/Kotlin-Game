package com.eyescroll.app.ui.screens

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyescroll.app.Readiness
import com.eyescroll.app.data.AppSettings
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.gesture.GestureDetector
import com.eyescroll.app.gesture.GestureMode
import com.eyescroll.app.gesture.HandFrame
import com.eyescroll.app.gesture.HandSnapshot
import com.eyescroll.app.gesture.HandTracker
import com.eyescroll.app.gesture.HandZone
import com.eyescroll.app.gesture.ScrollDirection
import com.eyescroll.app.service.GestureTrackingService
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.theme.EsColors

/** Live camera test: shows the detected fingertip and every gesture, without scrolling anything. */
@Composable
fun GestureTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { SettingsStore.get(context) }
    val settings by store.settings.collectAsStateWithLifecycle()
    val handler = remember { Handler(Looper.getMainLooper()) }

    var snapshot by remember { mutableStateOf(HandSnapshot()) }
    var error by remember { mutableStateOf<String?>(null) }
    var fps by remember { mutableIntStateOf(0) }
    var lastGesture by remember { mutableStateOf<ScrollDirection?>(null) }
    var gestureCount by remember { mutableIntStateOf(0) }
    val trail = remember { ArrayDeque<Offset>() }
    val detector = remember { GestureDetector(settings.toConfig()) }
    val flash = remember { Animatable(0f) }

    LaunchedEffect(settings) { detector.config = settings.toConfig() }
    LaunchedEffect(gestureCount) {
        if (gestureCount > 0) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(700))
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val fpsCounter = remember { longArrayOf(0L, 0L) } // window start, frames
    val handleFrame: (HandFrame) -> Unit = { f ->
        val now = SystemClock.uptimeMillis()
        val (snap, fired) = detector.process(f, now)
        snapshot = snap
        if (snap.handFound) {
            trail.addLast(Offset(1f - snap.tipX, snap.tipY))
            while (trail.size > 14) trail.removeFirst()
        } else {
            trail.clear()
        }
        if (fired != null) {
            lastGesture = fired
            gestureCount++
        }
        if (fpsCounter[0] == 0L) fpsCounter[0] = now
        fpsCounter[1]++
        if (now - fpsCounter[0] >= 1000) {
            fps = (fpsCounter[1] * 1000 / (now - fpsCounter[0])).toInt()
            fpsCounter[0] = now
            fpsCounter[1] = 0
        }
    }

    DisposableEffect(Unit) {
        if (!Readiness.hasCamera(context)) {
            error = "Camera permission is needed. Allow it in Setup."
            onDispose { }
        } else {
            GestureTrackingService.stop(context)
            val tracker = HandTracker(context, lifecycleOwner, 30, object : HandTracker.Listener {
                override fun onFrame(frame: HandFrame) {
                    handler.post { handleFrame(frame) }
                }

                override fun onError(message: String) {
                    handler.post { error = message }
                }
            })
            handler.postDelayed({ tracker.start(previewView.surfaceProvider) }, 300)
            onDispose {
                handler.removeCallbacksAndMessages(null)
                tracker.close()
            }
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = EsColors.Text) }
            Text("Gesture test", style = MaterialTheme.typography.titleLarge, color = EsColors.Text, modifier = Modifier.weight(1f))
            Text("$fps fps", style = MaterialTheme.typography.labelMedium, color = EsColors.TextMuted, modifier = Modifier.padding(end = 12.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier.size(150.dp).clip(CircleShape)
                        .border(3.dp, Brush.sweepGradient(listOf(EsColors.Primary, EsColors.Secondary, EsColors.Primary)), CircleShape),
                ) {
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                }
                // Radar: where the fingertip is in the camera frame (mirrored like a selfie).
                Box(
                    Modifier.width(120.dp).aspectRatio(0.75f).clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        if (snapshot.neutralY >= 0f) {
                            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, snapshot.neutralY * h), Offset(w, snapshot.neutralY * h), strokeWidth = 2f, pathEffect = dash)
                        }
                        trail.forEachIndexed { i, p ->
                            val a = (i + 1f) / trail.size
                            drawCircle(EsColors.Primary.copy(alpha = 0.15f + 0.35f * a), 3f + 6f * a, Offset(p.x * w, p.y * h))
                        }
                        if (snapshot.handFound) {
                            val c = when (snapshot.zone) {
                                HandZone.UP -> EsColors.Primary
                                HandZone.DOWN -> EsColors.Secondary
                                HandZone.CALIBRATING -> EsColors.Warning
                                else -> Color.White
                            }
                            val p = Offset((1f - snapshot.tipX) * w, snapshot.tipY * h)
                            drawCircle(c.copy(alpha = 0.3f), 22f, p)
                            drawCircle(c, 11f, p)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            val statusText = when {
                error != null -> error!!
                !snapshot.handFound -> "Show your hand to the front camera (30–150 cm away) and point your index finger up"
                snapshot.zone == HandZone.CALIBRATING -> "Hold your finger still for a moment…"
                settings.mode == GestureMode.SWIPE -> "Hand detected ✓ Now move your finger quickly up or down"
                else -> "Hand detected ✓ Raise or lower your finger and hold it there"
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) EsColors.Danger else if (snapshot.handFound) EsColors.Success else EsColors.TextMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))
            GlassCard(Modifier.fillMaxWidth(), highlighted = flash.value > 0.05f, padding = 20.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Detected gesture", style = MaterialTheme.typography.labelSmall, color = EsColors.TextMuted)
                    Spacer(Modifier.height(10.dp))
                    AnimatedContent(
                        targetState = gestureCount to lastGesture,
                        transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()).togetherWith(fadeOut()) },
                        label = "gesture",
                    ) { (_, g) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val color = when (g) {
                                ScrollDirection.UP -> EsColors.Primary
                                ScrollDirection.DOWN -> EsColors.Secondary
                                null -> EsColors.TextFaint
                            }
                            Box(
                                Modifier.size(84.dp).graphicsLayer { scaleX = 1f + flash.value * 0.12f; scaleY = scaleX }
                                    .clip(CircleShape).background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (g == ScrollDirection.UP) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    null,
                                    tint = color,
                                    modifier = Modifier.size(56.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when (g) {
                                    ScrollDirection.UP -> "Scroll UP"
                                    ScrollDirection.DOWN -> "Scroll DOWN"
                                    null -> "Waiting for a gesture"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (g == null) EsColors.TextMuted else color,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Gestures detected: $gestureCount", style = MaterialTheme.typography.labelMedium, color = EsColors.TextMuted)
                }
            }

            Spacer(Modifier.height(14.dp))
            GlassCard(Modifier.fillMaxWidth().padding(bottom = 24.dp), padding = 16.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, null, tint = EsColors.Warning, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Tips: keep your hand inside the camera view, use good light, and move from the wrist. " +
                            "Change Swipe / Hold mode, direction and sensitivity on the Home screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EsColors.TextMuted,
                    )
                }
            }
        }
    }
}

private fun AppSettings.toConfig() = GestureDetector.Config(mode, natural, sensitivity, repeatMs)
