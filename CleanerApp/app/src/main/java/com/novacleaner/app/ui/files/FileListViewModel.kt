package com.novacleaner.app.ui.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.data.ApkStatus
import com.novacleaner.app.data.FileEntry
import com.novacleaner.app.data.FileKind
import com.novacleaner.app.data.FileOps
import com.novacleaner.app.data.FileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FileListMode { LARGE, APK }

enum class SizeFilter(val label: String, val bytes: Long) {
    MB10("10 MB+", 10L shl 20), MB50("50 MB+", 50L shl 20), MB100("100 MB+", 100L shl 20), MB500("500 MB+", 500L shl 20), GB1("1 GB+", 1L shl 30)
}

enum class SortMode(val label: String) { SIZE("Largest first"), DATE("Newest first"), NAME("Name (A–Z)") }

enum class ApkFilter(val label: String) { ALL("All"), INSTALLED("Already installed"), NOT_INSTALLED("Not installed") }

data class FileListState(
    val loading: Boolean = true,
    val all: List<FileEntry> = emptyList(),
    val sizeFilter: SizeFilter = SizeFilter.MB50,
    val kindFilter: FileKind? = null,
    val apkFilter: ApkFilter = ApkFilter.ALL,
    val sort: SortMode = SortMode.SIZE,
    val selected: Set<String> = emptySet(),
    val deleting: Boolean = false,
    val deleteProgress: Float = 0f,
    val freedBytes: Long = 0L,
    val freedCount: Int = 0,
    val done: Boolean = false,
) {
    fun visible(mode: FileListMode): List<FileEntry> {
        val filtered = when (mode) {
            FileListMode.LARGE -> all.filter { it.size >= sizeFilter.bytes && (kindFilter == null || it.kind == kindFilter) }
            FileListMode.APK -> all.filter {
                val status = it.apk?.status
                when (apkFilter) {
                    ApkFilter.ALL -> true
                    ApkFilter.INSTALLED -> status == ApkStatus.INSTALLED || status == ApkStatus.OUTDATED
                    ApkFilter.NOT_INSTALLED -> status == null || status == ApkStatus.NOT_INSTALLED || status == ApkStatus.NEWER || status == ApkStatus.DAMAGED
                }
            }
        }
        return when (sort) {
            SortMode.SIZE -> filtered.sortedByDescending { it.size }
            SortMode.DATE -> filtered.sortedByDescending { it.modified }
            SortMode.NAME -> filtered.sortedBy { (it.apk?.label ?: it.name).lowercase() }
        }
    }

    val selectedSize: Long get() = all.filter { it.path in selected }.sumOf { it.size }
}

class FileListViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(FileListState())
    val state: StateFlow<FileListState> = _state.asStateFlow()
    private var mode: FileListMode? = null

    fun start(mode: FileListMode) {
        if (this.mode != null) return
        this.mode = mode
        val context = getApplication<Application>()
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val files = when (mode) {
                FileListMode.LARGE -> FileRepository.largeFiles(context, SizeFilter.MB10.bytes)
                FileListMode.APK -> FileRepository.apkFiles(context)
            }
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_SCAN_MS) delay(MIN_SCAN_MS - elapsed)
            val defaultFilter = when {
                mode == FileListMode.APK -> SizeFilter.MB10
                files.count { it.size >= SizeFilter.MB50.bytes } >= 3 -> SizeFilter.MB50
                else -> SizeFilter.MB10
            }
            val preselected = if (mode == FileListMode.APK) {
                files.filter { it.apk?.status == ApkStatus.INSTALLED || it.apk?.status == ApkStatus.OUTDATED }.map { it.path }.toSet()
            } else emptySet()
            _state.update { it.copy(loading = false, all = files, sizeFilter = defaultFilter, selected = preselected) }
        }
    }

    fun setSizeFilter(f: SizeFilter) = _state.update { it.copy(sizeFilter = f) }
    fun setKindFilter(k: FileKind?) = _state.update { it.copy(kindFilter = k) }
    fun setApkFilter(f: ApkFilter) = _state.update { it.copy(apkFilter = f) }
    fun setSort(s: SortMode) = _state.update { it.copy(sort = s) }

    fun toggle(path: String) = _state.update {
        it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path)
    }

    fun toggleAll(mode: FileListMode) = _state.update { s ->
        val visible = s.visible(mode).map { it.path }
        val all = visible.isNotEmpty() && visible.all { it in s.selected }
        s.copy(selected = if (all) s.selected - visible.toSet() else s.selected + visible)
    }

    fun delete() {
        val s = _state.value
        if (s.selected.isEmpty() || s.deleting) return
        val context = getApplication<Application>()
        _state.update { it.copy(deleting = true, deleteProgress = 0f) }
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val result = FileOps.delete(context, s.selected) { p -> _state.update { it.copy(deleteProgress = p) } }
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_DELETE_MS) delay(MIN_DELETE_MS - elapsed)
            AppGraph.prefs.recordClean(result.freedBytes)
            _state.update {
                it.copy(
                    deleting = false,
                    done = true,
                    freedBytes = result.freedBytes,
                    freedCount = result.deletedCount,
                    all = it.all.filterNot { f -> f.path in s.selected },
                    selected = emptySet(),
                )
            }
        }
    }

    private companion object {
        const val MIN_SCAN_MS = 2_200L
        const val MIN_DELETE_MS = 1_800L
    }
}
