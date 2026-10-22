package com.calorielens.app.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.LocalSnackbar
import com.calorielens.app.ui.components.OutlineButton
import com.calorielens.app.ui.components.SnackType
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class FlashMode(val icon: ImageVector, val label: String, val value: Int) {
    OFF(Icons.Rounded.FlashOff, "Off", ImageCapture.FLASH_MODE_OFF),
    AUTO(Icons.Rounded.FlashAuto, "Auto", ImageCapture.FLASH_MODE_AUTO),
    ON(Icons.Rounded.FlashOn, "On", ImageCapture.FLASH_MODE_ON),
}

private val tips = listOf(
    "Fit the whole meal inside the frame",
    "Good lighting improves accuracy",
    "Shoot from slightly above the plate",
    "One dish per photo works best",
)

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onCaptured: (String) -> Unit,
    vm: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val processing by vm.processing.collectAsStateWithLifecycle()

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            vm.importGallery(uri, onCaptured) { snackbar.show(it, SnackType.ERROR, title = "Couldn't open photo") }
        }
    }
    val openGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (granted) {
            CameraContent(
                onClose = onClose,
                onGallery = openGallery,
                processing = processing,
                onCapture = { controller ->
                    val file = vm.newCaptureFile()
                    vm.setProcessing(true)
                    controller.takePicture(
                        ImageCapture.OutputFileOptions.Builder(file).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                vm.importCapture(file, onCaptured) { snackbar.show(it, SnackType.ERROR) }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                vm.setProcessing(false)
                                snackbar.show(
                                    "Please hold steady and try again.",
                                    SnackType.ERROR,
                                    title = "Capture failed",
                                )
                            }
                        },
                    )
                },
            )
        } else {
            PermissionContent(
                permanentlyDenied = asked,
                onRequest = {
                    if (asked) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGallery = openGallery,
                onClose = onClose,
            )
        }

        AnimatedVisibility(
            visible = processing,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Brand.Mint, strokeWidth = 3.dp)
            }
        }
    }

    // Re-check permission when returning from system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun CameraContent(
    onClose: () -> Unit,
    onGallery: () -> Unit,
    processing: Boolean,
    onCapture: (LifecycleCameraController) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    var flash by remember { mutableStateOf(FlashMode.OFF) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusKey by remember { mutableLongStateOf(0L) }
    var focused by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    val shutterFlash = remember { Animatable(0f) }

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        controller.zoomState.observe(lifecycleOwner) { state ->
            zoom = state.zoomRatio
            maxZoom = state.maxZoomRatio
        }
        controller.tapToFocusState.observe(lifecycleOwner) { state ->
            focused = state == CameraController.TAP_TO_FOCUS_FOCUSED
        }
        controller.initializationFuture.addListener({ cameraReady = true }, ContextCompat.getMainExecutor(context))
        onDispose { controller.unbind() }
    }

    LaunchedEffect(flash) { controller.imageCaptureFlashMode = flash.value }

    // Fade focus ring away after a moment
    LaunchedEffect(focusKey) {
        if (focusKey != 0L) {
            delay(1400)
            focusPoint = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    this.controller = controller
                    var downX = 0f
                    var downY = 0f
                    var multi = false
                    setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; multi = false }
                            MotionEvent.ACTION_POINTER_DOWN -> multi = true
                            MotionEvent.ACTION_UP -> {
                                val moved = kotlin.math.hypot(event.x - downX, event.y - downY)
                                if (!multi && moved < 24f) {
                                    focusPoint = Offset(event.x, event.y)
                                    focused = false
                                    focusKey = System.currentTimeMillis()
                                }
                            }
                        }
                        false
                    }
                }
            },
        )

        // Camera warm-up curtain
        AnimatedVisibility(visible = !cameraReady, enter = fadeIn(), exit = fadeOut(tween(500))) {
            Box(Modifier.fillMaxSize().background(Color.Black))
        }

        // Top + bottom scrims for legibility
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))),
        )

        Viewfinder(Modifier.align(Alignment.Center))

        focusPoint?.let { p ->
            FocusRing(
                key = focusKey,
                focused = focused,
                modifier = Modifier.offset {
                    val half = with(density) { 36.dp.toPx() }
                    IntOffset((p.x - half).roundToInt(), (p.y - half).roundToInt())
                },
            )
        }

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(Icons.Rounded.Close, "Close", onClose)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveDot()
                Spacer(Modifier.width(8.dp))
                Text("AI Food Scanner", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            GlassButton(flash.icon, "Flash ${flash.label}", {
                flash = FlashMode.entries[(flash.ordinal + 1) % FlashMode.entries.size]
            }, tint = if (flash == FlashMode.OFF) Color.White else Brand.Amber)
        }

        // Bottom controls
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TipTicker()
            Spacer(Modifier.height(16.dp))
            ZoomSelector(
                zoom = zoom,
                maxZoom = maxZoom,
                onSelect = { controller.setZoomRatio(it) },
            )
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassButton(Icons.Rounded.PhotoLibrary, "Gallery", onGallery, size = 54)
                ShutterButton(enabled = cameraReady && !processing) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        shutterFlash.snapTo(0.9f)
                        shutterFlash.animateTo(0f, tween(380))
                    }
                    onCapture(controller)
                }
                GlassButton(Icons.Rounded.FlipCameraAndroid, "Switch camera", {
                    val front = controller.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
                    val target = if (front) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                    if (runCatching { controller.hasCamera(target) }.getOrDefault(false)) controller.cameraSelector = target
                }, size = 54)
            }
        }

        // Shutter flash
        if (shutterFlash.value > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shutterFlash.value }
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun LiveDot() {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "liveAlpha")
    Box(
        Modifier
            .size(8.dp)
            .graphicsLayer { alpha = a }
            .clip(CircleShape)
            .background(Brand.Mint),
    )
}

