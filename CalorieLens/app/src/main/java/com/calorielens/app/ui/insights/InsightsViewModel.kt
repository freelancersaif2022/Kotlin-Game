package com.calorielens.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.data.repo.DayTotals
import com.calorielens.app.data.repo.MealRepository
import com.calorielens.app.domain.CalorieCalculator
import com.calorielens.app.domain.DailyTargets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class RangeStats(
    val days: List<DayTotals> = emptyList(),
) {
    private val logged get() = days.filter { it.meals > 0 }
    val avgCalories: Int get() = if (logged.isEmpty()) 0 else logged.sumOf { it.calories } / logged.size
    val avgProtein: Float get() = if (logged.isEmpty()) 0f else logged.sumOf { it.protein.toDouble() }.toFloat() / logged.size
    val avgCarbs: Float get() = if (logged.isEmpty()) 0f else logged.sumOf { it.carbs.toDouble() }.toFloat() / logged.size
    val avgFat: Float get() = if (logged.isEmpty()) 0f else logged.sumOf { it.fat.toDouble() }.toFloat() / logged.size
    val totalMeals: Int get() = days.sumOf { it.meals }
    val loggedDays: Int get() = logged.size
    fun onTargetDays(goal: Int): Int = logged.count { it.calories in (goal * 0.85).toInt()..(goal * 1.1).toInt() }
}

data class InsightsState(
    val week: RangeStats = RangeStats(),
    val month: RangeStats = RangeStats(),
    val targets: DailyTargets = DailyTargets(2000, 150, 200, 65),
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repo: MealRepository,
    prefs: UserPreferences,
) : ViewModel() {
    private val today = LocalDate.now()

    val state: StateFlow<InsightsState> = combine(
        repo.dailyTotals(today.minusDays(6), today),
        repo.dailyTotals(today.minusDays(29), today),
        prefs.profile,
    ) { week, month, profile ->
        InsightsState(RangeStats(week), RangeStats(month), CalorieCalculator.targets(profile))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())
}
