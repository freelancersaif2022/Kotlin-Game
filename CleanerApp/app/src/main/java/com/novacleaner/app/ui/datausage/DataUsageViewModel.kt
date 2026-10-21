package com.novacleaner.app.ui.datausage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.data.AppDataUsage
import com.novacleaner.app.data.DailyUsage
import com.novacleaner.app.data.DataUsageRepository
import com.novacleaner.app.data.UsagePeriod
import com.novacleaner.app.data.UsageTotals
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UsageSort(val label: String) { TOTAL("Total"), MOBILE("Mobile"), WIFI("Wi-Fi") }

data class DataUsageState(
    val loading: Boolean = true,
    val period: UsagePeriod = UsagePeriod.TODAY,
    val totals: UsageTotals = UsageTotals(0, 0),
    val monthMobile: Long = 0L,
    val apps: List<AppDataUsage> = emptyList(),
    val daily: List<DailyUsage> = emptyList(),
    val sort: UsageSort = UsageSort.TOTAL,
) {
    val sortedApps: List<AppDataUsage>
        get() = when (sort) {
            UsageSort.TOTAL -> apps.sortedByDescending { it.total }
            UsageSort.MOBILE -> apps.filter { it.mobile > 0 }.sortedByDescending { it.mobile }
            UsageSort.WIFI -> apps.filter { it.wifi > 0 }.sortedByDescending { it.wifi }
        }
}

class DataUsageViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(DataUsageState())
    val state: StateFlow<DataUsageState> = _state.asStateFlow()
    val limitMb = AppGraph.prefs.settings
    private var started = false

    fun start() {
        if (started) return
        started = true
        load(_state.value.period)
    }

    fun setPeriod(period: UsagePeriod) {
        _state.update { it.copy(period = period) }
        load(period)
    }

    fun setSort(sort: UsageSort) = _state.update { it.copy(sort = sort) }

    fun setLimit(mb: Long) = viewModelScope.launch { AppGraph.prefs.setDataLimitMb(mb) }

    private fun load(period: UsagePeriod) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val (start, end) = DataUsageRepository.range(period)
            val (monthStart, _) = DataUsageRepository.range(UsagePeriod.MONTH)
            val totals = async { DataUsageRepository.totals(context, start, end) }
            val month = async { DataUsageRepository.totals(context, monthStart, end) }
            val apps = async { DataUsageRepository.perApp(context, start, end) }
            val daily = async { DataUsageRepository.daily(context, 7) }
            _state.update {
                it.copy(
                    loading = false,
                    totals = totals.await(),
                    monthMobile = month.await().mobile,
                    apps = apps.await(),
                    daily = daily.await(),
                )
            }
        }
    }
}
