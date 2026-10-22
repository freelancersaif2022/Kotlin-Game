package com.calorielens.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.CalorieCalculator
import com.calorielens.app.domain.DailyTargets
import com.calorielens.app.domain.Gender
import com.calorielens.app.domain.Goal
import com.calorielens.app.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val prefs: UserPreferences) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    val targets: DailyTargets get() = CalorieCalculator.targets(_profile.value)

    fun setName(v: String) = _profile.update { it.copy(name = v.take(24)) }
    fun setGender(v: Gender) = _profile.update { it.copy(gender = v) }
    fun setAge(v: Int) = _profile.update { it.copy(age = v.coerceIn(13, 90)) }
    fun setHeight(v: Int) = _profile.update { it.copy(heightCm = v.coerceIn(120, 230)) }
    fun setWeight(v: Float) = _profile.update { it.copy(weightKg = v.coerceIn(30f, 250f)) }
    fun setActivity(v: ActivityLevel) = _profile.update { it.copy(activity = v) }
    fun setGoal(v: Goal) = _profile.update { it.copy(goal = v) }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.completeOnboarding(_profile.value)
            onDone()
        }
    }
}
