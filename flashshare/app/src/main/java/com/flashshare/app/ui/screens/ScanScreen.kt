package com.flashshare.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.flashshare.app.container
import com.flashshare.app.net.QrPayload
import com.flashshare.app.transfer.Mode
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.AvatarImpl
import com.flashshare.app.ui.components.GradientButton
import com.flashshare.app.util.toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** mode: "send" – scan a receiver; "join" – scan a group sender's code. */
@Composable
fun ScanScreen(mode: String, nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var pinFor by remember { mutableStateOf<QrPayload?>(null) }
    val handled = remember { AtomicBoolean(false) }

    fun connect(p: QrPayload, pin: String?) {
        val items = if (mode == "join") emptyList() else c.selection.list
        c.transfers.join(p, pin, if (mode == "join") Mode.GROUP_JOIN else Mode.SEND, items)
        if (mode != "join") c.selection.clear()
        nav.navigate(Routes.TRANSFER) { popUpTo(Routes.HOME) }
    }

    fun onCode(text: String) {
        val p = QrPayload.decode(text)
        when {
            p == null -> { ctx.toast("That's not a FlashShare code"); handled.set(false) }
            mode == "send" && p.isGroup -> { ctx.toast("That's a group code. Use Receive › Join a group on this phone."); handled.set(false) }
            mode == "join" && !p.isGroup -> { ctx.toast("That phone is waiting to receive. Use Send on this phone instead."); handled.set(false) }
            p.encrypted -> pinFor = p
            else -> connect(p, null)
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { decodeFromImage(ctx, uri) }
            if (text == null) ctx.toast("No QR code found in that picture") else onCode(text)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(onCamera = { camera = it }) { text ->
            if (handled.compareAndSet(false, true)) scope.launch { onCode(text) }
        }
        ScanOverlay()
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
            Text(if (mode == "join") "Join a group" else "Scan to send", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Rounded.Image, "Scan from photo", tint = Color.White)
            }
            IconButton(onClick = { torch = !torch; camera?.cameraControl?.enableTorch(torch) }) {
                Icon(if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff, "Torch", tint = Color.White)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(20.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MeAvatar(36.dp)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.East, null, tint = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.QrCode2, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (mode == "join") "Scan the code on the sender's phone (Group Share)" else "On the other phone tap Receive, then scan its QR code",
                color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            )
            if (mode != "join") {
                val n = c.selection.list.size
                if (n > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("$n file${if (n == 1) "" else "s"} ready to send", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    pinFor?.let { p ->
        PinDialog(
            peerName = p.name, avatarId = p.avatarId,
            onDismiss = { pinFor = null; handled.set(false) },
            onPin = { pin -> pinFor = null; connect(p, pin) },
        )
    }
}

@Composable
fun PinDialog(peerName: String, avatarId: Int, error: String? = null, onDismiss: () -> Unit, onPin: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AvatarImpl(avatarId, null, 52.dp) },
        title = { Text("Enter pairing PIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$peerName uses encrypted sharing. Enter the 6-digit PIN shown on their screen.", textAlign = TextAlign.Center)
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) pin = v },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp, fontWeight = FontWeight.Bold),
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        confirmButton = { GradientButton("Connect", onClick = { onPin(pin) }, enabled = pin.length == 6, height = 46.dp) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CameraPreview(onCamera: (Camera) -> Unit, onCode: (String) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val reader = qrReader()
            analysis.setAnalyzer(executor) { img -> decodeFrame(reader, img)?.let(onCode) }
            runCatching {
                provider.unbindAll()
                onCamera(provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis))
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}

private fun qrReader() = MultiFormatReader().apply {
    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true))
}

private fun decodeFrame(reader: MultiFormatReader, img: ImageProxy): String? {
    try {
        val plane = img.planes[0]
        val w = img.width; val h = img.height
        val rowStride = plane.rowStride
        val buf = plane.buffer
        val data = ByteArray(w * h)
        if (rowStride == w) {
            buf.get(data, 0, minOf(data.size, buf.remaining()))
        } else {
            for (row in 0 until h) {
                buf.position(row * rowStride)
                buf.get(data, row * w, minOf(w, buf.remaining()))
            }
        }
        val source = PlanarYUVLuminanceSource(data, w, h, 0, 0, w, h, false)
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (e: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    } catch (e: Exception) {
        return null
    } finally {
        img.close()
    }
}

private fun decodeFromImage(ctx: Context, uri: Uri): String? = runCatching {
    val bmp: Bitmap = loadScaled(ctx, uri, 1600) ?: return null
    val px = IntArray(bmp.width * bmp.height)
    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    val src = RGBLuminanceSource(bmp.width, bmp.height, px)
    qrReader().decodeWithState(BinaryBitmap(HybridBinarizer(src))).text
}.getOrNull()

@Composable
private fun ScanOverlay() {
    val t = rememberInfiniteTransition(label = "scan")
    val line by t.animateFloat(0.05f, 0.95f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "line")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    val accent = Color(0xFF4D8BFF)
    Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val side = size.minDimension * 0.7f
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2 - 40.dp.toPx()
        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(Color.Transparent, Offset(left, top), GSize(side, side), CornerRadius(28.dp.toPx()), blendMode = BlendMode.Clear)
        val l = 44.dp.toPx(); val sw = 6.dp.toPx(); val r = 28.dp.toPx()
        val col = accent.copy(alpha = 0.75f + 0.25f * pulse)
        // corner brackets
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(col, Offset(x + dx * r * 0.3f, y), Offset(x + dx * l, y), sw, StrokeCap.Round)
            drawLine(col, Offset(x, y + dy * r * 0.3f), Offset(x, y + dy * l), sw, StrokeCap.Round)
        }
        corner(left, top, 1f, 1f); corner(left + side, top, -1f, 1f)
        corner(left, top + side, 1f, -1f); corner(left + side, top + side, -1f, -1f)
        val y = top + side * line
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.35f)), startY = y - 60.dp.toPx(), endY = y),
            Offset(left + 12.dp.toPx(), y - 60.dp.toPx()), GSize(side - 24.dp.toPx(), 60.dp.toPx()),
        )
        drawLine(accent, Offset(left + 16.dp.toPx(), y), Offset(left + side - 16.dp.toPx(), y), 3.dp.toPx(), StrokeCap.Round)
    }
}

/** Decodes an image without cropping, longest side limited to [maxSide]. */
fun loadScaled(ctx: Context, uri: Uri, maxSide: Int): Bitmap? = runCatching {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    ctx.contentResolver.openInputStream(uri)?.use {
        android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
    }
}.getOrNull()
