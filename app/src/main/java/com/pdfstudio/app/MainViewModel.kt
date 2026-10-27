package com.pdfstudio.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfstudio.app.data.CropRect
import com.pdfstudio.app.data.MediaSaver
import com.pdfstudio.app.data.PageBitmapCache
import com.pdfstudio.app.data.PageSize
import com.pdfstudio.app.data.PdfOps
import com.pdfstudio.app.data.PdfPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

enum class ImageFormat(val label: String, val extension: String, val mime: String, val compress: Bitmap.CompressFormat) {
    JPG("JPG", "jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    PNG("PNG", "png", "image/png", Bitmap.CompressFormat.PNG),
}

class DocState(
    val id: Int,
    val file: File,
    val name: String,
    val renderer: PdfPageRenderer,
    val pageSizes: List<PageSize>,
    val focusPage: Int,
) {
    val pageCount: Int get() = pageSizes.size
}

enum class BusyPhase { Working, Done, Failed }

data class Busy(
    val title: String,
    val progress: Float? = null,
    val detail: String? = null,
    val phase: BusyPhase = BusyPhase.Working,
)

data class UiState(
    val doc: DocState? = null,
    val busy: Busy? = null,
    val format: ImageFormat = ImageFormat.JPG,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    val cache = PageBitmapCache()

    private val workDir = File(app.cacheDir, "work").apply {
        deleteRecursively()
        mkdirs()
    }
    private var nextId = 0

    fun setFormat(format: ImageFormat) = _state.update { it.copy(format = format) }

    fun open(uri: Uri) {
        if (_state.value.busy != null) return
        viewModelScope.launch {
            runQuick("Opening PDF") {
                val context = getApplication<Application>()
                val (name, file) = withContext(Dispatchers.IO) {
                    val name = queryName(uri)
                    val file = newWorkFile()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { input.copyTo(it, 256 * 1024) }
                    } ?: error("Unable to read file")
                    name to file
                }
                try {
                    showDocument(file, name, focusPage = 0)
                } catch (e: SecurityException) {
                    throw IllegalStateException("This PDF is password-protected")
                } catch (e: Throwable) {
                    throw IllegalStateException("This file isn't a valid PDF")
                }
            }
        }
    }

    fun split(pages: List<Int>) {
        val doc = _state.value.doc ?: return
        if (pages.isEmpty() || _state.value.busy != null) return
        viewModelScope.launch {
            runQuick("Splitting PDF") {
                val out = withContext(Dispatchers.IO) {
                    newWorkFile().also { PdfOps.split(doc.file, pages, it) }
                }
                showDocument(out, "${baseName(doc.name)}_split", focusPage = 0)
            }
        }
    }

    fun crop(page: Int, rect: CropRect) {
        val doc = _state.value.doc ?: return
        if (rect.isFullPage || _state.value.busy != null) return
        viewModelScope.launch {
            runQuick("Cropping page ${page + 1}") {
                val out = withContext(Dispatchers.IO) {
                    newWorkFile().also { PdfOps.crop(doc.file, page, rect, it) }
                }
                showDocument(out, "${baseName(doc.name)}_cropped", focusPage = page)
            }
        }
    }

    /**
     * Runs a short operation. The progress dialog only appears if it takes long enough to
     * notice, so fast operations feel instant; failures are shown in the same dialog.
     */
    private suspend fun runQuick(title: String, block: suspend () -> Unit) = coroutineScope {
        val reveal = launch {
            delay(280)
            setBusy(Busy(title))
        }
        try {
            block()
            reveal.cancel()
            setBusy(null)
        } catch (e: Throwable) {
            reveal.cancel()
            setBusy(Busy("Something went wrong", detail = e.message ?: "Please try again", phase = BusyPhase.Failed))
            delay(1800)
            setBusy(null)
        }
    }

