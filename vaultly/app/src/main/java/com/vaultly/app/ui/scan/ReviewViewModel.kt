package com.vaultly.app.ui.scan

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultly.app.VaultlyApp
import com.vaultly.app.scan.ImageOps
import com.vaultly.app.scan.Pt
import com.vaultly.app.scan.Quad
import com.vaultly.app.scan.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ReviewStep { CROP, ENHANCE }

data class ReviewState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val source: ImageBitmap? = null,
    val quad: Quad = Quad.FULL,
    val detected: Boolean = false,
    val step: ReviewStep = ReviewStep.CROP,
    val filter: ScanFilter = ScanFilter.AUTO,
    val previews: Map<ScanFilter, ImageBitmap> = emptyMap(),
    val thumbs: Map<ScanFilter, ImageBitmap> = emptyMap(),
    val processing: Boolean = false,
    val saving: Boolean = false,
)

class ReviewViewModel(private val app: VaultlyApp, private val src: String, private val hint: Quad?) : ViewModel() {
    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    private var source: Bitmap? = null
    private var warped: Bitmap? = null
    private var rotation = 0
    private var previewJob: Job? = null

    private val uri: Uri = if (src.startsWith("/")) Uri.fromFile(File(src)) else Uri.parse(src)

    init {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.Default) { ImageOps.decode(app, uri) }
            if (bmp == null) {
                _state.update { it.copy(loading = false, failed = true) }
                return@launch
            }
            source = bmp
            val detected = withContext(Dispatchers.Default) { ImageOps.detect(bmp) }
            _state.update {
                it.copy(
                    loading = false,
                    source = bmp.asImageBitmap(),
                    quad = detected ?: hint?.takeIf { q -> q.isConvex() } ?: Quad.FULL,
                    detected = detected != null || hint != null,
                )
            }
        }
    }

    fun moveCorner(index: Int, p: Pt) {
        val clamped = Pt(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f))
        val next = _state.value.quad.withCorner(index, clamped)
        if (next.isConvex() && next.area() > 0.01f) _state.update { it.copy(quad = next) }
    }

    fun autoDetect() {
        val bmp = source ?: return
        viewModelScope.launch {
            val q = withContext(Dispatchers.Default) { ImageOps.detect(bmp) }
            if (q != null) {
                _state.update { it.copy(quad = q, detected = true) }
                app.notifier.success("Edges detected")
            } else {
                app.notifier.show("Couldn't find clear edges. Drag the corners to fit.")
            }
        }
    }

    fun fullPage() = _state.update { it.copy(quad = Quad.FULL) }

    fun toEnhance() {
        val bmp = source ?: return
        _state.update { it.copy(step = ReviewStep.ENHANCE, processing = true, previews = emptyMap(), thumbs = emptyMap()) }
        viewModelScope.launch {
            warped = withContext(Dispatchers.Default) { ImageOps.warp(bmp, _state.value.quad) }
            buildPreviews()
        }
    }

    fun backToCrop() {
        previewJob?.cancel()
        _state.update { it.copy(step = ReviewStep.CROP, processing = false) }
    }

    fun setFilter(f: ScanFilter) = _state.update { it.copy(filter = f) }

    fun rotate() {
        rotation = (rotation + 90) % 360
        buildPreviews()
    }

    private fun buildPreviews() {
        val base = warped ?: return
        previewJob?.cancel()
        _state.update { it.copy(processing = true) }
        previewJob = viewModelScope.launch {
            val preview = withContext(Dispatchers.Default) { ImageOps.rotate(ImageOps.scaleDown(base, 1400), rotation) }
            // Show the selected look first, then fill in the rest.
            val order = listOf(_state.value.filter) + ScanFilter.entries.filter { it != _state.value.filter }
            for (f in order) {
                val img = withContext(Dispatchers.Default) { ImageOps.applyFilter(preview, f) }
                val thumb = withContext(Dispatchers.Default) { ImageOps.scaleDown(img, 220) }
                _state.update {
                    it.copy(
                        previews = it.previews + (f to img.asImageBitmap()),
                        thumbs = it.thumbs + (f to thumb.asImageBitmap()),
                        processing = false,
                    )
                }
            }
        }
    }

    /** Applies the chosen filter at full resolution, saves to the vault and returns the file path. */
    fun finish(onDone: (String) -> Unit) {
        val base = warped ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val out = withContext(Dispatchers.Default) { ImageOps.applyFilter(ImageOps.rotate(base, rotation), _state.value.filter) }
            val path = app.images.save(out)
            cleanupSource()
            _state.update { it.copy(saving = false) }
            if (path != null) onDone(path) else app.notifier.error("Couldn't save the scan. Please try again.")
        }
    }

    /** Deletes the temporary camera photo. */
    fun cleanupSource() {
        if (src.startsWith("/") && src.startsWith(File(app.cacheDir, "scans").absolutePath)) runCatching { File(src).delete() }
    }
}
