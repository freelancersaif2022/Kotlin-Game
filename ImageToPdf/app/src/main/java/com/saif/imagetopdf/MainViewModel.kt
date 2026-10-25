package com.saif.imagetopdf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saif.imagetopdf.engine.ConversionResult
import com.saif.imagetopdf.engine.Converter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Splash : UiState
    data object Home : UiState
    data class Processing(val image: Uri, val progress: Float, val stage: String) : UiState
    data class Done(val image: Uri, val result: ConversionResult) : UiState
    data class Failed(val message: String) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<UiState>(UiState.Splash)
    val state: StateFlow<UiState> = _state

    fun splashFinished() {
        if (_state.value == UiState.Splash) _state.value = UiState.Home
    }

    fun convert(image: Uri) {
        _state.value = UiState.Processing(image, 0f, "Starting")
        viewModelScope.launch {
            _state.value = try {
                val result = Converter.convert(getApplication(), image) { p, stage ->
                    _state.value = UiState.Processing(image, p, stage)
                }
                UiState.Done(image, result)
            } catch (t: Throwable) {
                UiState.Failed(t.message ?: "Conversion failed")
            }
        }
    }

    fun reset() {
        _state.value = UiState.Home
    }
}
