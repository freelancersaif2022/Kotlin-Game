package com.calorielens.app.ui.scan

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.prefs.UserPreferences
import com.calorielens.app.data.repo.MealRepository
import com.calorielens.app.data.storage.ImageStore
import com.calorielens.app.ml.FoodRecognizer
import com.calorielens.app.ml.RecognitionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ScanState {
    data object Analyzing : ScanState
    data class Done(val mealId: Long, val name: String, val calories: Int) : ScanState
    data object NoFood : ScanState
    data class Error(val message: String) : ScanState
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val recognizer: FoodRecognizer,
    private val images: ImageStore,
    private val repo: MealRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Analyzing)
    val state: StateFlow<ScanState> = _state.asStateFlow()
    private var startedFor: String? = null

    fun start(path: String) {
        if (startedFor == path) return
        startedFor = path
        _state.value = ScanState.Analyzing
        viewModelScope.launch {
            val began = SystemClock.elapsedRealtime()
            val result = runCatching { recognizer.recognize(images.loadForAnalysis(path)) }
                .getOrElse { RecognitionResult.Failed(it.message ?: "Couldn't read the photo") }
            // Keep the analysis animation on screen long enough to read.
            val elapsed = SystemClock.elapsedRealtime() - began
            if (elapsed < MIN_ANALYSIS_MS) delay(MIN_ANALYSIS_MS - elapsed)
            _state.value = when (result) {
                is RecognitionResult.Food -> {
                    val id = repo.saveScan(path, result.best, result.alternatives)
                    prefs.recordScan()
                    val meal = repo.get(id)
                    ScanState.Done(id, meal?.name.orEmpty(), meal?.calories ?: 0)
                }
                RecognitionResult.NoFood -> {
                    images.delete(path)
                    ScanState.NoFood
                }
                is RecognitionResult.Failed -> {
                    images.delete(path)
                    ScanState.Error(result.message)
                }
            }
        }
    }

    private companion object {
        const val MIN_ANALYSIS_MS = 2_600L
    }
}