@Composable
private fun Viewfinder(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "viewfinder")
    val breathe by t.animateFloat(0.97f, 1.0f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val scan by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scanLine")
    val dash by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "dots")
    BoxWithConstraints(modifier.fillMaxWidth(0.8f).aspectRatio(1f)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = breathe; scaleY = breathe },
        ) {
            val w = size.width
            val h = size.height
            val corner = w * 0.14f
            val stroke = 5.dp.toPx()
            val brush = Brush.linearGradient(listOf(Brand.Mint, Brand.Cyan), Offset.Zero, Offset(w, h))
            val pts = listOf(
                Triple(Offset(0f, corner), Offset(0f, 0f), Offset(corner, 0f)),
                Triple(Offset(w - corner, 0f), Offset(w, 0f), Offset(w, corner)),
                Triple(Offset(0f, h - corner), Offset(0f, h), Offset(corner, h)),
                Triple(Offset(w - corner, h), Offset(w, h), Offset(w, h - corner)),
            )
            pts.forEach { (a, m, b) ->
                drawLine(Color.Black.copy(alpha = 0.25f), a, m, stroke + 4f, StrokeCap.Round)
                drawLine(Color.Black.copy(alpha = 0.25f), m, b, stroke + 4f, StrokeCap.Round)
                drawLine(brush, a, m, stroke, StrokeCap.Round)
                drawLine(brush, m, b, stroke, StrokeCap.Round)
            }
            // Center crosshair
            val c = center
            val cross = 10.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x - cross, c.y), Offset(c.x + cross, c.y), 1.5.dp.toPx(), StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x, c.y - cross), Offset(c.x, c.y + cross), 1.5.dp.toPx(), StrokeCap.Round)
            // Grid dots that twinkle
            val step = w / 6
            for (i in 1..5) for (j in 1..5) {
                val phase = ((i * 7 + j * 3) / 10f + dash) % 1f
                drawCircle(Color.White.copy(alpha = 0.06f + 0.14f * phase), 1.6.dp.toPx(), Offset(step * i, step * j))
            }
            // Laser
            val y = h * (0.06f + 0.88f * scan)
            val glowH = h * 0.18f
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Brand.Mint.copy(alpha = 0.22f)), y - glowH, y),
                topLeft = Offset(stroke * 2, y - glowH),
                size = Size(w - stroke * 4, glowH),
            )
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, Brand.Mint, Brand.Cyan, Color.Transparent)),
                Offset(stroke * 2, y),
                Offset(w - stroke * 2, y),
                2.5.dp.toPx(),
                StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun FocusRing(key: Long, focused: Boolean, modifier: Modifier) {
    val scale = remember(key) { Animatable(1.5f) }
    LaunchedEffect(key) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
    val color by animateColorAsState(if (focused) Brand.Mint else Color.White, tween(200), label = "focusColor")
    Canvas(
        modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    ) {
        drawCircle(color, size.minDimension / 2 - 2.dp.toPx(), style = Stroke(2.dp.toPx()))
        drawCircle(color.copy(alpha = 0.9f), 3.dp.toPx())
    }
}