    fun convert() {
        val doc = _state.value.doc ?: return
        if (_state.value.busy != null) return
        val format = _state.value.format
        val total = doc.pageCount
        val title = "Converting to ${format.label}"
        viewModelScope.launch {
            setBusy(Busy(title, 0f, "Page 1 of $total"))
            val context = getApplication<Application>()
            val base = baseName(doc.name)
            val digits = total.toString().length
            try {
                withContext(Dispatchers.IO) {
                    val renderer = PdfPageRenderer(doc.file)
                    try {
                        // Render on the renderer thread while two workers encode and save in parallel.
                        val rendered = Channel<Pair<Int, Bitmap>>(capacity = 1)
                        val done = AtomicInteger(0)
                        coroutineScope {
                            launch {
                                for (i in 0 until total) {
                                    val bitmap = renderer.renderAtDpi(i, EXPORT_DPI, MAX_EXPORT_PIXELS) ?: break
                                    rendered.send(i to bitmap)
                                }
                                rendered.close()
                            }
                            repeat(ENCODERS) {
                                launch(Dispatchers.Default) {
                                    for ((i, bitmap) in rendered) {
                                        val bytes = ByteArrayOutputStream(1 shl 20).use { stream ->
                                            bitmap.compress(format.compress, 100, stream)
                                            stream.toByteArray()
                                        }
                                        bitmap.recycle()
                                        val fileName = "${base}_page_${(i + 1).toString().padStart(digits, '0')}.${format.extension}"
                                        withContext(Dispatchers.IO) { MediaSaver.saveImage(context, bytes, fileName, format.mime) }
                                        val n = done.incrementAndGet()
                                        setBusy(Busy(title, n / total.toFloat(), "Page ${minOf(n + 1, total)} of $total"))
                                    }
                                }
                            }
                        }
                    } finally {
                        withContext(NonCancellable) { renderer.close() }
                    }
                }
                setBusy(Busy("Export complete", 1f, "$total ${format.label} image${if (total == 1) "" else "s"}", BusyPhase.Done))
                delay(1300)
            } catch (e: Throwable) {
                setBusy(Busy("Export failed", detail = e.message ?: "Please try again", phase = BusyPhase.Failed))
                delay(1800)
            } finally {
                setBusy(null)
            }
        }
    }

    private suspend fun showDocument(file: File, name: String, focusPage: Int) {
        val renderer = withContext(Dispatchers.IO) { PdfPageRenderer(file) }
        val sizes = try {
            renderer.pageSizes()
        } catch (t: Throwable) {
            renderer.close()
            file.delete()
            throw t
        }
        if (sizes.isEmpty()) {
            renderer.close()
            file.delete()
            error("Empty document")
        }
        val id = ++nextId
        val focus = focusPage.coerceIn(0, sizes.lastIndex)
        // Pre-render the first visible pages so the preview appears fully drawn.
        val metrics = getApplication<Application>().resources.displayMetrics
        val previewWidth = metrics.widthPixels - (32 * metrics.density).roundToInt()
        if (previewWidth > 0) {
            for (page in focus..minOf(focus + 1, sizes.lastIndex)) {
                renderer.render(page, previewWidth)?.let { cache.put(id, page, previewWidth, it) }
            }
        }
        val previous = _state.value.doc
        _state.update { it.copy(doc = DocState(id, file, name, renderer, sizes, focus)) }
        if (previous != null) {
            viewModelScope.launch {
                previous.renderer.close()
                cache.evictDocument(previous.id)
                withContext(Dispatchers.IO) { previous.file.delete() }
            }
        }
    }

    private fun setBusy(busy: Busy?) = _state.update { it.copy(busy = busy) }

    private fun newWorkFile() = File(workDir, "doc_${System.nanoTime()}.pdf")

    private fun queryName(uri: Uri): String {
        val raw = runCatching {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment ?: "document"
        val name = raw.substringAfterLast('/').removeSuffix(".pdf").removeSuffix(".PDF")
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "document" }
    }

    private fun baseName(name: String) = name.removeSuffix("_split").removeSuffix("_cropped")

    override fun onCleared() {
        val doc = _state.value.doc ?: return
        // viewModelScope is already cancelled here; close on a detached scope.
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { doc.renderer.close() }
    }

    private companion object {
        const val EXPORT_DPI = 300
        const val MAX_EXPORT_PIXELS = 16_000_000L
        const val ENCODERS = 2
    }
}
