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
        "Sharpening small text",
        "Finding seals, signatures & logos",
        "Matching fonts by rendering",
        "Verifying bold, italic & every word",
        "Rebuilding layout & colors",
        "Writing PDF",
    )

    suspend fun convert(context: Context, uri: Uri, progress: (Float, Int) -> Unit): ConversionResult =
        withContext(Dispatchers.Default) {
            progress(0.03f, 0)
            var bitmap = ImageLoader.load(context, uri, minSide = 1800, maxSide = 3200)

            progress(0.08f, 1)
            var text = recognize(bitmap)

            // Small print is read far better when enlarged: aim for ~44 px tall lines.
            progress(0.16f, 2)
            val heights = text.textBlocks.flatMap { it.lines }.mapNotNull { it.boundingBox?.height()?.toFloat() }
            val median = heights.median()
            val longSide = maxOf(bitmap.width, bitmap.height)
            if (!median.isNaN() && median < 30f && longSide < 3600) {
                val target = (longSide * (44f / median)).toInt().coerceAtMost(3600)
                if (target > longSide * 1.15f) {
                    bitmap.recycle()
                    bitmap = ImageLoader.load(context, uri, minSide = target, maxSide = target)
                    text = recognize(bitmap)
                }
            }
            val px = Pixels.of(bitmap)
            bitmap.recycle()
            val allLines = text.textBlocks.flatMap { it.lines }
            if (allLines.isEmpty()) error("No text found in this image. Try a clearer, well-lit photo.")

            // Seals, stamps and signatures: OCR either can't read them (low confidence), the
            // "text" sits inside a larger drawn mark, or it is coloured stamp ink on a mark.
            // Those stay pixel-exact graphics instead of being retyped.
            progress(0.26f, 3)
            val allMask = TextAnalyzer.textMask(px, allLines)
            val firstBg = BackgroundBuilder.build(px, allMask)
            val marks = GraphicsExtractor.regions(px, allMask, firstBg)
            val textLines = allLines.filter { line ->
                val box = line.boundingBox ?: return@filter false
                val area = box.width().toFloat() * box.height()
                val insideMark = marks.any { m ->
                    m.width().toFloat() * m.height() > area * 2.5f && GraphicsExtractor.overlap(box, m) > 0.5f
                }
                val stampInk = marks.any { GraphicsExtractor.overlap(box, it) > 0.2f } &&
                    isStampColor(InkAnalyzer.analyze(px, box).color)
                line.confidence >= 0.45f && !insideMark && !stampInk
            }

            val lines = TextAnalyzer.analyze(px, textLines) { p ->
                progress(0.34f + 0.42f * p, if (p < 0.45f) 4 else 5)
            }

            progress(0.8f, 6)
            val bg = BackgroundBuilder.build(px, TextAnalyzer.textMask(px, textLines))
            val inkMask = TextAnalyzer.textInkMask(px, lines)
            val graphics = GraphicsExtractor.cut(px, inkMask, bg, GraphicsExtractor.regions(px, inkMask, bg))

            progress(0.92f, 7)
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
            val fonts = lines.flatMap { l ->
                l.words.filter { !it.exact }.map { w -> FontUsage(l.family, w.weight, w.italic, (l.fontSize * k).roundToInt(), 1) }
            }
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

    /** Saturated blue, red, violet or green ink: typical of stamps and seals, not body text. */
    private fun isStampColor(c: Int): Boolean {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(c, hsv)
        return hsv[1] > 0.35f && hsv[2] > 0.25f
    }

    private suspend fun recognize(bitmap: Bitmap): Text = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
            .addOnCompleteListener { recognizer.close() }
    }
}
