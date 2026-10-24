package com.pdfmaster.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val size: Long,
    val pageCount: Int,
    val lastOpened: Long = 0L,
    val isFavorite: Boolean = false,
    val isCreated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPage: Int = 0,
)

@Entity(tableName = "bookmarks", indices = [Index(value = ["uri", "page"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val page: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
