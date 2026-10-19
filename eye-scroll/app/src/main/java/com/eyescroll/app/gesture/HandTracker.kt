package com.eyescroll.app.gesture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.hypot

/**
 * Front camera (CameraX) -> MediaPipe Hand Landmarker (21 points per hand) -> [HandFrame].
 * Runs on-device on one background thread; frames are never stored.
 */
class HandTracker(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    targetFps: Int,
    private val listener: Listener,
) {
    interface Listener {
        /** Called on the tracker's background thread. */
        fun onFrame(frame: HandFrame)
        fun onError(message: String)
    }

    @Volatile
    var targetFps: Int = targetFps.coerceIn(5, 30)
        set(value) {
            field = value.coerceIn(5, 30)
        }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var landmarker: HandLandmarker? = null
    @Volatile private var closed = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var lastSubmitMs = 0L
    private var frameCounter = 0
    @Volatile private var lastBrightness = -1f

    fun start(previewSurface: Preview.SurfaceProvider? = null, rotation: Int? = null) {
        executor.execute { createLandmarker() }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (closed) return@addListener
            try {
                val provider = future.get()
                cameraProvider = provider
                bind(provider, previewSurface, rotation)
            } catch (e: Exception) {
                listener.onError("Camera unavailable: ${e.message ?: e.javaClass.simpleName}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Keeps the analysed image upright when the screen rotates. */
    fun setRotation(rotation: Int) {
        analysis?.targetRotation = rotation
    }

    private fun createLandmarker() {
        if (closed) return
        try {
            val base = BaseOptions.builder()
                .setModelAssetPath(MODEL)
                .setDelegate(Delegate.CPU)
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result: HandLandmarkerResult, input: MPImage -> onResult(result, input) }
                .setErrorListener { e: RuntimeException -> listener.onError("Hand tracking error: ${e.message}") }
                .build()
            landmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Throwable) {
            val root = generateSequence(e) { it.cause }.last()
            val abis = Build.SUPPORTED_ABIS.joinToString()
            listener.onError("Could not load hand model: ${root.javaClass.simpleName} ${root.message ?: ""} [CPU: $abis]")
        }
    }

    private fun bind(provider: ProcessCameraProvider, previewSurface: Preview.SurfaceProvider?, rotation: Int?) {
        val selector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
            )
            .build()
        val builder = ImageAnalysis.Builder()
            .setResolutionSelector(selector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        if (rotation != null) builder.setTargetRotation(rotation)
        val ia = builder.build()
        ia.setAnalyzer(executor) { image -> analyze(image) }
        analysis = ia

        val useCases = mutableListOf<UseCase>(ia)
        if (previewSurface != null) {
            val preview = Preview.Builder().setResolutionSelector(selector).build()
            preview.setSurfaceProvider(previewSurface)
            useCases += preview
        }
        if (!provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            listener.onError("This phone has no front camera")
            return
        }
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, *useCases.toTypedArray())
    }

    private fun analyze(image: ImageProxy) {
        val lm = landmarker
        val now = SystemClock.uptimeMillis()
        if (closed || lm == null || now - lastSubmitMs < 1000L / targetFps) {
            image.close()
            return
        }
        lastSubmitMs = now
        val bitmap: Bitmap
        try {
            val raw = image.toBitmap()
            val rotation = image.imageInfo.rotationDegrees
            bitmap = if (rotation != 0) {
                val m = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, false)
            } else {
                raw
            }
        } catch (e: Exception) {
            return
        } finally {
            image.close()
        }
        if (frameCounter++ % 12 == 0) lastBrightness = measureBrightness(bitmap)
        try {
            lm.detectAsync(BitmapImageBuilder(bitmap).build(), now)
        } catch (_: Exception) {
            // Landmarker busy or closing: drop this frame.
        }
    }

    private fun onResult(result: HandLandmarkerResult, input: MPImage) {
        if (closed) return
        val ts = result.timestampMs()
        val hands = result.landmarks()
        if (hands.isEmpty() || hands[0].size < 21) {
            listener.onFrame(HandFrame.none(ts, lastBrightness))
            return
        }
        val lm = hands[0]
        val w = input.width.toFloat()
        val h = input.height.toFloat()
        // Hand size in "image height" units: wrist (0) to middle-finger base (9).
        val size = hypot((lm[0].x() - lm[9].x()) * w, (lm[0].y() - lm[9].y()) * h) / h
        val tip = lm[INDEX_TIP]
        listener.onFrame(HandFrame(ts, true, tip.x(), tip.y(), size, lastBrightness))
    }

    private fun measureBrightness(b: Bitmap): Float {
        val small = Bitmap.createScaledBitmap(b, 16, 12, false)
        val px = IntArray(16 * 12)
        small.getPixels(px, 0, 16, 0, 0, 16, 12)
        if (small !== b) small.recycle()
        var sum = 0.0
        for (p in px) sum += 0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)
        return (sum / px.size / 255.0).toFloat()
    }

    /** Must be called on the main thread. */
    fun close() {
        if (closed) return
        closed = true
        try {
            analysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
        executor.execute {
            try {
                landmarker?.close()
            } catch (_: Exception) {
            }
            landmarker = null
        }
        executor.shutdown()
    }

    companion object {
        const val MODEL = "hand_landmarker.task"
        private const val INDEX_TIP = 8
    }
}
