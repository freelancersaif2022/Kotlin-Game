package com.calorielens.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.db.MealEntity
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.data.repo.MealRepository
import com.calorielens.app.domain.CalorieCalculator
import com.calorielens.app.domain.DailyTargets
import com.calorielens.app.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class HomeState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val targets: DailyTargets = DailyTargets(2000, 150, 200, 65),
    val meals: List<MealEntity> = emptyList(),
    val streak: Int = 0,
) {
    val calories: Int get() = meals.sumOf { it.calories }
    val protein: Float get() = meals.sumOf { it.protein.toDouble() }.toFloat()
    val carbs: Float get() = meals.sumOf { it.carbs.toDouble() }.toFloat()
    val fat: Float get() = meals.sumOf { it.fat.toDouble() }.toFloat()
    val remaining: Int get() = targets.calories - calories
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    prefs: UserPreferences,
    private val repo: MealRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val state: StateFlow<HomeState> = combine(
        prefs.profile,
        repo.mealsOn(today),
        repo.dailyTotals(today.minusDays(29), today),
    ) { profile, meals, last30 ->
        var streak = 0
        for (day in last30.reversed()) {
            if (day.meals > 0) streak++ else if (day.date != today) break
        }
        HomeState(
            loading = false,
            profile = profile,
            targets = CalorieCalculator.targets(profile),
            meals = meals,
            streak = streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
