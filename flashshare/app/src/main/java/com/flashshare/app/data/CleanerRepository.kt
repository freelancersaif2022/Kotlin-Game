package com.flashshare.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.ContactsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class CleanItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String,
    val dateAdded: Long,
    val group: Int = 0,
)

data class CleanResult(
    val large: List<CleanItem>,
    /** Duplicate copies (the first/oldest copy of each group is excluded so it is kept). */
    val duplicates: List<CleanItem>,
    val apks: List<CleanItem>,
    val screenshots: List<CleanItem>,
    val cacheBytes: Long,
) {
    val total: Long get() = (large + duplicates + apks + screenshots).distinctBy { it.uri }.sumOf { it.size } + cacheBytes
}

data class StorageInfo(val total: Long, val free: Long) {
    val used: Long get() = total - free
    val usedFraction: Float get() = if (total > 0) used.toFloat() / total else 0f
}

class CleanerRepository(private val context: Context) {

    fun storage(): StorageInfo = runCatching {
        val s = StatFs(Environment.getExternalStorageDirectory().path)
        StorageInfo(s.totalBytes, s.availableBytes)
    }.getOrDefault(StorageInfo(0, 0))

    suspend fun scan(onProgress: (Float, String) -> Unit): CleanResult = withContext(Dispatchers.IO) {
        onProgress(0.01f, "Scanning media…")
        val files = allFiles { p -> onProgress(0.01f + p * 0.29f, "Scanning media…") }
        coroutineContext.ensureActive()

        onProgress(0.3f, "Finding large files…")
        val large = files.filter { it.size >= LARGE }.sortedByDescending { it.size }

        onProgress(0.45f, "Looking for duplicates…")
        val duplicates = findDuplicates(files) { p -> onProgress(0.45f + p * 0.4f, "Comparing files…") }

        onProgress(0.88f, "Checking installers & screenshots…")
        val apks = files.filter { it.mime == "application/vnd.android.package-archive" || it.name.endsWith(".apk", true) }
        val shots = files.filter { it.mime.startsWith("image/") && it.name.contains("screenshot", true) && System.currentTimeMillis() - it.dateAdded > 30L * 86_400_000 }

        onProgress(0.97f, "Measuring cache…")
        val cache = dirSize(context.cacheDir) + (context.externalCacheDir?.let { dirSize(it) } ?: 0)
        onProgress(1f, "Done")
        CleanResult(large, duplicates, apks, shots, cache)
    }

    private fun allFiles(progress: (Float) -> Unit = {}): List<CleanItem> {
        val base = MediaStore.Files.getContentUri("external")
        val cols = arrayOf(
            MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val out = ArrayList<CleanItem>()
        runCatching {
            context.contentResolver.query(base, cols, "${MediaStore.Files.FileColumns.SIZE} > 0", null, null)?.use { c ->
                val total = c.count.coerceAtLeast(1)
                while (c.moveToNext()) {
                    if (c.position % 400 == 0) progress(c.position.toFloat() / total)
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    val mediaType = c.getInt(5)
                    val uri = when (mediaType) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        else -> ContentUris.withAppendedId(base, id)
                    }
                    out += CleanItem(uri, name, c.getLong(2), c.getString(3) ?: MediaRepository.guessMime(name), c.getLong(4) * 1000)
                }
            }
        }
        return out
    }

    private suspend fun findDuplicates(files: List<CleanItem>, progress: (Float) -> Unit): List<CleanItem> {
        val candidates = files.filter { it.size >= 16 * 1024 }.groupBy { it.size }.values.filter { it.size > 1 }
        val out = ArrayList<CleanItem>()
        var group = 0
        candidates.forEachIndexed { i, sameSize ->
            coroutineContext.ensureActive()
            if (i % 5 == 0) progress(i.toFloat() / candidates.size.coerceAtLeast(1))
            sameSize.groupBy { fingerprint(it) }.forEach { (fp, copies) ->
                if (fp != null && copies.size > 1) {
                    group++
                    copies.sortedBy { it.dateAdded }.drop(1).forEach { out += it.copy(group = group) }
                }
            }
        }
        return out
    }

    /** SHA-256 of the first and last 64 KB – fast and reliable for equally sized files. */
    private fun fingerprint(item: CleanItem): String? = runCatching {
        context.contentResolver.openFileDescriptor(item.uri, "r")?.use { pfd ->
            val ch = java.io.FileInputStream(pfd.fileDescriptor).channel
            val md = MessageDigest.getInstance("SHA-256")
            val buf = java.nio.ByteBuffer.allocate(64 * 1024)
            ch.read(buf, 0); buf.flip(); md.update(buf)
            if (item.size > 128 * 1024) {
                buf.clear(); ch.read(buf, item.size - 64 * 1024); buf.flip(); md.update(buf)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
    }.getOrNull()

    fun clearCache(): Long {
        val before = dirSize(context.cacheDir) + (context.externalCacheDir?.let { dirSize(it) } ?: 0)
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        return before
    }

    private fun dirSize(f: File): Long = f.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    /** Exports all contacts as one .vcf file for Phone Clone. */
    suspend fun exportContacts(): ShareItem? = withContext(Dispatchers.IO) {
        runCatching {
            val cr = context.contentResolver
            val keys = ArrayList<String>()
            cr.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts.LOOKUP_KEY), null, null, null)?.use { c ->
                while (c.moveToNext()) c.getString(0)?.let { keys += it }
            }
            if (keys.isEmpty()) return@runCatching null
            val dir = File(context.cacheDir, "clone").apply { mkdirs() }
            val out = File(dir, "Contacts.vcf")
            out.outputStream().use { os ->
                keys.chunked(100).forEach { chunk ->
                    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(chunk.joinToString(":")))
                    runCatching { cr.openAssetFileDescriptor(uri, "r")?.use { afd -> afd.createInputStream().copyTo(os) } }
                }
            }
            ShareItem(Uri.fromFile(out), "Contacts (${keys.size}).vcf", out.length(), "text/x-vcard", Category.CONTACT,
                subtitle = "${keys.size} contacts")
        }.getOrNull()
    }

    fun contactCount(): Int = runCatching {
        context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts._ID), null, null, null)
            ?.use { it.count } ?: 0
    }.getOrDefault(0)

    private companion object {
        const val LARGE = 100L * 1024 * 1024
    }
}
