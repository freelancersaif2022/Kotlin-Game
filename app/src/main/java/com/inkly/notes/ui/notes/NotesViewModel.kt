package com.inkly.notes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkly.notes.data.Note
import com.inkly.notes.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val repo: NoteRepository) : ViewModel() {
    val query = MutableStateFlow("")

    /** null while the first load is in flight (avoids an empty-state flash). */
    val notes: StateFlow<List<Note>?> = combine(repo.observeAll(), query) { list, q ->
        if (q.isBlank()) list
        else list.filter { it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePin(note: Note) = viewModelScope.launch {
        repo.update(note.copy(pinned = !note.pinned))
    }

    fun delete(note: Note) = viewModelScope.launch { repo.delete(note.id) }

}
