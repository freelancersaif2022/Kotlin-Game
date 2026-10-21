package com.novacleaner.app.data

import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DeleteResult(val freedBytes: Long, val deletedCount: Int, val failedCount: Int)

object FileOps {

    /**
     * Deletes the given paths, reporting progress (0..1). Directories are removed recursively.
     * Afterwards the media index is refreshed so galleries don't show ghosts.
     */
    suspend fun delete(
        context: Context,
        paths: Collection<String>,
        onProgress: (Float) -> Unit = {},
    ): DeleteResult = withContext(Dispatchers.IO) {
        var freed = 0L
        var deleted = 0
        var failed = 0
        val removedFiles = ArrayList<String>()
        val total = paths.size.coerceAtLeast(1)
        paths.forEachIndexed { index, path ->
            val file = File(path)
            val size = sizeOf(file)
            val ok = runCatching {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }.getOrDefault(false) || !file.exists()
            if (ok) {
                freed += size
                deleted++
                removedFiles += path
            } else {
                failed++
            }
            if (index % 8 == 0 || index == paths.size - 1) {
                withContext(Dispatchers.Main) { onProgress((index + 1f) / total) }
            }
        }
        if (removedFiles.isNotEmpty()) {
            runCatching {
                MediaScannerConnection.scanFile(context, removedFiles.take(500).toTypedArray(), null, null)
            }
        }
        DeleteResult(freed, deleted, failed)
    }

    fun sizeOf(file: File): Long = if (file.isDirectory) {
        file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    } else file.length()
}
