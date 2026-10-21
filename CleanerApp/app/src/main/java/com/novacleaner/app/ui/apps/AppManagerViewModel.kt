package com.novacleaner.app.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacleaner.app.core.Permissions
import com.novacleaner.app.data.AppInfo
import com.novacleaner.app.data.AppsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class AppSort(val label: String) {
    SIZE("Size"), NAME("Name"), INSTALLED("Install date"), UPDATED("Last updated"), LAST_USED("Last used"), CACHE("Cache size")
}

enum class AppFilter(val label: String) { USER("Installed"), UNUSED("Unused 30+ days"), SYSTEM("System") }

data class AppsState(
    val loading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val sort: AppSort = AppSort.SIZE,
    val filter: AppFilter = AppFilter.USER,
    val query: String = "",
    val searching: Boolean = false,
    val selected: Set<String> = emptySet(),
    val hasUsage: Boolean = false,
    val detail: AppInfo? = null,
    val uninstallQueue: List<String> = emptyList(),
    val uninstalledBytes: Long = 0L,
    val uninstalledCount: Int = 0,
    val done: Boolean = false,
) {
    val visible: List<AppInfo>
        get() {
            val now = System.currentTimeMillis()
            val filtered = apps.filter { app ->
                val matchesFilter = when (filter) {
                    AppFilter.USER -> !app.isSystem
                    AppFilter.SYSTEM -> app.isSystem
                    AppFilter.UNUSED -> !app.isSystem && app.lastUsed > 0 && now - app.lastUsed > TimeUnit.DAYS.toMillis(30)
                }
                matchesFilter && (query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true))
            }
            return when (sort) {
                AppSort.SIZE -> filtered.sortedByDescending { it.totalBytes }
                AppSort.NAME -> filtered.sortedBy { it.label.lowercase() }
                AppSort.INSTALLED -> filtered.sortedByDescending { it.installTime }
                AppSort.UPDATED -> filtered.sortedByDescending { it.updateTime }
                AppSort.LAST_USED -> filtered.sortedBy { if (it.lastUsed == 0L) Long.MAX_VALUE else it.lastUsed }
                AppSort.CACHE -> filtered.sortedByDescending { it.cacheBytes }
            }
        }

    val selectedSize: Long get() = apps.filter { it.packageName in selected }.sumOf { it.totalBytes }
}

class AppManagerViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        load(initial = true)
    }

    fun refreshIfNeeded() {
        val context = getApplication<Application>()
        val hasUsage = Permissions.hasUsageAccess(context)
        if (started && hasUsage != _state.value.hasUsage) load(initial = false)
    }

    private fun load(initial: Boolean) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            if (initial) _state.update { it.copy(loading = true) }
            val apps = AppsRepository.load(context)
            val elapsed = System.currentTimeMillis() - startedAt
            if (initial && elapsed < 1_200L) delay(1_200L - elapsed)
            _state.update {
                it.copy(
                    loading = false,
                    apps = apps,
                    hasUsage = Permissions.hasUsageAccess(context),
                    selected = it.selected.filter { p -> apps.any { a -> a.packageName == p } }.toSet(),
                )
            }
        }
    }

    fun setSort(sort: AppSort) = _state.update { it.copy(sort = sort) }
    fun setFilter(filter: AppFilter) = _state.update { it.copy(filter = filter, selected = emptySet()) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun toggleSearch() = _state.update { it.copy(searching = !it.searching, query = "") }
    fun toggle(pkg: String) = _state.update { it.copy(selected = if (pkg in it.selected) it.selected - pkg else it.selected + pkg) }
    fun showDetail(app: AppInfo?) = _state.update { it.copy(detail = app) }

    fun startUninstall(packages: Collection<String>) = _state.update {
        it.copy(uninstallQueue = packages.toList(), uninstalledBytes = 0L, uninstalledCount = 0, detail = null)
    }

    /** Called when the system uninstall dialog for the queue head returns. */
    fun onUninstallReturned() {
        val context = getApplication<Application>()
        val s = _state.value
        val head = s.uninstallQueue.firstOrNull() ?: return
        val removed = !AppsRepository.isInstalled(context, head)
        val app = s.apps.firstOrNull { it.packageName == head }
        val bytes = if (removed) app?.totalBytes ?: 0L else 0L
        val rest = s.uninstallQueue.drop(1)
        _state.update {
            it.copy(
                uninstallQueue = rest,
                uninstalledBytes = it.uninstalledBytes + bytes,
                uninstalledCount = it.uninstalledCount + if (removed) 1 else 0,
                apps = if (removed) it.apps.filterNot { a -> a.packageName == head } else it.apps,
                selected = it.selected - head,
                done = rest.isEmpty() && (it.uninstalledCount + if (removed) 1 else 0) > 0,
            )
        }
    }
}
