package com.pdfmaster.app.ui.viewer

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfmaster.app.data.repo.OutputStorage
import com.pdfmaster.app.pdf.PdfDocumentSession
import com.pdfmaster.app.pdf.PdfToolException
import com.pdfmaster.app.pdf.PdfTools
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoadResult {
    data class Ready(val session: PdfDocumentSession, val localCopy: File?) : LoadResult
    data object NeedsPassword : LoadResult
}

/** Opens a document for rendering, falling back to a local copy for non-seekable providers. */
@Singleton
class DocumentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: OutputStorage,
    private val tools: PdfTools,
) {
    suspend fun open(uri: Uri): LoadResult {
        return try {
            LoadResult.Ready(PdfDocumentSession.open { pfd(uri) }, null)
        } catch (e: SecurityException) {
            LoadResult.NeedsPassword
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: IOException) {
            // Some providers hand out pipes the renderer can't seek; a local copy fixes that.
            val copy = storage.copyToWork(uri)
            try {
                LoadResult.Ready(PdfDocumentSession.open { ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY) }, copy)
            } catch (s: SecurityException) {
                LoadResult.NeedsPassword
            }
        }
    }

    suspend fun openWithPassword(uri: Uri, password: String): LoadResult.Ready {
        val copy = storage.copyToWork(uri)
        val decrypted = storage.newWorkFile("pdf")
        try {
            tools.decryptForViewing(copy, password, decrypted)
        } finally {
            copy.delete()
        }
        return LoadResult.Ready(PdfDocumentSession.open { ParcelFileDescriptor.open(decrypted, ParcelFileDescriptor.MODE_READ_ONLY) }, decrypted)
    }

    private fun pfd(uri: Uri): ParcelFileDescriptor =
        context.contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException("Cannot open $uri")

    companion object {
        fun isWrongPassword(t: Throwable) = t is PdfToolException
    }
}
