package com.clonehub.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clonehub.app.data.AppRepository
import com.clonehub.app.data.Clone
import com.clonehub.app.data.CloneStore
import com.clonehub.app.data.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val store = CloneStore(app)

    val clones: StateFlow<List<Clone>> =
        store.clones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _installed = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installed: StateFlow<List<InstalledApp>> = _installed.asStateFlow()

    private val _loadingApps = MutableStateFlow(false)
    val loadingApps: StateFlow<Boolean> = _loadingApps.asStateFlow()

    fun loadInstalledApps() {
        if (_installed.value.isNotEmpty()) return
        viewModelScope.launch {
            _loadingApps.value = true
            _installed.value = withContext(Dispatchers.IO) { repo.installedApps() }
            _loadingApps.value = false
        }
    }

    fun iconFor(packageName: String) = repo.icon(packageName)

    fun isInstalled(packageName: String) = repo.isInstalled(packageName)

    fun addClone(app: InstalledApp, accent: Long) {
        val current = clones.value
        val baseName = cloneName(app.label, current, app.packageName)
        val clone = Clone(
            id = System.currentTimeMillis(),
            packageName = app.packageName,
            label = baseName,
            accent = accent,
            createdAt = System.currentTimeMillis(),
        )
        persist(current + clone)
    }

    fun rename(id: Long, newLabel: String) {
        val trimmed = newLabel.trim()
        if (trimmed.isEmpty()) return
        persist(clones.value.map { if (it.id == id) it.copy(label = trimmed) else it })
    }

    fun recolor(id: Long, accent: Long) {
        persist(clones.value.map { if (it.id == id) it.copy(accent = accent) else it })
    }

    fun delete(id: Long) {
        persist(clones.value.filterNot { it.id == id })
    }

    fun launch(clone: Clone): Boolean = repo.launch(clone.packageName)

    private fun persist(list: List<Clone>) {
        viewModelScope.launch { store.save(list) }
    }

    /** Produces "Label", then "Label (2)", "Label (3)", ... for repeat clones. */
    private fun cloneName(label: String, existing: List<Clone>, pkg: String): String {
        val sameApp = existing.count { it.packageName == pkg }
        return if (sameApp == 0) label else "$label (${sameApp + 1})"
    }
}
