package com.calorielens.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.ads.AdsManager
import com.calorielens.app.data.prefs.AppSettings
import com.calorielens.app.data.prefs.ScanQuota
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.ml.FoodRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainState(
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val quota: ScanQuota = ScanQuota(0, 0),
) {
    val canScan: Boolean get() = settings.isPremium || quota.remaining > 0
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: UserPreferences,
    val ads: AdsManager,
    recognizer: FoodRecognizer,
) : ViewModel() {

    val state: StateFlow<MainState> = combine(prefs.settings, prefs.scanQuota) { s, q ->
        MainState(loaded = true, settings = s, quota = q)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainState())

    val isPremium: StateFlow<Boolean> = prefs.settings.map { it.isPremium }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            if (!prefs.currentSettings().isPremium) ads.initialize()
        }
        viewModelScope.launch { recognizer.warmUp() }
    }

    fun grantRewardScans() {
        viewModelScope.launch { prefs.addBonusScans(ScanQuota.REWARD_BONUS_SCANS) }
    }
}
