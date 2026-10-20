package com.flashshare.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileEntry(
    val file: File,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
    val children: Int,
) {
    val name: String get() = file.name
    val mime: String get() = MediaRepository.guessMime(file.name)
    val category: Category get() = Category.fromMime(mime, file.name)
}

/** Browses the whole shared storage (needs "All files access" on Android 11+). */
object FileBrowser {
    val root: File get() = Environment.getExternalStorageDirectory()

    fun canBrowse(ctx: Context): Boolean = if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun list(dir: File, showHidden: Boolean = false): List<FileEntry> = withContext(Dispatchers.IO) {
        (dir.listFiles() ?: emptyArray())
            .asSequence()
            .filter { showHidden || !it.name.startsWith(".") }
            .map { f ->
                if (f.isDirectory) FileEntry(f, true, 0, f.lastModified(), f.list()?.count { showHidden || !it.startsWith(".") } ?: 0)
                else FileEntry(f, false, f.length(), f.lastModified(), 0)
            }
            .sortedWith(compareBy<FileEntry>({ !it.isDir }, { it.name.lowercase() }))
            .toList()
    }

    fun toItem(f: File, relPath: String = ""): ShareItem {
        val mime = MediaRepository.guessMime(f.name)
        return ShareItem(
            Uri.fromFile(f), f.name, f.length(), mime, Category.fromMime(mime, f.name),
            relPath = relPath, dateAdded = f.lastModified(), subtitle = f.name.substringAfterLast('.', "").uppercase(),
        )
    }

    /** All files inside a folder (recursively), keeping the folder structure for the receiver. */
    suspend fun expand(dir: File): List<ShareItem> = withContext(Dispatchers.IO) {
        val base = dir.parentFile ?: dir
        dir.walkTopDown()
            .onEnter { !it.name.startsWith(".") || it == dir }
            .filter { it.isFile && !it.name.startsWith(".") }
            .map { f -> toItem(f, f.parentFile!!.relativeTo(base).path) }
            .toList()
    }

    /** Human friendly path, e.g. "Internal storage › DCIM › Camera". */
    fun pretty(dir: File): List<Pair<String, File>> {
        val out = ArrayList<Pair<String, File>>()
        var d: File? = dir
        while (d != null) {
            if (d == root) { out += "Internal storage" to d; break }
            out += d.name to d
            d = d.parentFile
        }
        return out.reversed()
    }
}
