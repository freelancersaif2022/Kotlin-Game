package com.spectral.ghostdetector.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spectral.ghostdetector.audio.Loop
import com.spectral.ghostdetector.audio.Sfx
import com.spectral.ghostdetector.data.Detection
import com.spectral.ghostdetector.ui.components.BlinkingDot
import com.spectral.ghostdetector.ui.components.DisclaimerText
import com.spectral.ghostdetector.ui.components.GhostBackground
import com.spectral.ghostdetector.ui.components.GlassIconButton
import com.spectral.ghostdetector.ui.components.GlassPanel
import com.spectral.ghostdetector.ui.components.NeonButton
import com.spectral.ghostdetector.ui.components.RadarScope
import com.spectral.ghostdetector.ui.components.ScanLines
import com.spectral.ghostdetector.ui.components.SectionLabel
import com.spectral.ghostdetector.ui.components.drawVignette
import com.spectral.ghostdetector.ui.components.soundClick
import com.spectral.ghostdetector.ui.findActivity
import com.spectral.ghostdetector.ui.rememberApp
import com.spectral.ghostdetector.ui.theme.GColors
import com.spectral.ghostdetector.ui.theme.GFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.random.Random

private enum class ScanState { CALIBRATING, SCANNING, PAUSED }

private data class Banner(val id: Long, val text: String, val danger: Boolean)

private val evpPhrases = listOf(
    "…get… out…", "…help… me…", "…who… are… you…", "…cold… so cold…", "…behind… you…",
    "…leave… this… place…", "…I'm… still… here…", "…find… me…", "…don't… go…", "…it's… dark…",
)

@Composable
fun ScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasPermission()) }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasPermission() }

    if (granted) {
        ScannerContent(onBack)
    } else {
        BackHandler(onBack = onBack)
        PermissionGate(
            onRequest = {
                val activity = context.findActivity()
                val permanentlyDenied = askedOnce && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                if (permanentlyDenied) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            },
            onBack = onBack,
        )
    }
}

