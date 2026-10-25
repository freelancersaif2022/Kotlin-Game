package com.saif.imagetopdf.engine

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/** Draws the rebuilt page as real vector text and shapes – the photo itself is never embedded. */
object PdfBuilder {

    const val PAGE_WIDTH_PT = 595 // A4 width; height follows the photo's proportions

    fun scale(model: PageModel) = PAGE_WIDTH_PT.toFloat() / model.width

    fun write(model: PageModel, file: File) {
        val k = scale(model)
        val pageHeight = (model.height * k).roundToInt().coerceAtLeast(1)
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, pageHeight, 1).create())
            val canvas = page.canvas
            canvas.drawColor(model.background)
            canvas.save()
            canvas.scale(k, k)

            val fill = Paint().apply { style = Paint.Style.FILL; isAntiAlias = false }
            for (s in model.shapes) {
                fill.color = s.color
                // A hair of overlap hides seams between neighbouring rectangles in PDF viewers.
                canvas.drawRect(s.left - 0.4f, s.top - 0.4f, s.right + 0.4f, s.bottom + 0.4f, fill)
            }

            val image = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            for (g in model.graphics) canvas.drawBitmap(g.bitmap, g.left, g.top, image)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG)
            val bounds = Rect()
            for (line in model.lines) {
                canvas.save()
                canvas.translate(line.originX, line.originY)
                if (line.angle != 0f) canvas.rotate(line.angle)
                for (word in line.words) {
                    Fonts.apply(paint, line.family, word.weight, word.italic)
                    paint.color = word.color
                    paint.textSize = line.fontSize
                    paint.textScaleX = 1f
                    paint.getTextBounds(word.text, 0, word.text.length, bounds)
                    // Stretch each word to the exact width it had in the photo.
                    val sx = if (bounds.width() > 0) (word.width / bounds.width()).coerceIn(0.55f, 1.8f) else 1f
                    paint.textScaleX = sx
                    canvas.drawText(word.text, word.x - bounds.left * sx, line.baseline, paint)
                }
                canvas.restore()
            }
            canvas.restore()
            doc.finishPage(page)
            FileOutputStream(file).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }
}
