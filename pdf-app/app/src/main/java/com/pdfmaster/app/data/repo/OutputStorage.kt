package com.pdfmaster.app.data.repo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.pdfmaster.app.data.model.OutputFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class OutputKind { DOCUMENT, IMAGE, TEXT }

/** Owns temporary working files and publishes finished outputs to shared storage. */
@Singleton
class OutputStorage @Inject constructor(@ApplicationContext private val context: Context) {

    private val folder = "PDF Master"

    fun workDir(): File = File(context.cacheDir, "work").apply { mkdirs() }

    fun newWorkFile(extension: String): File = File(workDir(), "tmp_${System.nanoTime()}.$extension")

    fun locationLabel(kind: OutputKind): String = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> "App storage / $folder"
        kind == OutputKind.IMAGE -> "Pictures / $folder"
        else -> "Download / $folder"
    }

    fun uniqueName(base: String, extension: String, suffix: String? = null): String {
        val clean = base.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "PDFMaster" }.take(60)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tail = if (suffix.isNullOrBlank()) stamp else "${suffix}_$stamp"
        return "${clean}_$tail.$extension"
    }

    suspend fun copyToWork(uri: Uri, extension: String = "pdf"): File = withContext(Dispatchers.IO) {
        val out = newWorkFile(extension)
        val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException("Unable to open $uri")
        input.use { i -> out.outputStream().use { o -> i.copyTo(o, 64 * 1024) } }
        out
    }

    suspend fun publish(file: File, displayName: String, mime: String, kind: OutputKind, pageCount: Int = 0): OutputFile =
        withContext(Dispatchers.IO) {
            val size = file.length()
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                publishToMediaStore(file, displayName, mime, kind)
            } else {
                val dirType = if (kind == OutputKind.IMAGE) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOCUMENTS
                val dir = File(context.getExternalFilesDir(dirType) ?: context.filesDir, folder).apply { mkdirs() }
                val target = File(dir, displayName)
                file.copyTo(target, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
            }
            file.delete()
            OutputFile(uri, displayName, size, mime, pageCount)
        }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun publishToMediaStore(file: File, displayName: String, mime: String, kind: OutputKind): Uri {
        val resolver = context.contentResolver
        val collection = if (kind == OutputKind.IMAGE) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relative = (if (kind == OutputKind.IMAGE) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS) + "/" + folder
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IllegalStateException("Could not create output file")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out, 64 * 1024) } }
                ?: throw IllegalStateException("Could not write output file")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    fun cacheSize(): Long = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache() {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
