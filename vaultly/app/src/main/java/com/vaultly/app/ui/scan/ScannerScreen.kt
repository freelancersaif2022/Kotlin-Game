package com.vaultly.app.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaultly.app.VaultlyApp
import com.vaultly.app.scan.LiveDetector
import com.vaultly.app.scan.Quad
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

private val DetectBlue = Color(0xFF3B82F6)

/** Camera scanner with live document detection (Auto) or plain shutter capture (Manual). */
@Composable
fun ScannerScreen(
    app: VaultlyApp,
    onClose: () -> Unit,
    onCaptured: (path: String, quad: Quad?) -> Unit,
    onPicked: (Uri) -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPicked(uri) }
    val pickPhoto = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraContent(app, onClose, onCaptured, pickPhoto)
        } else {
            PermissionContent(
                onClose = onClose,
                onAllow = {
                    if (askedOnce) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } else {
                        permission.launch(Manifest.permission.CAMERA)
                    }
                },
                onGallery = pickPhoto,
                settings = askedOnce,
            )
        }
    }
}

@Composable
private fun CameraContent(
    app: VaultlyApp,
    onClose: () -> Unit,
    onCaptured: (String, Quad?) -> Unit,
    onGallery: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val auto by app.prefs.autoCapture.collectAsStateWithLifecycle()

    val detector = remember { LiveDetector() }
    val live by detector.state.collectAsStateWithLifecycle()
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(
                ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build(),
            )
            .build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val flash = remember { Animatable(0f) }

    detector.auto = auto

    DisposableEffect(Unit) {
        var provider: ProcessCameraProvider? = null
        val job = scope.launch {
            val p = suspendCancellableCoroutine { cont ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
            }
            provider = p
            val ratio = ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()
            val preview = Preview.Builder().setResolutionSelector(ratio).build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, detector) }
            runCatching {
                p.unbindAll()
                camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis)
            }.onFailure { app.notifier.error("Couldn't open the camera. Try picking a photo instead.") }
        }
        onDispose {
            job.cancel()
            provider?.unbindAll()
            executor.shutdown()
        }
    }

    val capture: () -> Unit = capture@{
        if (capturing) return@capture
        capturing = true
        detector.paused = true
        val quad = detector.state.value.quad
        scope.launch {
            flash.snapTo(0.85f)
            flash.animateTo(0f, tween(380))
        }
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 3_600_000 }?.forEach { it.delete() }
        val file = File(dir, "scan-${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (torch) camera?.cameraControl?.enableTorch(false)
                    onCaptured(file.absolutePath, quad)
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    detector.resume()
                    app.notifier.error("Capture failed. Please try again.")
                }
            },
        )
    }

    LaunchedEffect(detector) {
        detector.captureRequests.collect { if (detector.auto) capture() else detector.resume() }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ previewView }, Modifier.fillMaxSize())

        // Detection overlay + tap to focus.
        BoxWithConstraints(
            Modifier.fillMaxSize().pointerInput(camera) {
                detectTapGestures { pos ->
                    val cam = camera ?: return@detectTapGestures
                    val point = previewView.meteringPointFactory.createPoint(pos.x, pos.y)
                    cam.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(point).setAutoCancelDuration(3, TimeUnit.SECONDS).build(),
                    )
                    focusPoint = pos
                }
            },
        ) {
            val vw = constraints.maxWidth.toFloat()
            val vh = constraints.maxHeight.toFloat()
            val q = live.quad
            val shown by animateFloatAsState(if (auto && q != null) 1f else 0f, tween(220), label = "quadAlpha")
            // Keeps the last shape while it fades out.
            val lastQuad = remember { arrayOfNulls<Quad>(1) }
            if (q != null) lastQuad[0] = q
            Canvas(Modifier.fillMaxSize()) {
                val quad = lastQuad[0] ?: return@Canvas
                if (shown <= 0.01f) return@Canvas
                val scale = max(vw / live.frameW, vh / live.frameH)
                val dx = (vw - live.frameW * scale) / 2
                val dy = (vh - live.frameH * scale) / 2
                val pts = quad.points.map { Offset(dx + it.x * live.frameW * scale, dy + it.y * live.frameH * scale) }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, DetectBlue.copy(alpha = 0.18f * shown))
                drawPath(path, DetectBlue.copy(alpha = shown), style = Stroke(3.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
                pts.forEach {
                    drawCircle(Color.White.copy(alpha = shown), 7.dp.toPx(), it)
                    drawCircle(DetectBlue.copy(alpha = shown), 7.dp.toPx(), it, style = Stroke(3.dp.toPx()))
                }
            }
            focusPoint?.let { fp -> FocusRing(fp) { focusPoint = null } }
        }

        // Shutter flash
        if (flash.value > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))

        // Top bar
        Box(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            GlassCircle(Icons.Outlined.Close, "Close", Modifier.align(Alignment.CenterStart), onClick = onClose)
            Text("Scan receipt", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(Alignment.Center))
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                GlassCircle(
                    if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    "Flash",
                    Modifier.align(Alignment.CenterEnd),
                    active = torch,
                ) {
                    torch = !torch
                    camera?.cameraControl?.enableTorch(torch)
                }
            }
        }

        // Hint pill – after a few seconds without a receipt, suggest how to help detection.
        var showTip by remember { mutableStateOf(false) }
        LaunchedEffect(live.quad == null, auto) {
            showTip = false
            if (auto && live.quad == null) {
                kotlinx.coroutines.delay(4000)
                showTip = true
            }
        }
        val hint = when {
            capturing -> "Capturing…"
            !auto -> "Line up the receipt, then tap the shutter"
            live.quad == null && showTip -> "Tip: place the receipt on a darker surface"
            live.quad == null -> "Looking for a receipt…"
            live.progress < 1f -> "Hold steady…"
            else -> "Capturing…"
        }
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 76.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(hint, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "hint") { text ->
                Row(
                    Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (auto) Icons.Outlined.AutoAwesome else Icons.Outlined.PanTool, null, Modifier.size(15.dp),
                        tint = if (auto && live.quad != null) Color(0xFF8AB8FF) else Palette.GoldLight,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }

        // Bottom controls
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .navigationBarsPadding().padding(top = 36.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ModeSwitch(auto) { app.prefs.setAutoCapture(it); detector.resume() }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassCircle(Icons.Outlined.PhotoLibrary, "Choose from photos", size = 52, onClick = onGallery)
                Spacer(Modifier.weight(1f))
                Shutter(progress = if (auto) live.progress else 0f, busy = capturing, onClick = capture)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(52.dp))
            }
        }
    }
}

