package com.pdfmaster.app.ui.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaster.app.data.model.PdfFile
import com.pdfmaster.app.data.prefs.AppPreferences
import com.pdfmaster.app.data.repo.FileRepository
import com.pdfmaster.app.data.repo.OutputStorage
import com.pdfmaster.app.pdf.PdfDocumentSession
import com.pdfmaster.app.pdf.PdfErrors
import com.pdfmaster.app.pdf.PdfTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ViewerState {
    data object Loading : ViewerState
    data class NeedsPassword(val wrong: Boolean = false, val checking: Boolean = false) : ViewerState
    data class Ready(val session: PdfDocumentSession, val file: PdfFile, val startPage: Int) : ViewerState
    data class Error(val message: String) : ViewerState
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Done(val query: String, val hits: List<Pair<Int, String>>) : SearchState
}

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val loader: DocumentLoader,
    private val files: FileRepository,
    private val tools: PdfTools,
    private val storage: OutputStorage,
    val prefs: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<ViewerState>(ViewerState.Loading)
    val state: StateFlow<ViewerState> = _state

    private val _search = MutableStateFlow<SearchState>(SearchState.Idle)
    val search: StateFlow<SearchState> = _search

    private var uri: Uri? = null
    private var session: PdfDocumentSession? = null
    private var localCopy: File? = null
    private var searchJob: Job? = null
    var bookmarks: Flow<List<Int>> = emptyFlow()
        private set
    var openCount = 0
        private set

    fun open(target: Uri) {
        if (uri == target) return
        uri = target
        bookmarks = files.bookmarks(target)
        viewModelScope.launch {
            _state.value = ViewerState.Loading
            try {
                when (val r = loader.open(target)) {
                    is LoadResult.Ready -> ready(target, r)
                    LoadResult.NeedsPassword -> _state.value = ViewerState.NeedsPassword()
                }
            } catch (t: Throwable) {
                _state.value = ViewerState.Error(PdfErrors.friendly(t))
            }
        }
    }

    fun submitPassword(password: String) {
        val target = uri ?: return
        viewModelScope.launch {
            _state.value = ViewerState.NeedsPassword(checking = true)
            try {
                ready(target, loader.openWithPassword(target, password))
            } catch (t: Throwable) {
                _state.value = if (DocumentLoader.isWrongPassword(t)) ViewerState.NeedsPassword(wrong = true)
                else ViewerState.Error(PdfErrors.friendly(t))
            }
        }
    }

    private suspend fun ready(target: Uri, r: LoadResult.Ready) {
        session?.close()
        session = r.session
        localCopy = r.localCopy
        val file = files.registerOpened(target, r.session.pageCount)
        openCount = prefs.incrementOpenCount()
        val start = file.lastPage.coerceIn(0, (r.session.pageCount - 1).coerceAtLeast(0))
        _state.value = ViewerState.Ready(r.session, file, start)
    }

    fun saveLastPage(page: Int) {
        val target = uri ?: return
        viewModelScope.launch { files.setLastPage(target, page) }
    }

    fun toggleBookmark(page: Int, add: Boolean) {
        val target = uri ?: return
        viewModelScope.launch { files.toggleBookmark(target, page, add) }
    }

    fun runSearch(query: String) {
        val target = uri ?: return
        searchJob?.cancel()
        if (query.isBlank()) {
            _search.value = SearchState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            _search.value = SearchState.Searching
            val hits = runCatching {
                val file = localCopy ?: storage.copyToWork(target).also { localCopy = it }
                tools.search(file, query.trim())
            }.getOrDefault(emptyList())
            _search.value = SearchState.Done(query.trim(), hits)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchState.Idle
    }

    override fun onCleared() {
        session?.close()
        localCopy?.delete()
    }
}
