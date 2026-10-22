package com.calorielens.app.ui.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorielens.app.data.storage.ImageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(private val images: ImageStore) : ViewModel() {

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    fun newCaptureFile(): File = images.newCaptureFile()

    fun setProcessing(value: Boolean) {
        _processing.value = value
    }

    fun importCapture(file: File, onReady: (String) -> Unit, onError: (String) -> Unit) = runImport(onReady, onError) {
        images.importCapture(file)
    }

    fun importGallery(uri: Uri, onReady: (String) -> Unit, onError: (String) -> Unit) = runImport(onReady, onError) {
        images.importUri(uri)
    }

    private fun runImport(onReady: (String) -> Unit, onError: (String) -> Unit, block: suspend () -> File) {
        _processing.value = true
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { onReady(it.absolutePath) }
                .onFailure { onError(it.message ?: "Couldn't process the photo") }
            _processing.value = false
        }
    }
}
