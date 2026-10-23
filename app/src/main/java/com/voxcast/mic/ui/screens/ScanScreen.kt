package com.voxcast.mic.ui.screens

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.voxcast.mic.net.ConnectionInfo
import com.voxcast.mic.ui.components.IconBadge
import com.voxcast.mic.ui.components.Messenger
import com.voxcast.mic.ui.components.RoundIconButton
import com.voxcast.mic.ui.components.ScannerOverlay
import com.voxcast.mic.ui.components.bounceClick
import com.voxcast.mic.ui.components.glass
import com.voxcast.mic.ui.components.scanWindow
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScanScreen(onBack: () -> Unit, onScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = rememberHaptic()
    var detected by remember { mutableStateOf<String?>(null) }
    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    val scanned by rememberUpdatedState(onScanned)

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        var lastInvalidAt = 0L
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = runCatching { providerFuture.get() }.getOrNull() ?: return@addListener
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || detected != null) {
                    proxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { codes ->
                        if (detected != null) return@addOnSuccessListener
                        val raw = codes.firstNotNullOfOrNull { it.rawValue } ?: return@addOnSuccessListener
                        if (ConnectionInfo.parse(raw) != null) {
                            detected = raw
                        } else {
                            val now = System.currentTimeMillis()
                            if (now - lastInvalidAt > 3_000) {
                                lastInvalidAt = now
                                Messenger.warning("Not a VoxCast code", "Scan the QR shown on the Speaker screen")
                            }
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.onFailure { Messenger.error("Camera unavailable", it.message) }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(torch, camera) { camera?.cameraControl?.enableTorch(torch) }

    LaunchedEffect(detected) {
        val raw = detected ?: return@LaunchedEffect
        haptic(HapticFeedbackType.LongPress)
        delay(650) // let the lock-on animation play
        scanned(raw)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        ScannerOverlay(detected = detected != null)

        // Lock-on check badge in the middle of the window.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val window = with(density) { scanWindow(GeoSize(maxWidth.toPx(), maxHeight.toPx())) }
            val centerY = with(density) { window.center.y.toDp() }
            val bottomY = with(density) { window.bottom.toDp() }
            AnimatedVisibility(
                visible = detected != null,
                enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 400f), initialScale = 0.2f) + fadeIn(),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = centerY - 40.dp),
            ) {
                IconBadge(Icons.Rounded.Check, VoxColors.SuccessGradient, size = 80.dp, glow = VoxColors.Success, iconTint = VoxColors.Background)
            }
            Text(
                if (detected != null) "Speaker found" else "Align the QR code inside the frame",
                style = MaterialTheme.typography.titleMedium,
                color = if (detected != null) VoxColors.Success else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = bottomY + 24.dp).fillMaxWidth(),
            )
        }

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack, background = Color.Black.copy(alpha = 0.4f))
            Spacer(Modifier.weight(1f))
            Text("Scan speaker code", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.weight(1f))
            RoundIconButton(
                if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                "Flashlight",
                { torch = !torch },
                background = Color.Black.copy(alpha = 0.4f),
                active = torch,
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .glass(RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(VoxIcons.QrScan, VoxColors.PrimaryGradient, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Point at the speaker phone", style = MaterialTheme.typography.titleSmall, color = VoxColors.Text)
                    Text(
                        "On the other phone choose Speaker to show its code. Both phones must share Wi-Fi, a hotspot, or Wi-Fi Direct.",
                        style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim,
                    )
                }
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, VoxColors.OutlineSoft, RoundedCornerShape(12.dp))
                    .bounceClick { showGuide = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.HelpOutline, null, tint = VoxColors.Cyan, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Connection guide",
                    style = MaterialTheme.typography.labelLarge, color = VoxColors.Cyan,
                )
            }
        }
    }

    if (showGuide) NetworkGuideSheet(onDismiss = { showGuide = false })
}
