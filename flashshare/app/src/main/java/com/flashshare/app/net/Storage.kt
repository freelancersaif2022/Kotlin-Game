package com.flashshare.app.net

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.flashshare.app.data.MediaRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

const val ROOT_FOLDER = "FlashShare"


/** Creates sinks in the folder chosen in Settings (default: Download/FlashShare/<Category>). */
class FileSaver(private val context: Context, private val treeUri: String?) : SinkFactory {
    private val dirLock = Any()
    private val dirCache = HashMap<String, DocumentFile>()

    override fun create(meta: FileMeta): Sink {
        val name = MediaRepository.sanitize(meta.name)
        val sub = buildList {
            add(meta.category.folder)
            meta.relPath.split('/').map { MediaRepository.sanitize(it) }.filter { it.isNotBlank() && it != "." && it != ".." }.forEach { add(it) }
        }
        return when {
            treeUri != null -> runCatching { treeSink(Uri.parse(treeUri), sub, name, meta.mime) }.getOrElse { defaultSink(sub, name, meta.mime) }
            else -> defaultSink(sub, name, meta.mime)
        }
    }

    private fun defaultSink(sub: List<String>, name: String, mime: String): Sink =
        if (Build.VERSION.SDK_INT >= 29) mediaStoreSink(sub, name, mime) else legacySink(sub, name)

    private fun mediaStoreSink(sub: List<String>, name: String, mime: String): Sink {
        val cr = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + ROOT_FOLDER + "/" + sub.joinToString("/") + "/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("Couldn't create $name")
        val pfd = cr.openFileDescriptor(uri, "rw") ?: throw IOException("Couldn't open $name")
        val fc = FileOutputStream(pfd.fileDescriptor).channel
        return object : Sink {
            override val channel: FileChannel = fc
            override fun finish(): String {
                runCatching { fc.force(false) }
                fc.close(); pfd.close()
                cr.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                return uri.toString()
            }
            override fun abort() {
                runCatching { fc.close(); pfd.close() }
                runCatching { cr.delete(uri, null, null) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun legacySink(sub: List<String>, name: String): Sink {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ROOT_FOLDER + "/" + sub.joinToString("/"))
        synchronized(dirLock) { dir.mkdirs() }
        val file = uniqueFile(dir, name)
        val raf = RandomAccessFile(file, "rw")
        return object : Sink {
            override val channel: FileChannel = raf.channel
            override fun finish(): String {
                raf.close()
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                return FileProvider.getUriForFile(context, context.packageName + ".files", file).toString()
            }
            override fun abort() {
                runCatching { raf.close() }
                file.delete()
            }
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        synchronized(dirLock) {
            var f = File(dir, name)
            val base = name.substringBeforeLast('.', name)
            val ext = name.substringAfterLast('.', "").let { if (it.isEmpty() || it == name) "" else ".$it" }
            var i = 1
            while (f.exists()) f = File(dir, "$base ($i)$ext").also { i++ }
            f.createNewFile()
            return f
        }
    }

    private fun treeSink(tree: Uri, sub: List<String>, name: String, mime: String): Sink {
        val cr = context.contentResolver
        val dir = synchronized(dirLock) {
            var d = DocumentFile.fromTreeUri(context, tree) ?: throw IOException("Save folder unavailable")
            if (!d.canWrite()) throw IOException("Save folder isn't writable")
            var path = ""
            for (s in sub) {
                path += "/$s"
                d = dirCache[path] ?: (d.findFile(s)?.takeIf { it.isDirectory } ?: d.createDirectory(s)
                    ?: throw IOException("Couldn't create folder $s")).also { dirCache[path] = it }
            }
            d
        }
        val doc = synchronized(dirLock) { dir.createFile(mime, name) } ?: throw IOException("Couldn't create $name")
        val pfd = cr.openFileDescriptor(doc.uri, "rw") ?: throw IOException("Couldn't open $name")
        val fc = FileOutputStream(pfd.fileDescriptor).channel
        return object : Sink {
            override val channel: FileChannel = fc
            override fun finish(): String {
                runCatching { fc.force(false) }
                fc.close(); pfd.close()
                return doc.uri.toString()
            }
            override fun abort() {
                runCatching { fc.close(); pfd.close() }
                runCatching { doc.delete() }
            }
        }
    }

    override fun freeSpace(): Long = runCatching {
        StatFs(Environment.getExternalStorageDirectory().path).availableBytes
    }.getOrDefault(Long.MAX_VALUE)

    companion object {
        fun describe(treeUri: String?): String {
            if (treeUri == null) return "Internal storage/Download/$ROOT_FOLDER"
            val path = Uri.parse(treeUri).lastPathSegment.orEmpty()
            return path.replace("primary:", "Internal storage/").ifEmpty { "Custom folder" }
        }
    }
}


/** Opens a file for sending: file:// paths directly, everything else through the ContentResolver. */
fun openSource(context: Context, uri: Uri): Source {
    val pfd: ParcelFileDescriptor = when (uri.scheme) {
        "file" -> ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
        else -> context.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException("Can't open file")
    }
    return Source(FileInputStream(pfd.fileDescriptor), pfd)
}
