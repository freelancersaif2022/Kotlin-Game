package com.novacleaner.app.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.math.abs

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val dateTaken: Long,
    val width: Int,
    val height: Int,
    val bucket: String,
) {
    val pixels: Long get() = width.toLong() * height
}

data class DuplicateGroup(val id: String, val items: List<MediaItem>, val similar: Boolean) {
    /** The copy we keep by default: highest resolution, then largest file, then newest. */
    val best: MediaItem
        get() = items.maxWith(compareBy<MediaItem> { it.pixels }.thenBy { it.size }.thenBy { it.dateTaken })
    val reclaimable: Long get() = items.filter { it.id != best.id }.sumOf { it.size }
}

data class DuplicateResult(val exact: List<DuplicateGroup>, val similar: List<DuplicateGroup>)

object MediaRepository {

    suspend fun images(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = ArrayList<MediaItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        runCatching {
            context.contentResolver.query(collection, projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val bucketCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (c.moveToNext()) {
                    val path = c.getString(dataCol) ?: continue
                    val id = c.getLong(idCol)
                    val taken = c.getLong(takenCol).takeIf { it > 0 } ?: (c.getLong(addedCol) * 1000L)
                    list += MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        path = path,
                        name = c.getString(nameCol) ?: File(path).name,
                        size = c.getLong(sizeCol),
                        dateTaken = taken,
                        width = c.getInt(wCol),
                        height = c.getInt(hCol),
                        bucket = c.getString(bucketCol).orEmpty(),
                    )
                }
            }
        }
        list
    }

    fun isScreenshot(item: MediaItem): Boolean =
        item.path.contains("screenshot", ignoreCase = true) || item.bucket.contains("screenshot", ignoreCase = true)

    suspend fun screenshots(context: Context): List<MediaItem> =
        images(context).filter { isScreenshot(it) && File(it.path).exists() }.sortedByDescending { it.dateTaken }

    /** Exact duplicates (same bytes) and visually similar shots (bursts, re-saves). */
    suspend fun findDuplicates(
        context: Context,
        onProgress: suspend (Float, String) -> Unit,
    ): DuplicateResult = withContext(Dispatchers.IO) {
        val all = images(context).filter { it.size > 0 && File(it.path).exists() }
        onProgress(0.05f, "Indexing ${all.size} photos")

        // --- Exact duplicates: same size, then same partial content hash.
        val bySize = all.groupBy { it.size }.values.filter { it.size > 1 }
        val candidates = bySize.sumOf { it.size }.coerceAtLeast(1)
        var hashed = 0
        val exactGroups = ArrayList<DuplicateGroup>()
        for (group in bySize) {
            currentCoroutineContext().ensureActive()
            val byHash = group.groupBy { item ->
                hashed++
                if (hashed % 10 == 0) onProgress(0.05f + 0.30f * hashed / candidates, item.name)
                partialHash(item.path)
            }
            byHash.forEach { (hash, items) ->
                if (hash != null && items.size > 1) exactGroups += DuplicateGroup("e$hash", items, similar = false)
            }
        }
        val inExact = exactGroups.flatMap { g -> g.items.map { it.id } }.toHashSet()

        // --- Similar photos: perceptual hash of recent photos, compared within short time windows.
        val pool = all.filter { it.id !in inExact }.sortedBy { it.dateTaken }.takeLast(MAX_SIMILAR_POOL)
        val hashes = LongArray(pool.size)
        val valid = BooleanArray(pool.size)
        pool.forEachIndexed { i, item ->
            currentCoroutineContext().ensureActive()
            val h = dHash(item.path)
            if (h != null) {
                hashes[i] = h
                valid[i] = true
            }
            if (i % 6 == 0) onProgress(0.35f + 0.6f * i / pool.size.coerceAtLeast(1), item.name)
        }
        val parent = IntArray(pool.size) { it }
        fun find(x: Int): Int {
            var r = x
            while (parent[r] != r) r = parent[r]
            var n = x
            while (parent[n] != r) { val next = parent[n]; parent[n] = r; n = next }
            return r
        }
        for (i in pool.indices) {
            if (!valid[i]) continue
            var j = i + 1
            while (j < pool.size && j <= i + 12 && pool[j].dateTaken - pool[i].dateTaken <= SIMILAR_WINDOW_MS) {
                if (valid[j] && aspectMatches(pool[i], pool[j]) &&
                    java.lang.Long.bitCount(hashes[i] xor hashes[j]) <= SIMILAR_THRESHOLD
                ) {
                    parent[find(j)] = find(i)
                }
                j++
            }
        }
        val similarGroups = pool.indices.groupBy { find(it) }.values
            .filter { it.size > 1 }
            .map { idx -> DuplicateGroup("s${pool[idx.first()].id}", idx.map { pool[it] }.sortedBy { it.dateTaken }, similar = true) }
        onProgress(1f, "Done")
        DuplicateResult(
            exact = exactGroups.sortedByDescending { it.reclaimable },
            similar = similarGroups.sortedByDescending { it.items.maxOf { item -> item.dateTaken } },
        )
    }

    private fun aspectMatches(a: MediaItem, b: MediaItem): Boolean {
        if (a.width <= 0 || a.height <= 0 || b.width <= 0 || b.height <= 0) return true
        val ra = a.width.toFloat() / a.height
        val rb = b.width.toFloat() / b.height
        return abs(ra - rb) < 0.06f || abs(ra - 1f / rb) < 0.06f
    }

    private fun partialHash(path: String): String? = runCatching {
        val md = MessageDigest.getInstance("MD5")
        RandomAccessFile(path, "r").use { raf ->
            val len = raf.length()
            val buf = ByteArray(CHUNK)
            val head = raf.read(buf)
            if (head > 0) md.update(buf, 0, head)
            if (len > CHUNK * 2) {
                raf.seek(len - CHUNK)
                val tail = raf.read(buf)
                if (tail > 0) md.update(buf, 0, tail)
            }
            md.update(len.toString().toByteArray())
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    private fun dHash(path: String): Long? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 48 && bounds.outHeight / (sample * 2) >= 48) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = BitmapFactory.decodeFile(path, opts) ?: return null
        val small = Bitmap.createScaledBitmap(decoded, 9, 8, true)
        if (small !== decoded) decoded.recycle()
        var hash = 0L
        var bit = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                if (luma(small.getPixel(x, y)) > luma(small.getPixel(x + 1, y))) hash = hash or (1L shl bit)
                bit++
            }
        }
        small.recycle()
        hash
    }.getOrNull()

    private fun luma(c: Int) = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000

    private const val CHUNK = 64 * 1024
    private const val MAX_SIMILAR_POOL = 2000
    private const val SIMILAR_WINDOW_MS = 20 * 60 * 1000L
    private const val SIMILAR_THRESHOLD = 6
}
