package com.inkly.notes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes", indices = [Index(value = ["uid"], unique = true)])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable identity across devices / reinstalls, used to merge backups. */
    val uid: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
