package com.vaultly.app.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the live camera currently sees. [quad] is in upright, normalized preview coordinates. */
data class LiveState(
    val quad: Quad? = null,
    val frameW: Int = 3,
    val frameH: Int = 4,
    /** 0..1 – how close auto-capture is to firing (the document must hold still). */
    val progress: Float = 0f,
)

/**
 * Camera analyzer for auto-capture: finds the document in each low-res frame, smooths the
 * corners, and requests a capture once the document has been held still for a moment.
 */
class LiveDetector : ImageAnalysis.Analyzer {
    @Volatile var auto: Boolean = true
    @Volatile var paused: Boolean = false

    private val _state = MutableStateFlow(LiveState())
    val state: StateFlow<LiveState> = _state.asStateFlow()

    private val _capture = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val captureRequests: SharedFlow<Unit> = _capture.asSharedFlow()

    private var smooth: Quad? = null
    private var anchor: Quad? = null
    private var stable = 0
    private var misses = 0

    override fun analyze(image: ImageProxy) {
        try {
            val rot = image.imageInfo.rotationDegrees
            val rw = if (rot % 180 == 0) image.width else image.height
            val rh = if (rot % 180 == 0) image.height else image.width
            if (!auto || paused) {
                reset()
                _state.value = LiveState(frameW = rw, frameH = rh)
                return
            }
            val found = detectLuma(image)?.rotated(rot)
            if (found == null) {
                if (++misses > 5) {
                    reset()
                    _state.value = LiveState(frameW = rw, frameH = rh)
                } else {
                    _state.value = _state.value.copy(frameW = rw, frameH = rh)
                }
                return
            }
            misses = 0
            val s = smooth?.lerp(found, 0.55f) ?: found
            smooth = s
            val a = anchor
            if (a != null && s.distanceTo(a) < STILL_TOLERANCE) stable++ else { anchor = s; stable = 0 }
            val progress = (stable / FRAMES_TO_CAPTURE.toFloat()).coerceAtMost(1f)
            _state.value = LiveState(s, rw, rh, progress)
            if (stable >= FRAMES_TO_CAPTURE) {
                paused = true
                _capture.tryEmit(Unit)
            }
        } finally {
            image.close()
        }
    }

    fun resume() {
        reset()
        paused = false
    }

    private fun reset() {
        smooth = null
        anchor = null
        stable = 0
        misses = 0
    }

    private fun detectLuma(image: ImageProxy): Quad? {
        val plane = image.planes[0]
        val buf = plane.buffer
        val rs = plane.rowStride
        val ps = plane.pixelStride
        val w = image.width
        val h = image.height
        val tw = 160
        val th = (h * tw / w).coerceAtLeast(16)
        val lum = IntArray(tw * th)
        for (y in 0 until th) {
            val row = (y * h / th) * rs
            for (x in 0 until tw) {
                lum[y * tw + x] = buf.get(row + (x * w / tw) * ps).toInt() and 0xFF
            }
        }
        return QuadDetector.detect(lum, tw, th)
    }

    companion object {
        private const val FRAMES_TO_CAPTURE = 14
        private const val STILL_TOLERANCE = 0.022f
    }
}
