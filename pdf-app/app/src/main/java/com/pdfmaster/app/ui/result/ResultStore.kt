package com.pdfmaster.app.ui.result

import com.pdfmaster.app.data.model.OutputFile
import com.pdfmaster.app.data.repo.OutputKind
import com.pdfmaster.app.ui.tools.PdfTool
import javax.inject.Inject
import javax.inject.Singleton

data class ToolResult(
    val tool: PdfTool?,
    val title: String,
    val message: String,
    val outputs: List<OutputFile> = emptyList(),
    val kind: OutputKind = OutputKind.DOCUMENT,
    val text: String? = null,
    val before: Long? = null,
    val after: Long? = null,
)

/** Hands the latest finished job to the result screen without passing it through navigation args. */
@Singleton
class ResultStore @Inject constructor() {
    @Volatile
    var current: ToolResult? = null
}