@Composable
private fun ScannerContent(onBack: () -> Unit) {
    val app = rememberApp()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mag = rememberMagnetometer()
    val hud = remember { HudState() }

    var state by remember { mutableStateOf(ScanState.CALIBRATING) }
    var torch by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var nightVision by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<Banner?>(null) }
    var selected by remember { mutableStateOf<TrackedGhost?>(null) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var lastEvp by remember { mutableLongStateOf(0L) }
    var leaving by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val calib = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        app.prefs.nightVision.collect { nightVision = it }
    }

    fun showBanner(text: String, danger: Boolean) {
        banner = Banner(System.nanoTime(), text, danger)
    }

    val onEvent by rememberUpdatedState<(EngineEvent) -> Unit> { ev ->
        when (ev) {
            is EngineEvent.Spawned -> {
                val p = ev.ghost.profile
                app.sounds.play(Sfx.PING, 0.9f)
                app.sounds.play(Sfx.LOCK, 0.7f)
                app.haptics.detect()
                if (p.threat >= 4) {
                    app.sounds.play(Sfx.ALARM, 0.8f)
                    app.haptics.alarm()
                    showBanner("HIGH THREAT ENTITY · KEEP YOUR DISTANCE", true)
                } else {
                    showBanner("PARANORMAL ENTITY DETECTED", false)
                }
                app.appScope.launch {
                    app.prefs.addDetection(
                        Detection(System.currentTimeMillis(), p.name, p.age, p.type.label, p.threat, p.mood)
                    )
                }
            }
            is EngineEvent.Vanished -> {
                app.sounds.play(Sfx.VANISH, 0.8f)
                app.sounds.play(Sfx.STATIC, 0.35f)
                app.haptics.vanish()
                showBanner("SIGNAL LOST · ENTITY VANISHED", false)
            }
            is EngineEvent.Warning -> {
                app.sounds.play(Sfx.STATIC, 0.4f)
                showBanner(ev.text, ev.danger)
            }
        }
    }
    val engine = remember { GhostEngine { onEvent(it) } }

    // ---- audio loops for this screen ----
    DisposableEffect(Unit) {
        app.sounds.startLoop(Loop.HUM, 0.22f)
        onDispose {
            app.sounds.stopLoop(Loop.HUM)
            app.sounds.stopLoop(Loop.HEARTBEAT)
        }
    }

    // ---- calibration → scanning ----
    LaunchedEffect(state) {
        when (state) {
            ScanState.CALIBRATING -> {
                engine.running = false
                calib.snapTo(0f)
                app.sounds.play(Sfx.SCAN_START, 0.8f)
                calib.animateTo(1f, tween(2600, easing = LinearEasing))
                app.sounds.play(Sfx.LOCK, 0.7f)
                showBanner("SENSORS CALIBRATED · SCANNING AREA", false)
                state = ScanState.SCANNING
            }
            ScanState.SCANNING -> engine.running = true
            ScanState.PAUSED -> {
                engine.running = false
                engine.vanishAll()
            }
        }
    }

    // ---- main 60fps loop ----
    LaunchedEffect(Unit) {
        var last = 0L
        var heartbeat = false
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now
                val scanning = state == ScanState.SCANNING
                engine.update(dt)
                hud.update(dt, engine, mag, scanning)
                if (scanning) elapsed += dt
                // geiger-counter clicks that speed up with EMF
                if (state != ScanState.PAUSED) {
                    val rate = (0.7f + hud.emf * 0.3f).coerceAtMost(26f)
                    if (Random.nextFloat() < rate * dt) {
                        app.sounds.play(Sfx.TICK, Random.nextFloat() * 0.25f + 0.15f, Random.nextFloat() * 0.4f + 0.8f)
                    }
                }
                val danger = engine.ghosts.any { it.profile.threat >= 4 && it.phase != Phase.VANISHING }
                if (danger != heartbeat) {
                    heartbeat = danger
                    if (danger) app.sounds.startLoop(Loop.HEARTBEAT, 0.7f) else app.sounds.stopLoop(Loop.HEARTBEAT)
                }
                app.sounds.setLoopVolume(Loop.HUM, 0.18f + hud.activity * 0.5f)
            }
        }
    }

    LaunchedEffect(banner?.id) {
        if (banner != null) {
            delay(2800)
            banner = null
        }
    }

    fun leave() {
        if (leaving) return
        leaving = true
        engine.running = false
        app.ads.showInterstitial(context.findActivity()) { onBack() }
    }
    BackHandler {
        if (selected != null) selected = null else leave()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            torch = torch,
            onFlashAvailable = { hasFlash = it },
            onError = { cameraError = it },
            modifier = Modifier.fillMaxSize(),
        )

        // ---- night vision / atmosphere layer ----
        Canvas(Modifier.fillMaxSize()) {
            hud.frame // redraw every frame
            if (nightVision) {
                drawRect(GColors.Neon.copy(alpha = 0.16f))
                repeat(260) {
                    drawCircle(
                        Color.White.copy(alpha = Random.nextFloat() * 0.12f),
                        radius = Random.nextFloat() * 1.6f + 0.4f,
                        center = Offset(Random.nextFloat() * size.width, Random.nextFloat() * size.height),
                    )
                }
            }
            val danger = engine.ghosts.any { it.profile.threat >= 4 && it.phase != Phase.VANISHING }
            if (danger) {
                val pulse = 0.5f + 0.5f * sin(elapsed * 6f)
                drawRect(
                    Brush.radialGradient(
                        0.6f to Color.Transparent,
                        1f to GColors.Danger.copy(alpha = 0.35f * pulse),
                        center = center, radius = size.maxDimension * 0.7f,
                    )
                )
            }
            drawVignette(0.6f)
        }
        ScanLines(Modifier.fillMaxSize(), alpha = 0.035f)
        Viewfinder(Modifier.fillMaxSize(), hud)

        GhostOverlay(
            engine = engine,
            selectedId = selected?.id,
            onGhostTapped = { g ->
                if (g != null) {
                    app.sounds.play(Sfx.LOCK, 0.8f)
                    app.haptics.tap()
                }
                selected = g
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ---- HUD ----
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopHud(
                state = state,
                elapsed = { elapsed },
                nightVision = nightVision,
                onBack = { leave() },
                onNightVision = {
                    nightVision = !nightVision
                    scope.launch { app.prefs.setNightVision(nightVision) }
                },
            )
            ReadoutStrip(hud, engine.ghosts.size)
            BannerView(banner)
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
                EmfMeter(hud, Modifier.padding(top = 18.dp))
                Spacer(Modifier.weight(1f))
                MiniRadar(engine, Modifier.padding(top = 18.dp))
            }
            WaveformPanel(hud, Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(10.dp))
            BottomControls(
                state = state,
                torch = torch,
                hasFlash = hasFlash,
                onEvp = {
                    val now = System.currentTimeMillis()
                    if (now - lastEvp > 3500) {
                        lastEvp = now
                        app.sounds.play(Sfx.WHISPER, 1f)
                        app.haptics.detect()
                        showBanner("EVP CAPTURED: \"${evpPhrases.random()}\"", false)
                    }
                },
                onToggleScan = {
                    state = if (state == ScanState.PAUSED) ScanState.CALIBRATING else ScanState.PAUSED
                },
                onTorch = { torch = !torch },
            )
            DisclaimerText(Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }

        if (state == ScanState.CALIBRATING) CalibrationOverlay(calib.value)

        AnimatedVisibility(
            visible = selected != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val g = selected
            if (g != null) {
                GhostDetailPanel(
                    ghost = g,
                    onListen = {
                        app.sounds.play(Sfx.WHISPER, 1f)
                        showBanner("EVP FROM ${g.profile.name.uppercase()}: \"${evpPhrases.random()}\"", false)
                    },
                    onClose = { selected = null },
                )
            }
        }

        cameraError?.let {
            Text(
                "Camera unavailable: $it",
                color = GColors.Danger, modifier = Modifier.align(Alignment.Center).padding(24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MiniRadar(engine: GhostEngine, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        val blips = engine.ghosts.map { g ->
            val dx = g.x - 0.5f
            val dy = g.y - 0.5f
            atan2(dy, dx) to (hypot(dx, dy) * 1.7f + 0.15f).coerceAtMost(0.95f)
        }
        RadarScope(Modifier.size(96.dp), speedMs = 2200, blips = blips, showRandomBlips = false)
        Spacer(Modifier.height(4.dp))
        Text("RANGE 8m", color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 9.sp)
    }
}

@Composable
private fun CameraPreview(
    torch: Boolean,
    onFlashAvailable: (Boolean) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                p.unbindAll()
                val cam = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                camera = cam
                onFlashAvailable(cam.cameraInfo.hasFlashUnit())
            } catch (e: Exception) {
                onError(e.message ?: "unknown error")
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }
    LaunchedEffect(camera, torch) {
        val cam = camera ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) cam.cameraControl.enableTorch(torch)
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun TopHud(
    state: ScanState,
    elapsed: () -> Float,
    nightVision: Boolean,
    onBack: () -> Unit,
    onNightVision: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
        Spacer(Modifier.weight(1f))
        GlassPanel(corner = 23.dp, padding = 0.dp) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (label, color) = when (state) {
                    ScanState.CALIBRATING -> "CALIBRATING" to GColors.Amber
                    ScanState.SCANNING -> "SCANNING" to GColors.Neon
                    ScanState.PAUSED -> "PAUSED" to GColors.TextSecondary
                }
                if (state == ScanState.PAUSED) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                } else {
                    BlinkingDot(color)
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = color, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp))
                Spacer(Modifier.width(10.dp))
                val s = elapsed().toInt()
                Text("%02d:%02d".format(s / 60, s % 60), color = GColors.TextPrimary, fontFamily = GFonts.Mono, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        GlassIconButton(Icons.Rounded.NightsStay, "Night vision", active = nightVision, onClick = onNightVision)
    }
}

@Composable
private fun ReadoutStrip(hud: HudState, entities: Int) {
    GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 12.dp), corner = 14.dp, padding = 10.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Readout("EMF", "%.1f".format(hud.emf), "mG", emfColor(hud.emf))
            Readout("TEMP", "%.1f".format(hud.temp), "°C", if (hud.temp < 18f) GColors.Amber else GColors.Neon)
            Readout("FREQ", "%.0f".format(hud.freq), "Hz")
            Readout("ENTITIES", "$entities", "", if (entities > 0) GColors.Amber else GColors.Neon)
        }
    }
}

