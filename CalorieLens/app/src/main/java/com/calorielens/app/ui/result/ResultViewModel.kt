package com.calorielens.app.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.db.MealEntity
import com.calorielens.app.data.db.MealType
import com.calorielens.app.data.repo.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: MealRepository,
) : ViewModel() {

    private val id: Long = savedState.get<Long>("id") ?: 0L
    val fresh: Boolean = savedState.get<Boolean>("fresh") ?: false

    /** Shown once per screen instance so returning to it does not repeat the confirmation. */
    var announced: Boolean = false

    val meal: StateFlow<MealEntity?> = repo.meal(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun switchTo(label: String) {
        val current = meal.value ?: return
        viewModelScope.launch { repo.switchTo(current, label) }
    }

    fun setMealType(type: MealType) {
        val current = meal.value ?: return
        viewModelScope.launch { repo.setMealType(current, type) }
    }

    fun delete(onDeleted: (MealEntity) -> Unit) {
        val current = meal.value ?: return
        viewModelScope.launch {
            repo.deleteRecord(current)
            onDeleted(current)
        }
    }

    fun undoDelete(meal: MealEntity) = repo.restore(meal)

    fun finalizeDelete(meal: MealEntity) = repo.purgePhotoIfDeleted(meal)
}
