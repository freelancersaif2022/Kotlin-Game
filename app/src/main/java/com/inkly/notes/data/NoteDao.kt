package com.inkly.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE uid = :uid")
    suspend fun getByUid(uid: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
