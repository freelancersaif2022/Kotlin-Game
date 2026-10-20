package com.vaultly.app.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.vaultly.app.data.Item
import com.vaultly.app.domain.DeadlineKind
import com.vaultly.app.domain.Deadlines
import com.vaultly.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Builds a "claim pack" PDF: a summary page with the purchase facts and deadlines,
 * followed by one page per receipt/proof photo. Ready to show at a store or attach to an email.
 */
class ClaimPackExporter(private val context: Context) {

    suspend fun export(item: Item): Uri = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        try {
            summaryPage(doc, item)
            val images = listOfNotNull(item.receiptImage) + item.photoList
            images.forEachIndexed { i, path ->
                imagePage(doc, path, if (i == 0 && item.receiptImage != null) "Receipt" else "Proof photo", i + 2)
            }
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val safe = item.title.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').take(40).ifBlank { "item" }
            val file = File(dir, "Vaultly-$safe.pdf")
            file.outputStream().use { doc.writeTo(it) }
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        } finally {
            doc.close()
        }
    }

    fun shareIntent(uri: Uri, item: Item): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Purchase record – ${item.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share claim pack")
    }

    private fun summaryPage(doc: PdfDocument, item: Item) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, 1).create())
        val c = page.canvas

        // Header band
        val header = Paint().apply { color = INK }
        c.drawRect(0f, 0f, W.toFloat(), 118f, header)
        c.drawText("VAULTLY", M, 52f, text(13f, GOLD, bold = true, spacing = 0.25f))
        c.drawText("Purchase record & claim pack", M, 84f, text(20f, Color.WHITE, bold = true))
        c.drawText("Generated ${Format.date(LocalDate.now())}", M, 104f, text(10f, Color.argb(170, 255, 255, 255)))

        var y = 160f
        c.drawText(item.title, M, y, text(22f, INK, bold = true)); y += 22f
        c.drawText(item.store, M, y, text(12f, MUTED)); y += 30f

        val rows = buildList {
            add("Store" to item.store)
            add("Purchase date" to Format.date(item.purchaseDate))
            add("Category" to item.categoryEnum.label)
            item.price?.let { add("Price paid" to Format.money(it, item.currency)) }
            if (item.serial.isNotBlank()) add("Serial / order no." to item.serial)
            add("Return window" to Format.returnWindow(item.returnDays))
            add("Warranty" to Format.warranty(item.warrantyMonths))
        }
        val line = Paint().apply { color = LINE; strokeWidth = 1f }
        rows.forEach { (k, v) ->
            c.drawText(k, M, y, text(11f, MUTED))
            c.drawText(v, W / 2f - 40f, y, text(11f, INK, bold = true))
            y += 10f
            c.drawLine(M, y, W - M, y, line)
            y += 20f
        }

        y += 8f
        c.drawText("Deadlines", M, y, text(14f, INK, bold = true)); y += 16f
        val deadlines = Deadlines.all(item)
        if (deadlines.isEmpty()) {
            c.drawText("No return window or warranty recorded.", M, y + 14f, text(11f, MUTED)); y += 34f
        }
        deadlines.forEach { d ->
            val box = RectF(M, y, W - M, y + 54f)
            c.drawRoundRect(box, 10f, 10f, Paint().apply { color = SOFT })
            val label = if (d.kind == DeadlineKind.RETURN) "Return by" else "Warranty until"
            c.drawText(label, M + 16f, y + 22f, text(10f, MUTED))
            c.drawText(Format.date(d.end), M + 16f, y + 41f, text(14f, INK, bold = true))
            val left = d.daysLeft()
            val status = Deadlines.describeLeft(left)
            val color = when {
                left < 0 -> MUTED
                left <= Deadlines.URGENT_DAYS -> RED
                left <= Deadlines.SOON_DAYS -> AMBER
                else -> GREEN
            }
            val p = text(12f, color, bold = true).apply { textAlign = Paint.Align.RIGHT }
            c.drawText(status, W - M - 16f, y + 33f, p)
            y += 66f
        }

        if (item.notes.isNotBlank()) {
            y += 8f
            c.drawText("Notes", M, y, text(14f, INK, bold = true)); y += 20f
            wrap(item.notes, text(11f, INK), W - 2 * M).take(10).forEach { c.drawText(it, M, y, text(11f, INK)); y += 16f }
        }

        val attachments = listOfNotNull(item.receiptImage).size + item.photoList.size
        c.drawText(
            if (attachments > 0) "$attachments attachment(s) on the following pages." else "No images attached.",
            M, H - 56f, text(10f, MUTED),
        )
        c.drawText("Page 1", W - M, H - 36f, text(9f, MUTED).apply { textAlign = Paint.Align.RIGHT })
        c.drawText("Kept safe with Vaultly", M, H - 36f, text(9f, MUTED))
        doc.finishPage(page)
    }

    private fun imagePage(doc: PdfDocument, path: String, title: String, number: Int) {
        val bitmap = decode(path) ?: return
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, number).create())
        val c = page.canvas
        c.drawText(title, M, 54f, text(16f, INK, bold = true))
        val area = RectF(M, 76f, W - M, H - 60f)
        val scale = minOf(area.width() / bitmap.width, area.height() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val dst = RectF(area.centerX() - w / 2, area.top, area.centerX() + w / 2, area.top + h)
        c.drawBitmap(bitmap, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        c.drawText("Page $number", W - M, H - 36f, text(9f, MUTED).apply { textAlign = Paint.Align.RIGHT })
        doc.finishPage(page)
        bitmap.recycle()
    }

    private fun decode(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1400) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun wrap(s: String, paint: Paint, width: Float): List<String> {
        val out = mutableListOf<String>()
        s.lines().forEach { para ->
            var current = ""
            para.split(' ').forEach { word ->
                val next = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(next) > width && current.isNotEmpty()) {
                    out += current; current = word
                } else current = next
            }
            out += current
        }
        return out
    }

    private fun text(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        letterSpacing = spacing
    }

    companion object {
        private const val W = 595
        private const val H = 842
        private const val M = 44f
        private val INK = Color.rgb(14, 22, 38)
        private val GOLD = Color.rgb(212, 178, 106)
        private val MUTED = Color.rgb(110, 116, 130)
        private val LINE = Color.rgb(230, 228, 222)
        private val SOFT = Color.rgb(246, 244, 238)
        private val RED = Color.rgb(200, 62, 62)
        private val AMBER = Color.rgb(196, 128, 8)
        private val GREEN = Color.rgb(28, 140, 96)
    }
}
