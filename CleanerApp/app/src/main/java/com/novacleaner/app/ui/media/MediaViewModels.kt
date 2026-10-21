package com.novacleaner.app.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.core.Format
import com.novacleaner.app.data.DuplicateGroup
import com.novacleaner.app.data.FileOps
import com.novacleaner.app.data.MediaItem
import com.novacleaner.app.data.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared delete/progress bits for the photo tools. */
data class DeleteState(
    val deleting: Boolean = false,
    val progress: Float = 0f,
    val done: Boolean = false,
    val freedBytes: Long = 0L,
    val freedCount: Int = 0,
)

data class DuplicatesState(
    val loading: Boolean = true,
    val progress: Float = 0f,
    val current: String = "",
    val exact: List<DuplicateGroup> = emptyList(),
    val similar: List<DuplicateGroup> = emptyList(),
    val similarTab: Boolean = false,
    val selected: Set<Long> = emptySet(),
    val delete: DeleteState = DeleteState(),
) {
    val groups: List<DuplicateGroup> get() = if (similarTab) similar else exact
    private val allItems: List<MediaItem> get() = (exact + similar).flatMap { it.items }
    val selectedSize: Long get() = allItems.filter { it.id in selected }.distinctBy { it.id }.sumOf { it.size }
}

class DuplicatesViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(DuplicatesState())
    val state: StateFlow<DuplicatesState> = _state.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        val context = getApplication<Application>()
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val result = MediaRepository.findDuplicates(context) { p, name ->
                _state.update { it.copy(progress = p * 0.98f, current = name) }
            }
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_SCAN_MS) delay(MIN_SCAN_MS - elapsed)
            val preselected = result.exact.flatMap { g -> g.items.filter { it.id != g.best.id }.map { it.id } }.toSet()
            _state.update {
                it.copy(
                    loading = false, progress = 1f, exact = result.exact, similar = result.similar, selected = preselected,
                    similarTab = result.exact.isEmpty() && result.similar.isNotEmpty(),
                )
            }
        }
    }

    fun setSimilarTab(similar: Boolean) = _state.update { it.copy(similarTab = similar) }

    fun toggle(id: Long) = _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }

    fun smartSelect(group: DuplicateGroup) = _state.update { s ->
        val ids = group.items.map { it.id }.toSet()
        val others = ids - group.best.id
        val alreadySmart = s.selected.containsAll(others) && group.best.id !in s.selected
        s.copy(selected = if (alreadySmart) s.selected - ids else (s.selected - group.best.id) + others)
    }

    fun delete() {
        val s = _state.value
        if (s.selected.isEmpty() || s.delete.deleting) return
        val items = (s.exact + s.similar).flatMap { it.items }.filter { it.id in s.selected }.distinctBy { it.id }
        val context = getApplication<Application>()
        _state.update { it.copy(delete = DeleteState(deleting = true)) }
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val result = FileOps.delete(context, items.map { it.path }) { p -> _state.update { it.copy(delete = it.delete.copy(progress = p)) } }
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < 1_800L) delay(1_800L - elapsed)
            AppGraph.prefs.recordClean(result.freedBytes)
            _state.update { it.copy(delete = DeleteState(done = true, freedBytes = result.freedBytes, freedCount = result.deletedCount)) }
        }
    }

    private companion object {
        const val MIN_SCAN_MS = 2_500L
    }
}

data class ScreenshotMonth(val key: String, val title: String, val items: List<MediaItem>)

data class ScreenshotsState(
    val loading: Boolean = true,
    val months: List<ScreenshotMonth> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val delete: DeleteState = DeleteState(),
) {
    val all: List<MediaItem> get() = months.flatMap { it.items }
    val totalSize: Long get() = all.sumOf { it.size }
    val selectedSize: Long get() = all.filter { it.id in selected }.sumOf { it.size }
}

class ScreenshotsViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ScreenshotsState())
    val state: StateFlow<ScreenshotsState> = _state.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        val context = getApplication<Application>()
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val shots = MediaRepository.screenshots(context)
            val months = shots.groupBy { Format.monthYear(it.dateTaken) }
                .map { (title, items) -> ScreenshotMonth(title, title, items) }
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < 2_000L) delay(2_000L - elapsed)
            _state.update { it.copy(loading = false, months = months) }
        }
    }

    fun toggle(id: Long) = _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }

    fun toggleMonth(month: ScreenshotMonth) = _state.update { s ->
        val ids = month.items.map { it.id }.toSet()
        s.copy(selected = if (s.selected.containsAll(ids)) s.selected - ids else s.selected + ids)
    }

    fun toggleAll() = _state.update { s ->
        val ids = s.all.map { it.id }.toSet()
        s.copy(selected = if (ids.isNotEmpty() && s.selected.containsAll(ids)) emptySet() else ids)
    }

    fun delete() {
        val s = _state.value
        if (s.selected.isEmpty() || s.delete.deleting) return
        val items = s.all.filter { it.id in s.selected }
        val context = getApplication<Application>()
        _state.update { it.copy(delete = DeleteState(deleting = true)) }
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val result = FileOps.delete(context, items.map { it.path }) { p -> _state.update { it.copy(delete = it.delete.copy(progress = p)) } }
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < 1_800L) delay(1_800L - elapsed)
            AppGraph.prefs.recordClean(result.freedBytes)
            _state.update { it.copy(delete = DeleteState(done = true, freedBytes = result.freedBytes, freedCount = result.deletedCount)) }
        }
    }
}
