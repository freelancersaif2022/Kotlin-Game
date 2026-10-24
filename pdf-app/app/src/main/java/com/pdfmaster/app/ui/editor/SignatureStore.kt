package com.pdfmaster.app.ui.editor

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignatureStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val dir get() = File(context.filesDir, "signatures").apply { mkdirs() }

    suspend fun list(): List<File> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "png" }?.sortedByDescending { it.lastModified() }.orEmpty()
    }

    suspend fun save(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val file = File(dir, "sig_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }

    suspend fun delete(file: File) = withContext(Dispatchers.IO) { file.delete() }
}
