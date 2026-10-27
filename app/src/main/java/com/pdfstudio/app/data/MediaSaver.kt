package com.pdfstudio.app.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** Saves exported images into the public Pictures/PDF Studio folder (visible in the gallery). */
object MediaSaver {
    const val FOLDER = "PDF Studio"

    /** Saves an image to the gallery, in Pictures/PDF Studio. */
    fun saveImage(context: Context, bytes: ByteArray, displayName: String, mime: String) {
        save(context, Environment.DIRECTORY_PICTURES, displayName, mime) { it.write(bytes) }
    }

    private fun save(context: Context, directory: String, name: String, mime: String, write: (OutputStream) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/$FOLDER")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: throw IOException("Unable to create $name")
            try {
                resolver.openOutputStream(uri)?.use(write) ?: throw IOException("Unable to write $name")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(directory), FOLDER)
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Unable to create ${dir.path}")
            val target = uniqueFile(dir, name)
            target.outputStream().buffered().use(write)
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime), null)
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        if (!file.exists()) return file
        val base = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (file.exists()) {
            file = File(dir, if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext")
            i++
        }
        return file
    }
}
