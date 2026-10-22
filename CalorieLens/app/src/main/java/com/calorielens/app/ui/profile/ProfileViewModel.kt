package com.calorielens.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.data.repo.MealRepository
import com.calorielens.app.domain.CalorieCalculator
import com.calorielens.app.domain.ThemeMode
import com.calorielens.app.domain.UserProfile
import com.calorielens.app.domain.WeightUnit
import com.calorielens.app.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val profile: UserProfile = UserProfile(),
    val totalScans: Int = 0,
    val mealCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val repo: MealRepository,
    private val premium: PremiumManager,
) : ViewModel() {

    val state: StateFlow<ProfileState> = combine(prefs.profile, prefs.totalScans, repo.mealCount()) { p, scans, meals ->
        ProfileState(p, scans, meals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState())

    fun saveProfile(profile: UserProfile, onSaved: (Int) -> Unit) {
        viewModelScope.launch {
            prefs.saveProfile(profile)
            onSaved(CalorieCalculator.targets(profile).calories)
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { prefs.setWeightUnit(unit) }
    }

    fun cancelPremium(onDone: () -> Unit) {
        viewModelScope.launch {
            premium.cancel()
            onDone()
        }
    }

    fun eraseEverything() {
        viewModelScope.launch {
            repo.deleteEverything()
            prefs.resetAll()
        }
    }
}
