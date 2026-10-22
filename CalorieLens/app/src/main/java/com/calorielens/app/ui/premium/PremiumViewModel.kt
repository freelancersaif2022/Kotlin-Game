package com.calorielens.app.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.prefs.PremiumPlan
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.premium.PlanOffer
import com.calorielens.app.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Processing : PurchaseState
    data object Restoring : PurchaseState
    data class Success(val plan: PremiumPlan) : PurchaseState
    data class RestoreResult(val found: Boolean) : PurchaseState
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val manager: PremiumManager,
    prefs: UserPreferences,
) : ViewModel() {

    val offers: List<PlanOffer> = manager.offers

    private val _selected = MutableStateFlow(PremiumPlan.YEARLY)
    val selected: StateFlow<PremiumPlan> = _selected.asStateFlow()

    private val _purchase = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchase: StateFlow<PurchaseState> = _purchase.asStateFlow()

    val settings = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun select(plan: PremiumPlan) {
        _selected.value = plan
    }

    fun buy() {
        if (_purchase.value != PurchaseState.Idle) return
        _purchase.value = PurchaseState.Processing
        viewModelScope.launch {
            val plan = _selected.value
            manager.purchase(plan)
            _purchase.value = PurchaseState.Success(plan)
        }
    }

    fun restore() {
        if (_purchase.value != PurchaseState.Idle) return
        _purchase.value = PurchaseState.Restoring
        viewModelScope.launch {
            _purchase.value = PurchaseState.RestoreResult(manager.restore())
        }
    }

    fun reset() {
        _purchase.value = PurchaseState.Idle
    }
}
