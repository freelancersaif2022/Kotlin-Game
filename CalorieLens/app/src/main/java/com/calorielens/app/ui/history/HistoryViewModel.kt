package com.calorielens.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.db.MealEntity
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.data.repo.DayTotals
import com.calorielens.app.data.repo.MealRepository
import com.calorielens.app.domain.CalorieCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class HistoryState(
    val selected: LocalDate = LocalDate.now(),
    val weekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val week: List<DayTotals> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val goal: Int = 2000,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: MealRepository,
    prefs: UserPreferences,
) : ViewModel() {

    private val selected = MutableStateFlow(LocalDate.now())

    private val weekStart = selected.map { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }

    val state: StateFlow<HistoryState> = combine(
        selected,
        weekStart.flatMapLatest { start -> repo.dailyTotals(start, start.plusDays(6)).map { start to it } },
        selected.flatMapLatest { repo.mealsOn(it) },
        prefs.profile.map { CalorieCalculator.targets(it).calories },
    ) { sel, (start, week), meals, goal ->
        HistoryState(selected = sel, weekStart = start, week = week, meals = meals, goal = goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun select(date: LocalDate) {
        if (!date.isAfter(LocalDate.now())) selected.value = date
    }

    fun shiftWeek(weeks: Long) {
        val target = selected.value.plusWeeks(weeks)
        selected.value = if (target.isAfter(LocalDate.now())) LocalDate.now() else target
    }
}
