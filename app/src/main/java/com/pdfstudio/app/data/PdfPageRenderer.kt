package com.pdfstudio.app.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Page size in PDF points (1/72 inch), already rotated as displayed. */
data class PageSize(val width: Int, val height: Int) {
    val aspect: Float get() = if (height == 0) 1f else width.toFloat() / height
}

/**
 * Thread-confined wrapper around [PdfRenderer]. All native calls run on a dedicated
 * thread, so callers can render from any coroutine and cancelled requests never start.
 */
class PdfPageRenderer(file: File) {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "pdf-render").apply { priority = Thread.NORM_PRIORITY + 1 } }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val pfd: ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer = try {
        PdfRenderer(pfd)
    } catch (t: Throwable) {
        pfd.close()
        executor.shutdown()
        throw t
    }

    @Volatile
    private var closed = false

    val pageCount: Int = renderer.pageCount

    suspend fun pageSizes(): List<PageSize> = withContext(dispatcher) {
        List(pageCount) { i -> renderer.openPage(i).use { PageSize(it.width, it.height) } }
    }

    /** Renders [index] scaled to [targetWidth] pixels wide. Returns null once closed. */
    suspend fun render(index: Int, targetWidth: Int): Bitmap? = withContext(dispatcher) {
        if (closed) return@withContext null
        renderer.openPage(index).use { page ->
            val w = targetWidth.coerceAtLeast(1)
            val h = (w * page.height.toFloat() / page.width).roundToInt().coerceAtLeast(1)
            draw(page, w, h)
        }
    }

    /** Renders [index] at [dpi] for export, limited to [maxPixels] to stay within memory. */
    suspend fun renderAtDpi(index: Int, dpi: Int, maxPixels: Long): Bitmap? = withContext(dispatcher) {
        if (closed) return@withContext null
        renderer.openPage(index).use { page ->
            var scale = dpi / 72f
            val pixels = page.width * scale * page.height * scale
            if (pixels > maxPixels) scale *= sqrt(maxPixels / pixels)
            val w = (page.width * scale).roundToInt().coerceAtLeast(1)
            val h = (page.height * scale).roundToInt().coerceAtLeast(1)
            try {
                draw(page, w, h)
            } catch (oom: OutOfMemoryError) {
                draw(page, (w * 0.6f).roundToInt(), (h * 0.6f).roundToInt())
            }
        }
    }

    private fun draw(page: PdfRenderer.Page, w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    suspend fun close() {
        if (closed) return
        closed = true
        withContext(dispatcher) {
            runCatching { renderer.close() }
            runCatching { pfd.close() }
        }
        dispatcher.close()
    }
}
