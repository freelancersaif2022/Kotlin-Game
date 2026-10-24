package com.pdfmaster.app.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.roundToInt

/**
 * Wraps the platform [PdfRenderer] for one open document. PdfRenderer is not thread-safe,
 * so every call is confined to a single-threaded dispatcher; rendered bitmaps are cached.
 */
class PdfDocumentSession private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    val pageSizes: List<Size>,
) : Closeable {

    val pageCount: Int get() = pageSizes.size

    private val cache = object : LruCache<String, Bitmap>(cacheBytes()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    @Volatile
    private var closed = false

    suspend fun render(index: Int, targetWidth: Int): Bitmap? {
        if (index !in pageSizes.indices || targetWidth <= 0) return null
        // Bucket widths so small zoom changes reuse the cached bitmap.
        val bucket = ((targetWidth + 127) / 128) * 128
        val key = "$index:$bucket"
        cache.get(key)?.let { return it }
        return withContext(renderDispatcher) {
            if (closed) return@withContext null
            cache.get(key)?.let { return@withContext it }
            val size = pageSizes[index]
            val width = bucket.coerceAtMost(MAX_SIDE)
            val height = (width * size.height.toFloat() / size.width).roundToInt().coerceIn(1, MAX_SIDE)
            val bitmap = try {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } catch (e: OutOfMemoryError) {
                cache.evictAll()
                return@withContext null
            }
            bitmap.eraseColor(Color.WHITE)
            runCatching {
                renderer.openPage(index).use { it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
            }.onFailure { return@withContext null }
            cache.put(key, bitmap)
            bitmap
        }
    }

    fun cached(index: Int, targetWidth: Int): Bitmap? {
        val bucket = ((targetWidth + 127) / 128) * 128
        return cache.get("$index:$bucket")
    }

    override fun close() {
        if (closed) return
        closed = true
        // Close on the render thread so we never close while a page is mid-render.
        renderScope.execute {
            runCatching { renderer.close() }
            runCatching { pfd.close() }
            cache.evictAll()
        }
    }

    companion object {
        private const val MAX_SIDE = 4096
        private val renderScope = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
            Thread(r, "pdf-render").apply { priority = Thread.NORM_PRIORITY + 1 }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private val renderDispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) = renderScope.execute(block)
        }

        private fun cacheBytes(): Int {
            val max = Runtime.getRuntime().maxMemory()
            return (max / 5).coerceAtMost(160L * 1024 * 1024).toInt()
        }

        /** Throws [SecurityException] when the document is password protected. */
        suspend fun open(pfdProvider: () -> ParcelFileDescriptor): PdfDocumentSession = withContext(Dispatchers.IO) {
            val pfd = pfdProvider()
            val renderer = try {
                withContext(renderDispatcher) { PdfRenderer(pfd) }
            } catch (t: Throwable) {
                runCatching { pfd.close() }
                throw t
            }
            val sizes = withContext(renderDispatcher) {
                (0 until renderer.pageCount).map { i -> renderer.openPage(i).use { Size(it.width, it.height) } }
            }
            PdfDocumentSession(pfd, renderer, sizes)
        }
    }
}
