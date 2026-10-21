package com.novacleaner.app.ui.home

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacleaner.app.core.Permissions
import com.novacleaner.app.data.RamInfo
import com.novacleaner.app.data.StorageCategory
import com.novacleaner.app.data.StorageInfo
import com.novacleaner.app.data.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val storage: StorageInfo? = null,
    val ram: RamInfo? = null,
    val breakdown: Map<StorageCategory, Long>? = null,
    val hasStorageAccess: Boolean = false,
    val batteryLevel: Int = -1,
    val charging: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun refresh() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val storage = StorageRepository.storage(context)
            val hasAccess = Permissions.hasStorage(context)
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.let {
                val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0) l * 100 / s.coerceAtLeast(1) else -1
            } ?: -1
            val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            _state.update {
                it.copy(
                    storage = storage,
                    ram = StorageRepository.ram(context),
                    hasStorageAccess = hasAccess,
                    batteryLevel = level,
                    charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                )
            }
            if (hasAccess) {
                val breakdown = StorageRepository.breakdown(context, storage)
                _state.update { it.copy(breakdown = breakdown) }
            }
        }
    }
}
