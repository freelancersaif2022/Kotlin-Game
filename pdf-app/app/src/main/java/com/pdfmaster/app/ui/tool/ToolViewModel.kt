package com.pdfmaster.app.ui.tool

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfmaster.app.data.model.OutputFile
import com.pdfmaster.app.data.repo.FileMeta
import com.pdfmaster.app.data.repo.FileRepository
import com.pdfmaster.app.data.repo.OutputKind
import com.pdfmaster.app.data.repo.OutputStorage
import com.pdfmaster.app.pdf.OcrEngine
import com.pdfmaster.app.pdf.PdfErrors
import com.pdfmaster.app.pdf.PdfTools
import com.pdfmaster.app.premium.PremiumManager
import com.pdfmaster.app.ui.result.ResultStore
import com.pdfmaster.app.ui.result.ToolResult
import com.pdfmaster.app.ui.tools.PdfTool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface RunState {
    data object Idle : RunState
    data class Running(val title: String, val progress: Float) : RunState
    data class Failed(val message: String) : RunState
    data object LimitReached : RunState
    data object Done : RunState
}

/** Helpers handed to each tool's job. */
class JobScope(private val vm: ToolViewModel, private val title: String) {
    val tools: PdfTools get() = vm.tools
    val storage: OutputStorage get() = vm.storage
    val ocr: OcrEngine get() = vm.ocr

    fun progress(value: Float) = vm.setProgress(title, value)

    suspend fun copy(uri: Uri): File = storage.copyToWork(uri)

    fun work(ext: String = "pdf"): File = storage.newWorkFile(ext)

    suspend fun publishPdf(file: File, name: String): OutputFile {
        val pages = runCatching { tools.pageCount(file) }.getOrDefault(0)
        return storage.publish(file, name, "application/pdf", OutputKind.DOCUMENT, pages)
    }
}

@HiltViewModel
class ToolViewModel @Inject constructor(
    val tools: PdfTools,
    val storage: OutputStorage,
    val files: FileRepository,
    val premium: PremiumManager,
    val ocr: OcrEngine,
    val loader: com.pdfmaster.app.ui.viewer.DocumentLoader,
    private val results: ResultStore,
) : ViewModel() {

    private val _state = MutableStateFlow<RunState>(RunState.Idle)
    val state: StateFlow<RunState> = _state
    private var job: Job? = null

    internal fun setProgress(title: String, value: Float) {
        _state.value = RunState.Running(title, value)
    }

    suspend fun meta(uri: Uri): FileMeta = files.meta(uri)
    suspend fun pageCount(uri: Uri): Int = files.pageCount(uri)

    fun run(tool: PdfTool, title: String, block: suspend JobScope.() -> ToolResult) {
        if (job?.isActive == true) return
        if (tool.metered && !premium.canRunOperation()) {
            _state.value = RunState.LimitReached
            return
        }
        job = viewModelScope.launch {
            _state.value = RunState.Running(title, 0f)
            try {
                val result = JobScope(this@ToolViewModel, title).block()
                files.registerCreated(result.outputs)
                if (tool.metered) premium.recordOperation()
                results.current = result
                _state.value = RunState.Done
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _state.value = RunState.Failed(PdfErrors.friendly(t))
            } finally {
                storage.workDir().listFiles()?.filter { it.name.startsWith("tmp_") && System.currentTimeMillis() - it.lastModified() > 15 * 60_000 }?.forEach { it.delete() }
            }
        }
    }

    fun resultStore(): ResultStore = results

    fun reset() {
        _state.value = RunState.Idle
    }
}