@Composable
private fun Readout(label: String, value: String, unit: String, color: Color = GColors.Neon) {
    Column {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = color, fontFamily = GFonts.Mono, fontSize = 17.sp)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(unit, color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

private fun emfColor(v: Float) = when {
    v > 35f -> GColors.Danger
    v > 12f -> GColors.Amber
    else -> GColors.Neon
}

@Composable
private fun BannerView(banner: Banner?) {
    Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = fadeOut(),
        ) {
            val b = banner ?: return@AnimatedVisibility
            val color = if (b.danger) GColors.Danger else GColors.Amber
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.12f))))
                    .border(1.dp, color, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Warning, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(b.text, color = GColors.TextPrimary, fontFamily = GFonts.Mono, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun EmfMeter(hud: HudState, modifier: Modifier = Modifier) {
    val emf = hud.emf
    GlassPanel(modifier, corner = 12.dp, padding = 8.dp) {
        SectionLabel("EMF")
        Spacer(Modifier.height(6.dp))
        // logarithmic scale 0.1 .. 100 mG
        val level = ((ln(emf.coerceAtLeast(0.1f) / 0.1f)) / ln(1000f)).coerceIn(0f, 1f)
        val segments = 16
        Canvas(Modifier.width(26.dp).height(170.dp)) {
            val gap = 3.dp.toPx()
            val segH = (size.height - gap * (segments - 1)) / segments
            for (i in 0 until segments) {
                val frac = (i + 1f) / segments
                val y = size.height - (i + 1) * segH - i * gap
                val col = when {
                    frac > 0.8f -> GColors.Danger
                    frac > 0.55f -> GColors.Amber
                    else -> GColors.Neon
                }
                val on = frac <= level + 0.001f
                drawRoundRect(
                    if (on) col else col.copy(alpha = 0.12f),
                    topLeft = Offset(0f, y), size = Size(size.width, segH), cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("%.0f".format(emf), color = emfColor(emf), fontFamily = GFonts.Mono, fontSize = 12.sp)
    }
}

@Composable
private fun WaveformPanel(hud: HudState, modifier: Modifier = Modifier) {
    GlassPanel(modifier.fillMaxWidth(), corner = 14.dp, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("EVP SPECTRAL ANALYZER")
            Spacer(Modifier.weight(1f))
            SectionLabel("%.0f%% ACTIVITY".format(hud.activity * 100), color = if (hud.activity > 0.4f) GColors.Amber else GColors.Neon)
        }
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(46.dp)) {
            hud.frame
            val midY = size.height / 2f
            drawLine(GColors.Neon.copy(alpha = 0.15f), Offset(0f, midY), Offset(size.width, midY), 1f)
            val n = hud.waveSize
            val step = size.width / (n - 1)
            val path = Path()
            for (i in 0 until n) {
                val px = i * step
                val py = midY - hud.waveAt(i) * midY * 0.95f
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val col = if (hud.activity > 0.4f) GColors.Amber else GColors.Neon
            drawPath(path, col.copy(alpha = 0.25f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, col, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun BottomControls(
    state: ScanState,
    torch: Boolean,
    hasFlash: Boolean,
    onEvp: () -> Unit,
    onToggleScan: () -> Unit,
    onTorch: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabeledButton("EVP") { GlassIconButton(Icons.Rounded.GraphicEq, "EVP", onClick = onEvp) }
        val paused = state == ScanState.PAUSED
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            if (paused) listOf(GColors.Neon, GColors.NeonDim) else listOf(Color(0xFF1B5E3B), Color(0xFF07160F))
                        )
                    )
                    .border(3.dp, GColors.Neon, CircleShape)
                    .soundClick(onClick = onToggleScan),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (paused) "Resume" else "Pause",
                    tint = if (paused) GColors.Void else GColors.Neon,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            SectionLabel(if (paused) "RESUME" else "PAUSE")
        }
        LabeledButton("TORCH") {
            GlassIconButton(
                if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                "Torch",
                active = torch,
                tint = GColors.Amber,
                onClick = { if (hasFlash) onTorch() },
            )
        }
    }
}

@Composable
private fun LabeledButton(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        content()
        Spacer(Modifier.height(4.dp))
        SectionLabel(label)
    }
}

/** Camera viewfinder decorations: screen corner marks, center reticle and side ticks. */
@Composable
private fun Viewfinder(modifier: Modifier, hud: HudState) {
    Canvas(modifier) {
        hud.frame
        val m = 18.dp.toPx()
        val len = 28.dp.toPx()
        val sw = 2.dp.toPx()
        val c = GColors.Neon.copy(alpha = 0.55f)
        val w = size.width
        val h = size.height
        // screen corners
        drawLine(c, Offset(m, m + 60.dp.toPx()), Offset(m + len, m + 60.dp.toPx()), sw)
        drawLine(c, Offset(m, m + 60.dp.toPx()), Offset(m, m + 60.dp.toPx() + len), sw)
        drawLine(c, Offset(w - m, m + 60.dp.toPx()), Offset(w - m - len, m + 60.dp.toPx()), sw)
        drawLine(c, Offset(w - m, m + 60.dp.toPx()), Offset(w - m, m + 60.dp.toPx() + len), sw)
        val bottom = h - 210.dp.toPx()
        drawLine(c, Offset(m, bottom), Offset(m + len, bottom), sw)
        drawLine(c, Offset(m, bottom), Offset(m, bottom - len), sw)
        drawLine(c, Offset(w - m, bottom), Offset(w - m - len, bottom), sw)
        drawLine(c, Offset(w - m, bottom), Offset(w - m, bottom - len), sw)
        // center reticle
        val cx = w / 2f
        val cy = h / 2f
        val r = 34.dp.toPx()
        drawCircle(GColors.Neon.copy(alpha = 0.25f), radius = r, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
        val t = hud.frame / 60f
        for (i in 0 until 4) {
            val a = t * 0.8f + i * (Math.PI.toFloat() / 2f)
            drawArc(
                GColors.Neon.copy(alpha = 0.6f), startAngle = Math.toDegrees(a.toDouble()).toFloat(), sweepAngle = 30f,
                useCenter = false, topLeft = Offset(cx - r * 1.3f, cy - r * 1.3f), size = Size(r * 2.6f, r * 2.6f),
                style = Stroke(1.5.dp.toPx()),
            )
        }
        drawLine(GColors.Neon.copy(alpha = 0.5f), Offset(cx - 10.dp.toPx(), cy), Offset(cx + 10.dp.toPx(), cy), 1.dp.toPx())
        drawLine(GColors.Neon.copy(alpha = 0.5f), Offset(cx, cy - 10.dp.toPx()), Offset(cx, cy + 10.dp.toPx()), 1.dp.toPx())
    }
}

@Composable
private fun CalibrationOverlay(progress: Float) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.Black.copy(alpha = 0.55f))
                drawCircle(GColors.Neon.copy(alpha = 0.15f), style = Stroke(6.dp.toPx()))
                drawArc(
                    GColors.Neon, -90f, 360f * progress, false,
                    style = Stroke(6.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(progress * 100).toInt()}%", color = GColors.Neon, fontFamily = GFonts.Orbitron, fontSize = 30.sp)
                Spacer(Modifier.height(4.dp))
                SectionLabel("CALIBRATING", color = GColors.TextPrimary)
                SectionLabel(
                    when {
                        progress < 0.33f -> "EMF BASELINE"
                        progress < 0.66f -> "THERMAL GRID"
                        else -> "SPECTRAL FILTER"
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit, onBack: () -> Unit) {
    GhostBackground {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(110.dp).clip(CircleShape).background(GColors.Neon.copy(alpha = 0.12f)).border(2.dp, GColors.Neon, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PhotoCamera, null, tint = GColors.Neon, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("CAMERA ACCESS NEEDED", style = MaterialTheme.typography.headlineSmall, color = GColors.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "The spectral scanner overlays its tracking HUD on your live back-camera view. " +
                    "Nothing is recorded, saved or uploaded.",
                color = GColors.TextSecondary, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            NeonButton("Allow camera", Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera, onClick = onRequest)
            Spacer(Modifier.height(12.dp))
            NeonButton("Go back", Modifier.fillMaxWidth(), filled = false, onClick = onBack)
        }
    }
}
