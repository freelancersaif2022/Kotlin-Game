package com.vaultly.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Locale

/** Runs on-device text recognition on a stored receipt image and parses the result. */
class ReceiptReader(private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun read(imagePath: String): ParsedReceipt? = runCatching {
        val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))
        val text = recognizer.process(image).await()
        val fragments = text.textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
            line.boundingBox?.let { TextFragment(line.text, it.left, it.top, it.bottom) }
        }
        // Rebuild rows by position; fall back to ML Kit's own order if boxes are missing.
        val raw = if (fragments.isNotEmpty()) joinRows(fragments).joinToString("\n")
        else text.textBlocks.flatMap { block -> block.lines.map { it.text } }.joinToString("\n")
        val dayFirst = Locale.getDefault().country !in MONTH_FIRST_COUNTRIES
        ReceiptParser(dayFirst).parse(raw)
    }.getOrNull()

    companion object {
        private val MONTH_FIRST_COUNTRIES = setOf("US", "PH", "CA", "FM", "PW", "MH")
    }
}
