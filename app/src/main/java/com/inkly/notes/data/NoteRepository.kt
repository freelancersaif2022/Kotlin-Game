package com.inkly.notes.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

data class MergeResult(val added: Int, val updated: Int)

class NoteRepository(private val db: NoteDatabase) {
    private val dao = db.noteDao()

    fun observeAll(): Flow<List<Note>> = dao.observeAll()
    suspend fun getAll(): List<Note> = dao.getAll()
    suspend fun get(id: Long): Note? = dao.getById(id)
    suspend fun insert(note: Note): Long = dao.insert(note)
    suspend fun update(note: Note) = dao.update(note)
    suspend fun delete(id: Long) = dao.deleteById(id)

    /** Merges backed-up notes into the local database; the newest edit of each note wins. */
    suspend fun merge(incoming: List<Note>): MergeResult = db.withTransaction {
        var added = 0
        var updated = 0
        for (note in incoming) {
            val local = dao.getByUid(note.uid)
            when {
                local == null -> {
                    dao.insert(note.copy(id = 0)); added++
                }
                note.updatedAt > local.updatedAt -> {
                    dao.update(note.copy(id = local.id)); updated++
                }
            }
        }
        MergeResult(added, updated)
    }
}