@Composable
private fun TipTicker() {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            index = (index + 1) % tips.size
        }
    }
    AnimatedContent(
        targetState = index,
        transitionSpec = { (fadeIn(tween(350)) + scaleIn(tween(350), 0.92f)) togetherWith (fadeOut(tween(250)) + scaleOut(tween(250), 1.05f)) },
        label = "tips",
    ) { i ->
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Brand.Amber, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(tips[i], style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun ZoomSelector(zoom: Float, maxZoom: Float, onSelect: (Float) -> Unit) {
    val options = buildList {
        add(1f)
        if (maxZoom >= 2f) add(2f)
        if (maxZoom >= 3f) add(3f)
    }
    if (options.size < 2) return
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val active = options.lastOrNull { zoom >= it - 0.05f } ?: 1f
        options.forEach { option ->
            val selected = option == active
            val size by animateFloatAsState(if (selected) 40f else 34f, spring(), label = "zoomSize")
            Box(
                Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                    .pressScale(scaleDown = 0.9f) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (selected && zoom > option + 0.05f) "%.1fx".format(zoom) else "${option.toInt()}x",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Brand.Amber else Color.White,
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val inner by animateFloatAsState(if (pressed) 0.82f else 1f, spring(dampingRatio = 0.5f, stiffness = 700f), label = "shutterInner")
    val t = rememberInfiniteTransition(label = "shutterRing")
    val rotation by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ringRotation")
    Box(
        Modifier
            .size(88.dp)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            drawCircle(
                Brush.sweepGradient(listOf(Brand.Mint, Brand.Cyan, Color.White, Brand.Mint)),
                style = Stroke(4.dp.toPx()),
                radius = size.minDimension / 2 - 2.dp.toPx(),
            )
        }
        Box(
            Modifier
                .size(70.dp)
                .graphicsLayer { scaleX = inner; scaleY = inner; alpha = if (enabled) 1f else 0.5f }
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.CameraAlt, null, tint = Color(0xFF0B0D12), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun GlassButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    size: Int = 44,
) {
    Box(
        Modifier
            .size(size.dp)
            .pressScale(scaleDown = 0.88f, onClick = onClick)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size((size * 0.48f).dp))
    }
}

@Composable
private fun PermissionContent(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onGallery: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            Modifier
                .statusBarsPadding()
                .padding(16.dp),
        ) { GlassButton(Icons.Rounded.Close, "Close", onClose) }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brand.Mint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CameraAlt, null, tint = Brand.Mint, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Camera access needed", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "CalorieLens uses your camera to recognise food. Photos never leave your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            GradientButton(
                if (permanentlyDenied) "Open settings" else "Allow camera",
                onRequest,
                icon = if (permanentlyDenied) Icons.Rounded.Settings else Icons.Rounded.CameraAlt,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlineButton("Choose from gallery", onGallery, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoLibrary)
        }
    }
}
