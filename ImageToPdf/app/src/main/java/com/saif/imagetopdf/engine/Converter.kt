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

    /** Stages shown on the processing screen, in order. */
    val STAGES = listOf(
        "Reading image",
        "Recognizing text",
        "Finding seals, signatures & logos",
        "Detecting fonts, weights & colors",
        "Rebuilding layout",
        "Writing PDF",
    )

    suspend fun convert(context: Context, uri: Uri, progress: (Float, Int) -> Unit): ConversionResult =
        withContext(Dispatchers.Default) {
            progress(0.04f, 0)
            val bitmap = ImageLoader.load(context, uri)

            progress(0.12f, 1)
            val text = recognize(bitmap)
            val px = Pixels.of(bitmap)
            bitmap.recycle()
            val allLines = text.textBlocks.flatMap { it.lines }
            if (allLines.isEmpty()) error("No text found in this image. Try a clearer, well-lit photo.")

            // Seals, stamps and signatures: OCR either can't read them (low confidence) or the
            // "text" sits inside a larger drawn mark. Those stay pixel-exact graphics instead.
            progress(0.42f, 2)
            val firstBg = BackgroundBuilder.build(px, TextAnalyzer.textMask(px, allLines))
            val marks = GraphicsExtractor.regions(px, TextAnalyzer.textMask(px, allLines), firstBg)
            val textLines = allLines.filter { line ->
                val box = line.boundingBox ?: return@filter false
                val area = box.width().toFloat() * box.height()
                val insideMark = marks.any { m ->
                    m.width().toFloat() * m.height() > area * 2.5f && GraphicsExtractor.overlap(box, m) > 0.5f
                }
                line.confidence >= 0.45f && !insideMark
            }
            val textMask = TextAnalyzer.textMask(px, textLines)

            progress(0.58f, 3)
            val lines = TextAnalyzer.analyze(px, textLines)

            progress(0.74f, 4)
            val bg = BackgroundBuilder.build(px, textMask)
            val graphics = GraphicsExtractor.cut(px, textMask, bg, GraphicsExtractor.regions(px, textMask, bg))

            progress(0.9f, 5)
            val model = PageModel(px.w, px.h, bg.pageColor, bg.shapes + bg.rules, graphics, lines)
            val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "converted_${System.currentTimeMillis()}.pdf")
            try {
                PdfBuilder.write(model, file)
            } finally {
                graphics.forEach { it.bitmap.recycle() }
            }

            val k = PdfBuilder.scale(model)
            val fonts = lines.flatMap { l -> l.words.map { w -> FontUsage(l.family, w.weight, w.italic, (l.fontSize * k).roundToInt(), 1) } }
                .groupBy { Triple(it.family, it.weight, it.italic) to it.sizePt }
                .map { (_, list) -> list.first().copy(words = list.size) }
                .sortedByDescending { it.words }

            progress(1f, STAGES.size)
            ConversionResult(
                pdfPath = file.absolutePath,
                lineCount = lines.size,
                wordCount = lines.sumOf { it.words.size },
                shapeCount = model.shapes.size,
                graphicCount = graphics.size,
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