@Composable
private fun ModeSwitch(auto: Boolean, onChange: (Boolean) -> Unit) {
    val width = 112.dp
    val offset by animateDpAsState(if (auto) 0.dp else width, spring(dampingRatio = 0.8f), label = "mode")
    Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).padding(4.dp)) {
        Box(
            Modifier.offset { IntOffset(offset.roundToPx(), 0) }.width(width).height(38.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold))),
        )
        Row {
            listOf(true to "Auto capture", false to "Manual").forEach { (value, label) ->
                val color by animateColorAsState(if (auto == value) Palette.Ink else Color.White, label = "modeText")
                Box(
                    Modifier.width(width).height(38.dp).clip(CircleShape).clickable { onChange(value) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = MaterialTheme.typography.labelMedium, color = color) }
            }
        }
    }
}

@Composable
private fun Shutter(progress: Float, busy: Boolean, onClick: () -> Unit) {
    val animated by animateFloatAsState(progress, tween(120), label = "shutter")
    val pressScale by animateFloatAsState(if (busy) 0.86f else 1f, spring(), label = "press")
    Box(Modifier.size(84.dp).clickable(enabled = !busy, onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White, radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
            if (animated > 0f) {
                drawArc(
                    DetectBlue, -90f, 360f * animated, false,
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawCircle(Color.White, radius = (size.minDimension / 2 - 10.dp.toPx()) * pressScale)
        }
        if (busy) CircularProgressIndicator(Modifier.size(28.dp), color = Palette.Ink, strokeWidth = 3.dp)
    }
}

@Composable
private fun FocusRing(at: Offset, onDone: () -> Unit) {
    val anim = remember(at) { Animatable(1.4f) }
    LaunchedEffect(at) {
        anim.animateTo(1f, spring(dampingRatio = 0.6f))
        kotlinx.coroutines.delay(700)
        onDone()
    }
    val density = LocalDensity.current
    val r = with(density) { 34.dp.toPx() }
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(Palette.GoldLight, r * anim.value, at, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun GlassCircle(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Int = 44,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(size.dp).clip(CircleShape)
            .background(if (active) Palette.GoldLight else Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size((size * 0.46f).dp), tint = if (active) Palette.Ink else Color.White) }
}

@Composable
private fun PermissionContent(onClose: () -> Unit, onAllow: () -> Unit, onGallery: () -> Unit, settings: Boolean) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B2A4A), Palette.Ink, Palette.InkDeep)))) {
        GlassCircle(Icons.Outlined.Close, "Close", Modifier.statusBarsPadding().padding(16.dp), onClick = onClose)
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Palette.GoldLight.copy(alpha = 0.35f), RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.CameraAlt, null, Modifier.size(44.dp), tint = Palette.GoldLight) }
            Spacer(Modifier.height(24.dp))
            Text("Camera access", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(
                "Vaultly uses the camera only to scan your receipts. Photos stay on your phone and are never uploaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PrimaryButton(if (settings) "Open settings" else "Allow camera", onAllow, Modifier.fillMaxWidth(), gold = true, icon = Icons.Outlined.CropFree)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onGallery),
                contentAlignment = Alignment.Center,
            ) { Text("Choose from photos instead", style = MaterialTheme.typography.labelLarge, color = Color.White) }
        }
    }
}
