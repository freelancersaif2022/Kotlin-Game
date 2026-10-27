package com.pdfstudio.app.data

import android.graphics.Bitmap
import android.util.LruCache

/** Memory-bounded cache of rendered pages, keyed by document, page and width. */
class PageBitmapCache {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun get(docId: Int, page: Int, width: Int): Bitmap? = cache.get(key(docId, page, width))

    fun put(docId: Int, page: Int, width: Int, bitmap: Bitmap) {
        cache.put(key(docId, page, width), bitmap)
    }

    fun evictDocument(docId: Int) {
        val prefix = "$docId:"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    private fun key(docId: Int, page: Int, width: Int) = "$docId:$page:$width"
}
