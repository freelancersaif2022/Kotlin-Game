package com.pdfmaster.app.ui.tool

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.pdfmaster.app.pdf.PdfDocumentSession
import com.pdfmaster.app.ui.viewer.LoadResult
import kotlinx.coroutines.launch

@Stable
class SinglePdfState internal constructor(private val launch: () -> Unit) {
    var picked by mutableStateOf<PickedPdf?>(null)
        internal set
    var loading by mutableStateOf(false)
        internal set

    fun pick() = launch()
}

suspend fun ToolViewModel.load(uri: Uri): PickedPdf {
    val meta = meta(uri)
    return PickedPdf(uri, meta.name, meta.size, pageCount(uri))
}

@Composable
fun rememberSinglePdf(vm: ToolViewModel, initialUri: Uri?): SinglePdfState {
    val scope = rememberCoroutineScope()
    var state: SinglePdfState? = null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val s = state ?: return@rememberLauncherForActivityResult
        if (uri != null) scope.launch {
            s.loading = true
            s.picked = vm.load(uri)
            s.loading = false
        }
    }
    val result = remember { SinglePdfState { launcher.launch(arrayOf("application/pdf")) } }
    state = result
    LaunchedEffect(initialUri) {
        if (initialUri != null && result.picked == null) {
            result.loading = true
            result.picked = runCatching { vm.load(initialUri) }.getOrNull()
            result.loading = false
        }
    }
    return result
}

/** Opens a render session for previews; closed automatically when the uri changes or leaves composition. */
@Composable
fun rememberPreviewSession(vm: ToolViewModel, uri: Uri?): PdfDocumentSession? {
    var session by remember(uri) { mutableStateOf<PdfDocumentSession?>(null) }
    LaunchedEffect(uri) {
        if (uri != null) {
            session = runCatching { (vm.loader.open(uri) as? LoadResult.Ready)?.session }.getOrNull()
        }
    }
    DisposableEffect(uri) {
        onDispose { session?.close() }
    }
    return session
}
