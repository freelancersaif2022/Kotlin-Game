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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

data class Busy(val title: String, val progress: Float? = null, val detail: String? = null)

data class UiState(
    val doc: DocState? = null,
    val busy: Busy? = null,
    val format: ImageFormat = ImageFormat.JPG,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    val cache = PageBitmapCache()

    private val workDir = File(app.cacheDir, "work").apply {
        deleteRecursively()
        mkdirs()
    }
    private var nextId = 0

    fun toast(message: String) {
        _messages.tryEmit(message)
    }

    fun setFormat(format: ImageFormat) = _state.update { it.copy(format = format) }

    fun open(uri: Uri) {
        if (_state.value.busy != null) return
        viewModelScope.launch {
            setBusy(Busy("Opening PDF"))
            try {
                val context = getApplication<Application>()
                val (name, file) = withContext(Dispatchers.IO) {
                    val name = queryName(uri)
                    val file = newWorkFile()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { input.copyTo(it, 256 * 1024) }
                    } ?: error("Unable to read file")
                    name to file
                }
                showDocument(file, name, focusPage = 0)
            } catch (e: SecurityException) {
                toast("This PDF is password-protected")
            } catch (e: Throwable) {
                toast("Couldn't open this file. Make sure it's a valid PDF.")
            } finally {
                setBusy(null)
            }
        }
    }

    fun split(pages: List<Int>) {
        val doc = _state.value.doc ?: return
        if (pages.isEmpty() || _state.value.busy != null) return
        viewModelScope.launch {
            setBusy(Busy("Splitting PDF"))
            try {
                val name = "${baseName(doc.name)}_split"
                val out = withContext(Dispatchers.IO) {
                    val out = newWorkFile()
                    PdfOps.split(doc.file, pages, out)
                    MediaSaver.savePdf(getApplication(), out, "${name}_${stamp()}.pdf")
                    out
                }
                showDocument(out, name, focusPage = 0)
                toast("${pages.size} page${if (pages.size == 1) "" else "s"} split · Saved to Download/${MediaSaver.FOLDER}")
            } catch (e: Throwable) {
                toast("Split failed: ${e.message ?: "unknown error"}")
            } finally {
                setBusy(null)
            }
        }
    }

    fun crop(page: Int, rect: CropRect) {
        val doc = _state.value.doc ?: return
        if (rect.isFullPage || _state.value.busy != null) return
        viewModelScope.launch {
            setBusy(Busy("Cropping page ${page + 1}"))
            try {
                val name = "${baseName(doc.name)}_cropped"
                val out = withContext(Dispatchers.IO) {
                    val out = newWorkFile()
                    PdfOps.crop(doc.file, page, rect, out)
                    MediaSaver.savePdf(getApplication(), out, "${name}_${stamp()}.pdf")
                    out
                }
                showDocument(out, name, focusPage = page)
                toast("Page ${page + 1} cropped · Saved to Download/${MediaSaver.FOLDER}")
            } catch (e: Throwable) {
                toast("Crop failed: ${e.message ?: "unknown error"}")
            } finally {
                setBusy(null)
            }
        }
    }

    fun convert() {
        val doc = _state.value.doc ?: return
        if (_state.value.busy != null) return
        val format = _state.value.format
        val total = doc.pageCount
        viewModelScope.launch {
            setBusy(Busy("Converting to ${format.label}", 0f, "0 of $total"))
            val context = getApplication<Application>()
            val base = baseName(doc.name)
            val digits = total.toString().length
            try {
                withContext(Dispatchers.IO) {
                    val renderer = PdfPageRenderer(doc.file)
                    try {
                        // Render the next page while the previous one is being encoded and saved.
                        val rendered = Channel<Pair<Int, Bitmap>>(Channel.RENDEZVOUS)
                        coroutineScope {
                            launch {
                                for (i in 0 until total) {
                                    val bitmap = renderer.renderAtDpi(i, EXPORT_DPI, MAX_EXPORT_PIXELS) ?: break
                                    rendered.send(i to bitmap)
                                }
                                rendered.close()
                            }
                            for ((i, bitmap) in rendered) {
                                val bytes = ByteArrayOutputStream(1 shl 20).use { stream ->
                                    bitmap.compress(format.compress, 100, stream)
                                    stream.toByteArray()
                                }
                                bitmap.recycle()
                                val fileName = "${base}_page_${(i + 1).toString().padStart(digits, '0')}.${format.extension}"
                                MediaSaver.saveImage(context, bytes, fileName, format.mime)
                                val done = i + 1
                                setBusy(Busy("Converting to ${format.label}", done / total.toFloat(), "$done of $total"))
                            }
                        }
                    } finally {
                        withContext(NonCancellable) { renderer.close() }
                    }
                }
                toast("$total image${if (total == 1) "" else "s"} saved to Gallery & Download/${MediaSaver.FOLDER}")
            } catch (e: Throwable) {
                toast("Convert failed: ${e.message ?: "unknown error"}")
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
        val previous = _state.value.doc
        _state.update {
            it.copy(doc = DocState(++nextId, file, name, renderer, sizes, focusPage.coerceIn(0, sizes.lastIndex)))
        }
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

    private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    override fun onCleared() {
        val doc = _state.value.doc ?: return
        // viewModelScope is already cancelled here; close on a detached scope.
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { doc.renderer.close() }
    }

    private companion object {
        const val EXPORT_DPI = 300
        const val MAX_EXPORT_PIXELS = 16_000_000L
    }
}
