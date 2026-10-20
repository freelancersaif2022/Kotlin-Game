package com.vaultly.app

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class NoteKind { SUCCESS, INFO, ERROR }

data class Note(val message: String, val kind: NoteKind, val id: Long = System.nanoTime())

/** App-wide snackbar messages; any layer can post, the UI shows them in a styled host. */
class Notifier {
    private val _notes = MutableSharedFlow<Note>(extraBufferCapacity = 8)
    val notes: SharedFlow<Note> = _notes.asSharedFlow()

    fun show(message: String, kind: NoteKind = NoteKind.INFO) {
        _notes.tryEmit(Note(message, kind))
    }

    fun success(message: String) = show(message, NoteKind.SUCCESS)
    fun error(message: String) = show(message, NoteKind.ERROR)
}
