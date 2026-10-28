package com.inkly.notes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkly.notes.data.Note
import com.inkly.notes.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SaveState { Idle, Saving, Saved }

/**
 * Autosaving editor. Edits are debounced and written to the database; pending edits are flushed
 * when the screen stops or closes (on the app scope so they survive the ViewModel).
 */
class EditorViewModel(
    private val repo: NoteRepository,
    private val appScope: CoroutineScope,
    noteId: Long,
) : ViewModel() {
    var title by mutableStateOf(""); private set
    var content by mutableStateOf(""); private set
    var color by mutableIntStateOf(0); private set
    var pinned by mutableStateOf(false); private set
    var updatedAt by mutableLongStateOf(System.currentTimeMillis()); private set
    var saveState by mutableStateOf(SaveState.Idle); private set
    var loaded by mutableStateOf(noteId == 0L); private set
    val isNew = noteId == 0L

    private var current: Note? = null
    private var dirty = false
    private var deleted = false
    private var saveJob: Job? = null
    private val mutex = Mutex()

    init {
        if (noteId != 0L) viewModelScope.launch {
            repo.get(noteId)?.let { n ->
                current = n
                title = n.title; content = n.content; color = n.color; pinned = n.pinned; updatedAt = n.updatedAt
            }
            loaded = true
        }
    }

    fun onTitleChange(v: String) { title = v; markDirty() }
    fun onContentChange(v: String) { content = v; markDirty() }
    fun onColorChange(v: Int) { color = v; markDirty() }
    fun togglePin() { pinned = !pinned; markDirty() }

    private fun markDirty() {
        dirty = true
        saveState = SaveState.Saving
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            save()
        }
    }

    private suspend fun save() = mutex.withLock {
        if (deleted || !dirty) {
            if (saveState == SaveState.Saving) saveState = SaveState.Saved
            return@withLock
        }
        dirty = false
        val now = System.currentTimeMillis()
        val existing = current
        if (existing == null) {
            if (title.isBlank() && content.isBlank()) {
                saveState = SaveState.Idle
                return@withLock
            }
            val n = Note(title = title, content = content, color = color, pinned = pinned, createdAt = now, updatedAt = now)
            current = n.copy(id = repo.insert(n))
        } else {
            val n = existing.copy(title = title, content = content, color = color, pinned = pinned, updatedAt = now)
            repo.update(n)
            current = n
        }
        updatedAt = now
        saveState = SaveState.Saved
    }

    fun flush() {
        saveJob?.cancel()
        appScope.launch { save() }
    }

    fun delete(onDone: () -> Unit) {
        deleted = true
        saveJob?.cancel()
        appScope.launch {
            mutex.withLock { current?.let { repo.delete(it.id) } }
            onDone()
        }
    }

    override fun onCleared() {
        saveJob?.cancel()
        appScope.launch {
            save()
            val c = current
            // An emptied note isn't worth keeping.
            if (!deleted && c != null && title.isBlank() && content.isBlank()) repo.delete(c.id)
        }
    }
}
