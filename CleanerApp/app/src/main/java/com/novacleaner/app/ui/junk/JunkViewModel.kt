package com.novacleaner.app.ui.junk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.data.AppsRepository
import com.novacleaner.app.data.FileOps
import com.novacleaner.app.data.JunkCategory
import com.novacleaner.app.data.JunkItem
import com.novacleaner.app.data.JunkScanner
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class JunkPhase { SCANNING, RESULTS, CLEANING, DONE, EMPTY }

data class CategoryResult(val category: JunkCategory, val items: List<JunkItem>) {
    val size: Long = items.sumOf { it.size }
}

data class JunkUiState(
    val phase: JunkPhase = JunkPhase.SCANNING,
    val progress: Float = 0f,
    val currentPath: String = "",
    val scannedFiles: Int = 0,
    val liveFound: Map<JunkCategory, Long> = emptyMap(),
    val categories: List<CategoryResult> = emptyList(),
    val selected: Set<String> = emptySet(),
    val expanded: Set<JunkCategory> = emptySet(),
    val cleanProgress: Float = 0f,
    val freedBytes: Long = 0L,
    val freedCount: Int = 0,
    val systemCache: Long = 0L,
) {
    val totalFound: Long get() = categories.sumOf { it.size }
    val selectedSize: Long
        get() = categories.sumOf { c -> c.items.filter { it.path in selected }.sumOf { it.size } }
    val selectedCount: Int get() = selected.size
}

class JunkViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(JunkUiState())
    val state: StateFlow<JunkUiState> = _state.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        val context = getApplication<Application>()
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val systemCache = async { runCatching { AppsRepository.totalCache(context) }.getOrDefault(0L) }
            var items: List<JunkItem> = emptyList()
            JunkScanner.scan(context).collect { s ->
                _state.update {
                    it.copy(progress = s.progress * 0.97f, currentPath = s.currentPath, scannedFiles = s.scannedFiles, liveFound = s.foundBytes)
                }
                if (s.done) items = s.items
            }
            // Keep the scan on screen long enough to read – feels deliberate, not glitchy.
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_SCAN_MS) {
                val steps = 12
                repeat(steps) { i ->
                    delay((MIN_SCAN_MS - elapsed) / steps)
                    _state.update { it.copy(progress = (it.progress + (1f - it.progress) * (i + 1f) / steps)) }
                }
            }
            val categories = JunkCategory.entries.mapNotNull { cat ->
                val list = items.filter { it.category == cat }.sortedByDescending { it.size }
                if (list.isEmpty()) null else CategoryResult(cat, list)
            }
            _state.update {
                it.copy(
                    progress = 1f,
                    categories = categories,
                    selected = items.filter { item -> item.preselected }.map { item -> item.path }.toSet(),
                    systemCache = systemCache.await(),
                    phase = if (categories.isEmpty()) JunkPhase.EMPTY else JunkPhase.RESULTS,
                )
            }
        }
    }

    fun toggleItem(path: String) = _state.update {
        it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path)
    }

    fun toggleCategory(category: CategoryResult) = _state.update { s ->
        val paths = category.items.map { it.path }
        val allSelected = paths.all { it in s.selected }
        s.copy(selected = if (allSelected) s.selected - paths.toSet() else s.selected + paths)
    }

    fun toggleExpanded(category: JunkCategory) = _state.update {
        it.copy(expanded = if (category in it.expanded) it.expanded - category else it.expanded + category)
    }

    fun clean() {
        val s = _state.value
        if (s.selected.isEmpty() || s.phase != JunkPhase.RESULTS) return
        val context = getApplication<Application>()
        _state.update { it.copy(phase = JunkPhase.CLEANING, cleanProgress = 0f) }
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val expected = s.selectedSize
            val result = FileOps.delete(context, s.selected) { p -> _state.update { it.copy(cleanProgress = p * 0.9f) } }
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_CLEAN_MS) {
                val steps = 10
                repeat(steps) { i ->
                    delay((MIN_CLEAN_MS - elapsed) / steps)
                    _state.update { it.copy(cleanProgress = 0.9f + 0.1f * (i + 1) / steps) }
                }
            }
            val freed = maxOf(result.freedBytes, if (result.failedCount == 0) expected else 0L)
            AppGraph.prefs.recordClean(freed)
            _state.update { it.copy(phase = JunkPhase.DONE, freedBytes = freed, freedCount = result.deletedCount, cleanProgress = 1f) }
        }
    }

    private companion object {
        const val MIN_SCAN_MS = 3_800L
        const val MIN_CLEAN_MS = 2_600L
    }
}
