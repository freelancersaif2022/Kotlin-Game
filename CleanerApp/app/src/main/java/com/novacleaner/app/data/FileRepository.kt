package com.novacleaner.app.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileEntry(
    val path: String,
    val name: String,
    val size: Long,
    val modified: Long,
    val kind: FileKind,
    val apk: ApkMeta? = null,
)

object FileRepository {

    /** Files at least [minBytes] large, biggest first, read from the MediaStore index. */
    suspend fun largeFiles(context: Context, minBytes: Long): List<FileEntry> = withContext(Dispatchers.IO) {
        val result = ArrayList<FileEntry>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        runCatching {
            context.contentResolver.query(
                uri, projection, "${MediaStore.Files.FileColumns.SIZE} >= ?", arrayOf(minBytes.toString()),
                "${MediaStore.Files.FileColumns.SIZE} DESC",
            )?.use { c ->
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val path = c.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.isFile) continue
                    val name = c.getString(nameCol) ?: file.name
                    result += FileEntry(
                        path = path,
                        name = name,
                        size = c.getLong(sizeCol),
                        modified = c.getLong(dateCol) * 1000L,
                        kind = FileKind.from(c.getString(mimeCol), name),
                    )
                }
            }
        }
        result
    }

    /** Every APK on shared storage (walks the file system, the index often misses them). */
    suspend fun apkFiles(context: Context): List<FileEntry> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val skip = buildSet {
            add(File(root, "Android/obb").absolutePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(File(root, "Android/data").absolutePath)
        }
        val pm = context.packageManager
        val result = ArrayList<FileEntry>()
        val stack = ArrayDeque<Pair<File, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty()) {
            val (dir, depth) = stack.removeLast()
            if (dir.absolutePath in skip || depth > 10) continue
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory) {
                    stack.addLast(f to depth + 1)
                } else if (f.name.endsWith(".apk", ignoreCase = true)) {
                    result += FileEntry(
                        path = f.absolutePath,
                        name = f.name,
                        size = f.length(),
                        modified = f.lastModified(),
                        kind = FileKind.APK,
                        apk = ApkInspector.inspect(pm, f.absolutePath),
                    )
                }
            }
        }
        result.sortedByDescending { it.size }
    }
}
