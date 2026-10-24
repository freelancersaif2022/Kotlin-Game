package com.pdfmaster.app.ui.editor

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaster.app.pdf.PageMark
import com.pdfmaster.app.pdf.PdfDocumentSession
import com.pdfmaster.app.pdf.PdfErrors
import com.pdfmaster.app.ui.viewer.DocumentLoader
import com.pdfmaster.app.ui.viewer.LoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface EditorLoad {
    data object Loading : EditorLoad
    data class Ready(val session: PdfDocumentSession) : EditorLoad
    data class Error(val message: String, val locked: Boolean = false) : EditorLoad
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val loader: DocumentLoader,
    val signatures: SignatureStore,
) : ViewModel() {
    private val _load = MutableStateFlow<EditorLoad>(EditorLoad.Loading)
    val load: StateFlow<EditorLoad> = _load

    val marks = mutableStateListOf<PageMark>()
    private val redoStack = ArrayList<PageMark>()
    val canRedo get() = redoStack.isNotEmpty()

    private var uri: Uri? = null
    private var session: PdfDocumentSession? = null
    private var localCopy: File? = null

    fun open(target: Uri) {
        if (uri == target) return
        uri = target
        viewModelScope.launch {
            _load.value = try {
                when (val r = loader.open(target)) {
                    is LoadResult.Ready -> {
                        session = r.session
                        localCopy = r.localCopy
                        EditorLoad.Ready(r.session)
                    }
                    LoadResult.NeedsPassword -> EditorLoad.Error("This PDF is password protected. Unlock it first, then you can edit and sign it.", locked = true)
                }
            } catch (t: Throwable) {
                EditorLoad.Error(PdfErrors.friendly(t))
            }
        }
    }

    fun add(mark: PageMark) {
        marks.add(mark)
        redoStack.clear()
    }

    fun replace(old: PageMark, new: PageMark) {
        val i = marks.indexOf(old)
        if (i >= 0) marks[i] = new
    }

    fun remove(mark: PageMark) {
        marks.remove(mark)
    }

    fun undo() {
        if (marks.isNotEmpty()) redoStack.add(marks.removeAt(marks.lastIndex))
    }

    fun redo() {
        if (redoStack.isNotEmpty()) marks.add(redoStack.removeAt(redoStack.lastIndex))
    }

    override fun onCleared() {
        session?.close()
        localCopy?.delete()
    }
}
