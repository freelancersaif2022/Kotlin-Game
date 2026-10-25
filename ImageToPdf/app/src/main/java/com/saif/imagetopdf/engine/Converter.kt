package com.saif.imagetopdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/** Image → OCR → style detection → layout/shape rebuild → vector PDF. Runs fully on the phone. */
object Converter {

    suspend fun convert(context: Context, uri: Uri, progress: (Float, String) -> Unit): ConversionResult =
        withContext(Dispatchers.Default) {
            progress(0.05f, "Reading image")
            val bitmap = ImageLoader.load(context, uri)

            progress(0.15f, "Recognizing text on-device")
            val text = recognize(bitmap)
            val px = Pixels.of(bitmap)
            bitmap.recycle()
            if (text.textBlocks.isEmpty()) error("No text found in this image. Try a clearer, well-lit photo.")

            progress(0.45f, "Detecting fonts, sizes, weights & colors")
            val lines = TextAnalyzer.analyze(px, text)

            progress(0.7f, "Rebuilding background, boxes & lines")
            val bg = BackgroundBuilder.build(px, TextAnalyzer.textMask(px, text))

            progress(0.9f, "Writing PDF")
            val model = PageModel(px.w, px.h, bg.pageColor, bg.shapes, lines)
            val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "converted_${System.currentTimeMillis()}.pdf")
            PdfBuilder.write(model, file)

            val k = PdfBuilder.scale(model)
            val fonts = lines.flatMap { l -> l.words.map { w -> FontUsage(l.family, w.weight, w.italic, (l.fontSize * k).roundToInt(), 1) } }
                .groupBy { Triple(it.family, it.weight, it.italic) to it.sizePt }
                .map { (_, list) -> list.first().copy(words = list.size) }
                .sortedByDescending { it.words }

            progress(1f, "Done")
            ConversionResult(
                pdfPath = file.absolutePath,
                lineCount = lines.size,
                wordCount = lines.sumOf { it.words.size },
                shapeCount = bg.shapes.size,
                fonts = fonts,
            )
        }

    private suspend fun recognize(bitmap: Bitmap): Text = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
            .addOnCompleteListener { recognizer.close() }
    }
}
