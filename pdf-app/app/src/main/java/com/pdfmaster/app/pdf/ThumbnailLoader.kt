package com.pdfmaster.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** First-page thumbnails for file lists, cached in memory and on disk. */
@Singleton
class ThumbnailLoader @Inject constructor(@ApplicationContext private val context: Context) {

    private val memory = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val failed = HashSet<String>()
    private val permits = Semaphore(2)
    private val dir by lazy { File(context.cacheDir, "thumbs").apply { mkdirs() } }

    fun peek(uri: Uri): Bitmap? = memory.get(uri.toString())

    suspend fun load(uri: Uri, width: Int = 180): Bitmap? {
        val key = uri.toString()
        memory.get(key)?.let { return it }
        if (synchronized(failed) { key in failed }) return null
        return permits.withPermit {
            withContext(Dispatchers.IO) {
                memory.get(key)?.let { return@withContext it }
                val disk = File(dir, key.hashCode().toUInt().toString(16) + ".webp")
                if (disk.exists()) {
                    BitmapFactory.decodeFile(disk.path)?.let { memory.put(key, it); return@withContext it }
                }
                val bmp = runCatching {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        PdfRenderer(pfd).use { r ->
                            r.openPage(0).use { page ->
                                val h = (width * page.height.toFloat() / page.width).roundToInt().coerceIn(1, width * 2)
                                Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888).also {
                                    it.eraseColor(Color.WHITE)
                                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                }
                            }
                        }
                    }
                }.getOrNull()
                if (bmp == null) {
                    synchronized(failed) { failed += key }
                    return@withContext null
                }
                memory.put(key, bmp)
                runCatching {
                    disk.outputStream().use {
                        @Suppress("DEPRECATION")
                        bmp.compress(Bitmap.CompressFormat.WEBP, 80, it)
                    }
                }
                bmp
            }
        }
    }

    fun invalidate(uri: Uri) {
        memory.remove(uri.toString())
        synchronized(failed) { failed -= uri.toString() }
    }
}
