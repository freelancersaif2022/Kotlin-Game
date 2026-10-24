package com.pdfmaster.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE lastOpened > 0 ORDER BY lastOpened DESC LIMIT 100")
    fun observeRecent(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY lastOpened DESC")
    fun observeFavorites(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isCreated = 1 ORDER BY createdAt DESC")
    fun observeCreated(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE uri = :uri")
    suspend fun get(uri: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE uri = :uri")
    fun observe(uri: String): Flow<DocumentEntity?>

    @Upsert
    suspend fun upsert(entity: DocumentEntity)

    @Query("UPDATE documents SET isFavorite = :favorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, favorite: Boolean)

    @Query("UPDATE documents SET lastPage = :page WHERE uri = :uri")
    suspend fun setLastPage(uri: String, page: Int)

    @Query("UPDATE documents SET name = :name WHERE uri = :uri")
    suspend fun rename(uri: String, name: String)

    @Query("UPDATE documents SET lastOpened = 0 WHERE uri = :uri")
    suspend fun removeFromRecent(uri: String)

    @Query("DELETE FROM documents WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("UPDATE documents SET lastOpened = 0 WHERE isCreated = 0 OR isCreated = 1")
    suspend fun clearRecent()

    @Query("SELECT * FROM bookmarks WHERE uri = :uri ORDER BY page")
    fun observeBookmarks(uri: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE uri = :uri AND page = :page")
    suspend fun removeBookmark(uri: String, page: Int)
}
