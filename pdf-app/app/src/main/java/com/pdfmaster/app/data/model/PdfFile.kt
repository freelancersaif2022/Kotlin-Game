package com.pdfmaster.app.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PdfFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val pageCount: Int,
    val date: Long,
    val isFavorite: Boolean = false,
    val isCreated: Boolean = false,
    val lastPage: Int = 0,
) {
    val key: String get() = uri.toString()
}

@Immutable
data class OutputFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String,
    val pageCount: Int = 0,
) {
    val isPdf: Boolean get() = mime == "application/pdf"
}
