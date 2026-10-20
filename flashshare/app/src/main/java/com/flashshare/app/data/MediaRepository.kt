package com.flashshare.app.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    private val cr get() = context.contentResolver

    suspend fun photos(): List<ShareItem> = queryVisual(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Category.PHOTO, video = false
    )

    suspend fun videos(): List<ShareItem> = queryVisual(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Category.VIDEO, video = true
    )

    private suspend fun queryVisual(base: Uri, cat: Category, video: Boolean): List<ShareItem> = withContext(Dispatchers.IO) {
        val cols = mutableListOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        if (video) cols += MediaStore.Video.VideoColumns.DURATION
        val out = ArrayList<ShareItem>()
        runCatching {
            cr.query(base, cols.toTypedArray(), "${MediaStore.MediaColumns.SIZE} > 0", null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
                val iId = c.getColumnIndexOrThrow(cols[0]); val iName = c.getColumnIndexOrThrow(cols[1])
                val iSize = c.getColumnIndexOrThrow(cols[2]); val iMime = c.getColumnIndexOrThrow(cols[3])
                val iDate = c.getColumnIndexOrThrow(cols[4]); val iBucket = c.getColumnIndex(cols[5])
                val iDur = if (video) c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val name = c.getString(iName) ?: "file_$id"
                    out += ShareItem(
                        uri = ContentUris.withAppendedId(base, id),
                        name = name,
                        size = c.getLong(iSize),
                        mime = c.getString(iMime) ?: guessMime(name),
                        category = cat,
                        dateAdded = c.getLong(iDate) * 1000,
                        durationMs = if (iDur >= 0) c.getLong(iDur) else 0,
                        subtitle = if (iBucket >= 0) c.getString(iBucket).orEmpty() else "",
                    )
                }
            }
        }
        out
    }

    suspend fun music(): List<ShareItem> = withContext(Dispatchers.IO) {
        val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TITLE
        )
        val out = ArrayList<ShareItem>()
        runCatching {
            cr.query(base, cols, "${MediaStore.Audio.Media.SIZE} > 0", null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: "audio_$id"
                    val artist = c.getString(5)?.takeUnless { it == "<unknown>" } ?: "Unknown artist"
                    out += ShareItem(
                        uri = ContentUris.withAppendedId(base, id), name = name, size = c.getLong(2),
                        mime = c.getString(3) ?: guessMime(name), category = Category.MUSIC,
                        dateAdded = c.getLong(4) * 1000, durationMs = c.getLong(6), subtitle = artist,
                    )
                }
            }
        }
        out
    }

    suspend fun apps(includeSystem: Boolean = false): List<ShareItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val launchable = pm.queryIntentActivities(launcher, 0).map { it.activityInfo.packageName }.toHashSet()
        @Suppress("DEPRECATION")
        pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName in launchable && it.packageName != context.packageName }
            .filter { includeSystem || it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 }
            .mapNotNull { ai ->
                val apk = File(ai.sourceDir ?: return@mapNotNull null)
                if (!apk.canRead()) return@mapNotNull null
                val label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(ai.packageName)
                val version = runCatching {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(ai.packageName, 0).versionName
                }.getOrNull().orEmpty()
                ShareItem(
                    uri = Uri.fromFile(apk),
                    name = sanitize(label) + ".apk",
                    size = apk.length(),
                    mime = "application/vnd.android.package-archive",
                    category = Category.APP,
                    subtitle = if (version.isNotEmpty()) "v$version" else ai.packageName,
                    packageName = ai.packageName,
                    dateAdded = apk.lastModified(),
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    /** Documents visible through MediaStore (everything on Android ≤12, own + shared downloads on 13+). */
    suspend fun documents(): List<ShareItem> = withContext(Dispatchers.IO) {
        val base = MediaStore.Files.getContentUri("external")
        val mimes = DOC_MIMES
        val sel = buildString {
            append("${MediaStore.Files.FileColumns.SIZE} > 0 AND (")
            append(mimes.joinToString(" OR ") { "${MediaStore.Files.FileColumns.MIME_TYPE} = ?" })
            append(")")
        }
        val cols = arrayOf(
            MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DATE_ADDED
        )
        val out = ArrayList<ShareItem>()
        runCatching {
            cr.query(base, cols, sel, mimes.toTypedArray(), "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(3) ?: guessMime(name)
                    out += ShareItem(
                        uri = ContentUris.withAppendedId(base, id), name = name, size = c.getLong(2), mime = mime,
                        category = Category.fromMime(mime, name), dateAdded = c.getLong(4) * 1000,
                        subtitle = name.substringAfterLast('.', "").uppercase(),
                    )
                }
            }
        }
        out
    }

    /** Resolves files picked through the system document picker. */
    suspend fun fromDocumentUris(uris: List<Uri>): List<ShareItem> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            runCatching {
                var name = "file"
                var size = -1L
                cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        name = c.getString(0) ?: name
                        if (!c.isNull(1)) size = c.getLong(1)
                    }
                }
                if (size < 0) size = cr.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1
                if (size < 0) return@runCatching null
                val mime = cr.getType(uri) ?: guessMime(name)
                ShareItem(uri, name, size, mime, Category.fromMime(mime, name), subtitle = name.substringAfterLast('.', "").uppercase())
            }.getOrNull()
        }
    }

    /** Recursively expands a picked folder, keeping the folder structure in [ShareItem.relPath]. */
    suspend fun fromTree(tree: Uri): List<ShareItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, tree) ?: return@withContext emptyList()
        val out = ArrayList<ShareItem>()
        fun walk(dir: DocumentFile, path: String) {
            dir.listFiles().forEach { f ->
                if (f.isDirectory) walk(f, path + "/" + sanitize(f.name ?: "folder"))
                else if (f.isFile && f.length() >= 0) {
                    val name = f.name ?: return@forEach
                    val mime = f.type ?: guessMime(name)
                    out += ShareItem(f.uri, name, f.length(), mime, Category.fromMime(mime, name), relPath = path.trimStart('/'))
                }
            }
        }
        walk(root, sanitize(root.name ?: "Folder"))
        out
    }

    /** Files FlashShare received, from the configured save location. */
    suspend fun received(treeUri: String?): List<ShareItem> = withContext(Dispatchers.IO) {
        if (treeUri != null) {
            val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return@withContext emptyList()
            val out = ArrayList<ShareItem>()
            fun walk(d: DocumentFile, depth: Int) {
                if (depth > 6) return
                d.listFiles().forEach { f ->
                    if (f.isDirectory) walk(f, depth + 1)
                    else {
                        val name = f.name ?: return@forEach
                        val mime = f.type ?: guessMime(name)
                        out += ShareItem(f.uri, name, f.length(), mime, Category.fromMime(mime, name), dateAdded = f.lastModified())
                    }
                }
            }
            walk(root, 0)
            return@withContext out.sortedByDescending { it.dateAdded }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val base = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val cols = arrayOf(
                MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_ADDED
            )
            val out = ArrayList<ShareItem>()
            runCatching {
                cr.query(base, cols, "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf("Download/FlashShare/%"),
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
                    while (c.moveToNext()) {
                        val name = c.getString(1) ?: continue
                        val mime = c.getString(3) ?: guessMime(name)
                        out += ShareItem(ContentUris.withAppendedId(base, c.getLong(0)), name, c.getLong(2), mime,
                            Category.fromMime(mime, name), dateAdded = c.getLong(4) * 1000)
                    }
                }
            }
            return@withContext out
        }
        @Suppress("DEPRECATION")
        val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "FlashShare")
        dir.walkTopDown().filter { it.isFile }.map { f ->
            val mime = guessMime(f.name)
            ShareItem(Uri.fromFile(f), f.name, f.length(), mime, Category.fromMime(mime, f.name), dateAdded = f.lastModified())
        }.sortedByDescending { it.dateAdded }.toList()
    }

    companion object {
        val DOC_MIMES = listOf(
            "application/pdf", "application/msword", "text/plain", "application/rtf", "text/csv",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed", "application/epub+zip",
            "application/vnd.android.package-archive", "application/json", "text/html", "text/x-vcard", "text/vcard",
        )

        fun guessMime(name: String): String {
            val ext = name.substringAfterLast('.', "").lowercase()
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                ?: if (ext == "apk") "application/vnd.android.package-archive" else "application/octet-stream"
        }

        fun sanitize(name: String): String =
            name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_").trim().trim('.').ifEmpty { "file" }.take(120)

        val isTiramisu get() = Build.VERSION.SDK_INT >= 33
    }
}
