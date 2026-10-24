package com.pdfmaster.app.data.repo

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.pdfmaster.app.data.db.BookmarkEntity
import com.pdfmaster.app.data.db.DocumentDao
import com.pdfmaster.app.data.db.DocumentEntity
import com.pdfmaster.app.data.model.OutputFile
import com.pdfmaster.app.data.model.PdfFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class FileMeta(val name: String, val size: Long)

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DocumentDao,
) {
    val recent: Flow<List<PdfFile>> = dao.observeRecent().map { list -> list.map { it.toModel(it.lastOpened) } }
    val favorites: Flow<List<PdfFile>> = dao.observeFavorites().map { list -> list.map { it.toModel(it.lastOpened) } }
    val created: Flow<List<PdfFile>> = dao.observeCreated().map { list -> list.map { it.toModel(it.createdAt) } }

    fun observe(uri: Uri): Flow<PdfFile?> = dao.observe(uri.toString()).map { it?.toModel(it.lastOpened) }

    fun bookmarks(uri: Uri): Flow<List<Int>> = dao.observeBookmarks(uri.toString()).map { list -> list.map { it.page } }

    suspend fun toggleBookmark(uri: Uri, page: Int, add: Boolean) {
        if (add) dao.addBookmark(BookmarkEntity(uri = uri.toString(), page = page)) else dao.removeBookmark(uri.toString(), page)
    }

    fun hasStorageAccess(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun meta(uri: Uri): FileMeta = withContext(Dispatchers.IO) {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
        }
        if (size <= 0) {
            size = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L }.getOrDefault(0L)
        }
        FileMeta(name, size)
    }

    /** Page count, or -1 when the document is encrypted or cannot be parsed by the platform renderer. */
    suspend fun pageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd -> PdfRenderer(pfd).use { it.pageCount } } ?: -1
        }.getOrDefault(-1)
    }

    suspend fun isAccessible(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)
    }

    /** Records that a document was opened so it shows up in Recent files. */
    suspend fun registerOpened(uri: Uri, pageCount: Int? = null): PdfFile = withContext(Dispatchers.IO) {
        if (uri.scheme == "content") {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        val existing = dao.get(uri.toString())
        val meta = meta(uri)
        val pages = pageCount ?: existing?.pageCount?.takeIf { it > 0 } ?: pageCount(uri)
        val entity = (existing ?: DocumentEntity(uri = uri.toString(), name = meta.name, size = meta.size, pageCount = pages))
            .copy(name = meta.name, size = meta.size, pageCount = pages, lastOpened = System.currentTimeMillis())
        dao.upsert(entity)
        entity.toModel(entity.lastOpened)
    }

    suspend fun registerCreated(outputs: List<OutputFile>) {
        val now = System.currentTimeMillis()
        outputs.filter { it.isPdf }.forEach { out ->
            dao.upsert(
                DocumentEntity(
                    uri = out.uri.toString(), name = out.name, size = out.size, pageCount = out.pageCount,
                    isCreated = true, createdAt = now,
                ),
            )
        }
    }

    suspend fun setFavorite(file: PdfFile, favorite: Boolean) {
        val existing = dao.get(file.key)
        if (existing == null) {
            dao.upsert(DocumentEntity(uri = file.key, name = file.name, size = file.size, pageCount = file.pageCount, isFavorite = favorite))
        } else {
            dao.setFavorite(file.key, favorite)
        }
    }

    suspend fun setLastPage(uri: Uri, page: Int) = dao.setLastPage(uri.toString(), page)

    suspend fun lastPage(uri: Uri): Int = dao.get(uri.toString())?.lastPage ?: 0

    suspend fun removeFromRecent(file: PdfFile) = dao.removeFromRecent(file.key)

    suspend fun forget(file: PdfFile) = dao.delete(file.key)

    suspend fun clearRecent() = dao.clearRecent()

    /** Renames files this app owns. Returns false when the provider does not allow it. */
    suspend fun rename(file: PdfFile, newName: String): Boolean = withContext(Dispatchers.IO) {
        val finalName = if (newName.endsWith(".pdf", ignoreCase = true)) newName else "$newName.pdf"
        val ok = runCatching {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, finalName) }
            context.contentResolver.update(file.uri, values, null, null) > 0
        }.getOrDefault(false)
        if (ok) dao.rename(file.key, finalName)
        ok
    }

    suspend fun delete(file: PdfFile): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching { context.contentResolver.delete(file.uri, null, null) > 0 }.getOrDefault(false)
        if (ok) dao.delete(file.key)
        ok
    }

    suspend fun scanDevice(): List<PdfFile> = withContext(Dispatchers.IO) {
        if (!hasStorageAccess()) return@withContext emptyList()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("application/pdf", "%.pdf")
        val favorites = HashSet<String>()
        val result = ArrayList<PdfFile>()
        runCatching {
            context.contentResolver.query(collection, projection, selection, args, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val name = c.getString(nameCol) ?: continue
                    val size = c.getLong(sizeCol)
                    if (size <= 0) continue
                    val uri = ContentUris.withAppendedId(collection, c.getLong(idCol))
                    result += PdfFile(uri, name, size, 0, c.getLong(dateCol) * 1000)
                }
            }
        }
        result.forEach { f -> dao.get(f.key)?.takeIf { it.isFavorite }?.let { favorites += it.uri } }
        result.map { if (it.key in favorites) it.copy(isFavorite = true) else it }
    }

    private fun DocumentEntity.toModel(date: Long) = PdfFile(
        uri = Uri.parse(uri), name = name, size = size, pageCount = pageCount, date = date,
        isFavorite = isFavorite, isCreated = isCreated, lastPage = lastPage,
    )
}
