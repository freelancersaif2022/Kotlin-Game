package com.pdfmaster.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix as AndroidMatrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.Matrix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

typealias Progress = (Float) -> Unit

enum class CompressLevel(val maxDimension: Int, val quality: Float) {
    LOW(2200, 0.85f),
    RECOMMENDED(1600, 0.65f),
    EXTREME(1100, 0.45f),
}

enum class PageSizeOption { A4, LETTER, FIT }
enum class OrientationOption { AUTO, PORTRAIT, LANDSCAPE }
enum class MarginOption(val points: Float) { NONE(0f), SMALL(18f), LARGE(42f) }

enum class NumberPosition { BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, TOP_LEFT, TOP_CENTER, TOP_RIGHT }
enum class NumberFormat(val label: String) { PLAIN("1"), PAGE_N("Page 1"), N_OF_TOTAL("1 of 12"), PAGE_N_OF_TOTAL("Page 1 of 12") }

data class PageEdit(val sourceIndex: Int, val rotation: Int)

data class CompressResult(val file: File, val before: Long, val after: Long)

/** All document manipulation runs here, off the main thread, using PdfBox. */
@Singleton
class PdfTools @Inject constructor(@ApplicationContext private val context: Context) {

    private val fontPath = "com/tom_roush/pdfbox/resources/ttf/LiberationSans-Regular.ttf"

    init {
        PDFBoxResourceLoader.init(context)
    }

    private fun open(file: File, password: String? = null): PDDocument {
        val doc = if (password.isNullOrEmpty()) {
            PDDocument.load(file, MemoryUsageSetting.setupMixed(48L * 1024 * 1024))
        } else {
            PDDocument.load(file, password, MemoryUsageSetting.setupMixed(48L * 1024 * 1024))
        }
        // Owner-password-only files open fine but refuse edits; drop that restriction for the output.
        if (doc.isEncrypted) doc.isAllSecurityToBeRemoved = true
        return doc
    }

    private fun font(doc: PDDocument): PDFont = PDType0Font.load(doc, PDFBoxResourceLoader.getStream(fontPath))

    /** Replaces characters the bundled font can't draw so a stray emoji never aborts the whole job. */
    private fun safeText(font: PDFont, text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val s = String(Character.toChars(cp))
            val ok = runCatching { font.encode(s); true }.getOrDefault(false)
            sb.append(if (ok) s else if (cp == '\t'.code) "    " else "?")
            i += Character.charCount(cp)
        }
        return sb.toString()
    }

    suspend fun isPasswordProtected(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(file).close()
            false
        } catch (e: InvalidPasswordException) {
            true
        }
    }

    suspend fun pageCount(file: File, password: String? = null): Int = withContext(Dispatchers.IO) {
        open(file, password).use { it.numberOfPages }
    }

    suspend fun merge(files: List<File>, out: File, progress: Progress): File = withContext(Dispatchers.IO) {
        files.forEach { f ->
            if (isPasswordProtected(f)) throw PdfToolException("One of the selected files is password protected. Unlock it first.")
        }
        progress(0.2f)
        val merger = PDFMergerUtility()
        files.forEach { merger.addSource(it) }
        merger.destinationFileName = out.absolutePath
        merger.mergeDocuments(MemoryUsageSetting.setupMixed(64L * 1024 * 1024))
        progress(1f)
        out
    }

    /** Writes each page group to its own file. Page indexes are zero-based. */
    suspend fun split(file: File, groups: List<List<Int>>, outDir: File, baseName: String, progress: Progress): List<File> =
        withContext(Dispatchers.IO) {
            open(file).use { src ->
                val total = src.numberOfPages
                groups.mapIndexed { gi, pages ->
                    coroutineContext.ensureActive()
                    val target = File(outDir, "${baseName}_part${gi + 1}_${System.nanoTime()}.pdf")
                    PDDocument().use { dst ->
                        pages.filter { it in 0 until total }.forEach { dst.importPage(src.getPage(it)) }
                        dst.save(target)
                    }
                    progress((gi + 1f) / groups.size)
                    target
                }
            }
        }

    suspend fun organize(file: File, edits: List<PageEdit>, out: File, progress: Progress): File = withContext(Dispatchers.IO) {
        open(file).use { src ->
            PDDocument().use { dst ->
                edits.forEachIndexed { i, edit ->
                    val srcPage = src.getPage(edit.sourceIndex)
                    val imported = dst.importPage(srcPage)
                    imported.rotation = ((srcPage.rotation + edit.rotation) % 360 + 360) % 360
                    progress((i + 1f) / edits.size * 0.9f)
                }
                dst.save(out)
            }
        }
        progress(1f)
        out
    }

    suspend fun compress(file: File, level: CompressLevel, out: File, progress: Progress): CompressResult = withContext(Dispatchers.IO) {
        val before = file.length()
        open(file).use { doc ->
            val visited = HashSet<COSName>()
            val n = doc.numberOfPages
            for (i in 0 until n) {
                coroutineContext.ensureActive()
                doc.getPage(i).resources?.let { recompressImages(doc, it, level, visited, depth = 0) }
                progress((i + 1f) / n * 0.9f)
            }
            doc.save(out)
        }
        progress(1f)
        if (out.length() >= before) {
            // Nothing to gain: hand back an identical copy rather than a bigger file.
            file.copyTo(out, overwrite = true)
        }
        CompressResult(out, before, out.length())
    }

    private fun recompressImages(doc: PDDocument, res: PDResources, level: CompressLevel, visited: MutableSet<COSName>, depth: Int) {
        if (depth > 4) return
        for (name in res.xObjectNames.toList()) {
            val xo = runCatching { res.getXObject(name) }.getOrNull() ?: continue
            when (xo) {
                is PDImageXObject -> {
                    if (xo.isStencil || xo.cosObject.containsKey(COSName.SMASK)) continue
                    if (xo.width * xo.height < 160 * 160) continue
                    val bitmap = runCatching { xo.image }.getOrNull() ?: continue
                    val scale = min(1f, level.maxDimension.toFloat() / max(bitmap.width, bitmap.height))
                    val scaled = if (scale < 1f) {
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt().coerceAtLeast(1), (bitmap.height * scale).roundToInt().coerceAtLeast(1), true)
                    } else bitmap
                    val opaque = ensureOpaque(scaled)
                    val replacement = JPEGFactory.createFromImage(doc, opaque, level.quality)
                    val oldLength = xo.cosObject.length
                    if (replacement.cosObject.length < oldLength) res.put(name, replacement)
                    if (opaque !== scaled) opaque.recycle()
                    if (scaled !== bitmap) scaled.recycle()
                    bitmap.recycle()
                }
                is PDFormXObject -> {
                    if (visited.add(name)) xo.resources?.let { recompressImages(doc, it, level, visited, depth + 1) }
                }
            }
        }
    }

    private fun ensureOpaque(src: Bitmap): Bitmap {
        if (!src.hasAlpha()) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply { drawColor(Color.WHITE); drawBitmap(src, 0f, 0f, null) }
        return out
    }

    suspend fun lock(file: File, password: String, out: File, progress: Progress): File = withContext(Dispatchers.IO) {
        if (isPasswordProtected(file)) throw PdfToolException("This PDF is already password protected.")
        progress(0.3f)
        open(file).use { doc ->
            val policy = StandardProtectionPolicy(password + "#owner" + System.nanoTime(), password, AccessPermission())
            policy.encryptionKeyLength = 256
            doc.protect(policy)
            doc.save(out)
        }
        progress(1f)
        out
    }

    suspend fun unlock(file: File, password: String, out: File, progress: Progress): File = withContext(Dispatchers.IO) {
        progress(0.3f)
        val doc = try {
            PDDocument.load(file, password)
        } catch (e: InvalidPasswordException) {
            throw PdfToolException("Incorrect password. Please check it and try again.", e)
        }
        doc.use {
            if (!it.isEncrypted) throw PdfToolException("This PDF isn't password protected.")
            it.isAllSecurityToBeRemoved = true
            it.save(out)
        }
        progress(1f)
        out
    }

    /** Decrypts a document so the platform renderer can display it. */
    suspend fun decryptForViewing(file: File, password: String, out: File): File = withContext(Dispatchers.IO) {
        val doc = try {
            PDDocument.load(file, password)
        } catch (e: InvalidPasswordException) {
            throw PdfToolException("Incorrect password", e)
        }
        doc.use {
            it.isAllSecurityToBeRemoved = true
            it.save(out)
        }
        out
    }

    suspend fun watermark(
        file: File, text: String, color: Int, opacity: Float, sizeRatio: Float, diagonal: Boolean, out: File, progress: Progress,
    ): File = withContext(Dispatchers.IO) {
        open(file).use { doc ->
            val f = font(doc)
            val clean = safeText(f, text)
            val gs = PDExtendedGraphicsState().apply {
                nonStrokingAlphaConstant = opacity
                strokingAlphaConstant = opacity
            }
            val n = doc.numberOfPages
            for (i in 0 until n) {
                coroutineContext.ensureActive()
                val page = doc.getPage(i)
                withDisplaySpace(doc, page) { cs, w, h ->
                    val fontSize = min(w, h) * sizeRatio
                    val textWidth = f.getStringWidth(clean) / 1000f * fontSize
                    cs.setGraphicsStateParameters(gs)
                    cs.setNonStrokingColor(Color.red(color), Color.green(color), Color.blue(color))
                    cs.beginText()
                    cs.setFont(f, fontSize)
                    val angle = if (diagonal) Math.atan2(h.toDouble(), w.toDouble()) else 0.0
                    val m = Matrix.getRotateInstance(angle, w / 2f, h / 2f)
                    m.translate(-textWidth / 2f, -fontSize / 3f)
                    cs.setTextMatrix(m)
                    cs.showText(clean)
                    cs.endText()
                }
                progress((i + 1f) / n * 0.95f)
            }
            doc.save(out)
        }
        progress(1f)
        out
    }

    suspend fun addPageNumbers(
        file: File, position: NumberPosition, format: NumberFormat, startAt: Int, fontSize: Float, out: File, progress: Progress,
    ): File = withContext(Dispatchers.IO) {
        open(file).use { doc ->
            val f = font(doc)
            val n = doc.numberOfPages
            for (i in 0 until n) {
                coroutineContext.ensureActive()
                val number = startAt + i
                val total = startAt + n - 1
                val label = when (format) {
                    NumberFormat.PLAIN -> "$number"
                    NumberFormat.PAGE_N -> "Page $number"
                    NumberFormat.N_OF_TOTAL -> "$number of $total"
                    NumberFormat.PAGE_N_OF_TOTAL -> "Page $number of $total"
                }
                withDisplaySpace(doc, doc.getPage(i)) { cs, w, h ->
                    val tw = f.getStringWidth(label) / 1000f * fontSize
                    val margin = 28f
                    val x = when (position) {
                        NumberPosition.BOTTOM_LEFT, NumberPosition.TOP_LEFT -> margin
                        NumberPosition.BOTTOM_CENTER, NumberPosition.TOP_CENTER -> (w - tw) / 2f
                        else -> w - margin - tw
                    }
                    val y = when (position) {
                        NumberPosition.TOP_LEFT, NumberPosition.TOP_CENTER, NumberPosition.TOP_RIGHT -> h - margin - fontSize
                        else -> margin
                    }
                    cs.setNonStrokingColor(40, 42, 54)
                    cs.beginText()
                    cs.setFont(f, fontSize)
                    cs.newLineAtOffset(x, y)
                    cs.showText(label)
                    cs.endText()
                }
                progress((i + 1f) / n * 0.95f)
            }
            doc.save(out)
        }
        progress(1f)
        out
    }

    suspend fun imagesToPdf(
        images: List<Uri>, pageSize: PageSizeOption, orientation: OrientationOption, margin: MarginOption, out: File, progress: Progress,
    ): File = withContext(Dispatchers.IO) {
        PDDocument().use { doc ->
            images.forEachIndexed { index, uri ->
                coroutineContext.ensureActive()
                val bitmap = decodeImage(uri, 2400) ?: throw PdfToolException("One of the images couldn't be opened.")
                val opaque = ensureOpaque(bitmap)
                val image = JPEGFactory.createFromImage(doc, opaque, 0.88f)
                val landscapeImage = bitmap.width > bitmap.height
                val base = when (pageSize) {
                    PageSizeOption.A4 -> PDRectangle.A4
                    PageSizeOption.LETTER -> PDRectangle.LETTER
                    PageSizeOption.FIT -> PDRectangle(bitmap.width * 0.75f + margin.points * 2, bitmap.height * 0.75f + margin.points * 2)
                }
                val landscape = when (orientation) {
                    OrientationOption.AUTO -> landscapeImage
                    OrientationOption.PORTRAIT -> false
                    OrientationOption.LANDSCAPE -> true
                }
                val rect = if (pageSize != PageSizeOption.FIT && landscape) PDRectangle(base.height, base.width) else base
                val page = PDPage(rect)
                doc.addPage(page)
                val availW = rect.width - margin.points * 2
                val availH = rect.height - margin.points * 2
                val scale = min(availW / bitmap.width, availH / bitmap.height)
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(image, (rect.width - drawW) / 2f, (rect.height - drawH) / 2f, drawW, drawH)
                }
                if (opaque !== bitmap) opaque.recycle()
                bitmap.recycle()
                progress((index + 1f) / images.size * 0.92f)
            }
            doc.save(out)
        }
        progress(1f)
        out
    }

    suspend fun textToPdf(text: String, fontSize: Float, out: File, progress: Progress): File = withContext(Dispatchers.IO) {
        PDDocument().use { doc ->
            val f = font(doc)
            val pageRect = PDRectangle.A4
            val margin = 56f
            val leading = fontSize * 1.45f
            val maxWidth = pageRect.width - margin * 2
            val lines = ArrayList<String>()
            safeText(f, text.replace("\r", "")).split('\n').forEach { paragraph ->
                if (paragraph.isBlank()) {
                    lines += ""
                } else {
                    var line = StringBuilder()
                    paragraph.split(' ').forEach { word ->
                        val candidate = if (line.isEmpty()) word else "$line $word"
                        if (f.getStringWidth(candidate) / 1000f * fontSize <= maxWidth) {
                            line = StringBuilder(candidate)
                        } else {
                            if (line.isNotEmpty()) lines += line.toString()
                            // Hard-wrap words that are longer than a whole line.
                            var rest = word
                            while (f.getStringWidth(rest) / 1000f * fontSize > maxWidth && rest.length > 1) {
                                var cut = rest.length - 1
                                while (cut > 1 && f.getStringWidth(rest.substring(0, cut)) / 1000f * fontSize > maxWidth) cut--
                                lines += rest.substring(0, cut)
                                rest = rest.substring(cut)
                            }
                            line = StringBuilder(rest)
                        }
                    }
                    if (line.isNotEmpty()) lines += line.toString()
                }
            }
            val perPage = ((pageRect.height - margin * 2) / leading).toInt().coerceAtLeast(1)
            val chunks = if (lines.isEmpty()) listOf(listOf("")) else lines.chunked(perPage)
            chunks.forEachIndexed { i, chunk ->
                val page = PDPage(pageRect)
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(f, fontSize)
                    cs.setNonStrokingColor(27, 29, 41)
                    cs.setLeading(leading)
                    cs.newLineAtOffset(margin, pageRect.height - margin - fontSize)
                    chunk.forEach { l ->
                        cs.showText(l)
                        cs.newLine()
                    }
                    cs.endText()
                }
                progress((i + 1f) / chunks.size * 0.95f)
            }
            doc.save(out)
        }
        progress(1f)
        out
    }

    suspend fun extractText(file: File, progress: Progress): String = withContext(Dispatchers.IO) {
        open(file).use { doc ->
            val n = doc.numberOfPages
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            val sb = StringBuilder()
            for (i in 1..n) {
                coroutineContext.ensureActive()
                stripper.startPage = i
                stripper.endPage = i
                val pageText = stripper.getText(doc).trim()
                if (pageText.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append("\n\n")
                    sb.append("— Page ").append(i).append(" —\n\n").append(pageText)
                }
                progress(i.toFloat() / n)
            }
            sb.toString()
        }
    }

    /** Returns 0-based page indexes whose text contains [query], with a short snippet for each. */
    suspend fun search(file: File, query: String): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        open(file).use { doc ->
            val stripper = PDFTextStripper()
            val hits = ArrayList<Pair<Int, String>>()
            for (i in 1..doc.numberOfPages) {
                coroutineContext.ensureActive()
                stripper.startPage = i
                stripper.endPage = i
                val text = stripper.getText(doc).replace(Regex("\\s+"), " ")
                val at = text.indexOf(query, ignoreCase = true)
                if (at >= 0) {
                    val start = max(0, at - 40)
                    val end = min(text.length, at + query.length + 60)
                    val snippet = (if (start > 0) "…" else "") + text.substring(start, end).trim() + (if (end < text.length) "…" else "")
                    hits += (i - 1) to snippet
                }
            }
            hits
        }
    }

    /** Renders pages with the platform renderer, which is much faster than PdfBox for rasterising. */
    suspend fun renderPages(file: File, scale: Float, onPage: suspend (index: Int, total: Int, bitmap: Bitmap) -> Unit) =
        withContext(Dispatchers.IO) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        coroutineContext.ensureActive()
                        renderer.openPage(i).use { page ->
                            val w = (page.width * scale).roundToInt().coerceIn(1, 4000)
                            val h = (page.height * scale).roundToInt().coerceIn(1, 4000)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            onPage(i, renderer.pageCount, bmp)
                            bmp.recycle()
                        }
                    }
                }
            }
        }

    suspend fun applyMarks(file: File, marks: List<PageMark>, out: File, progress: Progress): File = withContext(Dispatchers.IO) {
        open(file).use { doc ->
            val byPage = marks.groupBy { it.page }
            val f by lazy { font(doc) }
            val images = HashMap<String, PDImageXObject>()
            byPage.entries.forEachIndexed { idx, (pageIndex, pageMarks) ->
                coroutineContext.ensureActive()
                if (pageIndex !in 0 until doc.numberOfPages) return@forEachIndexed
                withDisplaySpace(doc, doc.getPage(pageIndex)) { cs, w, h ->
                    pageMarks.forEach { mark ->
                        when (mark) {
                            is InkMark -> drawInk(cs, mark, w, h)
                            is TextMark -> drawText(cs, f, mark, w, h)
                            is ImageMark -> {
                                val img = images.getOrPut(mark.path) {
                                    val bmp = BitmapFactory.decodeFile(mark.path) ?: throw PdfToolException("Signature image is missing.")
                                    LosslessFactory.createFromImage(doc, bmp).also { bmp.recycle() }
                                }
                                cs.drawImage(img, mark.x * w, h - (mark.y + mark.h) * h, mark.w * w, mark.h * h)
                            }
                        }
                    }
                }
                progress((idx + 1f) / byPage.size * 0.9f)
            }
            doc.save(out)
        }
        progress(1f)
        out
    }

    private fun drawInk(cs: PDPageContentStream, mark: InkMark, w: Float, h: Float) {
        val pts = mark.points
        if (pts.size < 2) return
        cs.saveGraphicsState()
        if (mark.alpha < 1f) {
            cs.setGraphicsStateParameters(PDExtendedGraphicsState().apply { strokingAlphaConstant = mark.alpha })
        }
        cs.setStrokingColor(Color.red(mark.color), Color.green(mark.color), Color.blue(mark.color))
        cs.setLineWidth(mark.width * w)
        cs.setLineCapStyle(1)
        cs.setLineJoinStyle(1)
        cs.moveTo(pts[0] * w, h - pts[1] * h)
        if (pts.size == 2) {
            cs.lineTo(pts[0] * w + 0.01f, h - pts[1] * h)
        } else {
            var i = 2
            while (i + 1 < pts.size) {
                cs.lineTo(pts[i] * w, h - pts[i + 1] * h)
                i += 2
            }
        }
        cs.stroke()
        cs.restoreGraphicsState()
    }

    private fun drawText(cs: PDPageContentStream, f: PDFont, mark: TextMark, w: Float, h: Float) {
        val size = mark.size * w
        val lines = safeText(f, mark.text).split('\n')
        val leading = size * 1.25f
        mark.background?.let { bg ->
            val textW = lines.maxOf { f.getStringWidth(it) / 1000f * size }
            val pad = size * 0.45f
            cs.saveGraphicsState()
            cs.setNonStrokingColor(Color.red(bg), Color.green(bg), Color.blue(bg))
            cs.addRect(mark.x * w - pad, h - mark.y * h - lines.size * leading - pad * 0.6f, textW + pad * 2, lines.size * leading + pad * 1.4f)
            cs.fill()
            cs.restoreGraphicsState()
        }
        cs.beginText()
        cs.setFont(f, size)
        cs.setNonStrokingColor(Color.red(mark.color), Color.green(mark.color), Color.blue(mark.color))
        cs.setLeading(leading)
        cs.newLineAtOffset(mark.x * w, h - mark.y * h - size)
        lines.forEach {
            cs.showText(it)
            cs.newLine()
        }
        cs.endText()
    }

    /**
     * Opens an append stream whose coordinate system matches the page as the user sees it:
     * origin bottom-left of the visible (rotated) page, so callers never deal with /Rotate.
     */
    private inline fun withDisplaySpace(doc: PDDocument, page: PDPage, block: (PDPageContentStream, Float, Float) -> Unit) {
        val box = page.cropBox
        val pw = box.width
        val ph = box.height
        val rotation = ((page.rotation % 360) + 360) % 360
        val (dw, dh) = if (rotation == 90 || rotation == 270) ph to pw else pw to ph
        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            cs.saveGraphicsState()
            val m = when (rotation) {
                90 -> Matrix(0f, 1f, -1f, 0f, box.lowerLeftX + pw, box.lowerLeftY)
                180 -> Matrix(-1f, 0f, 0f, -1f, box.lowerLeftX + pw, box.lowerLeftY + ph)
                270 -> Matrix(0f, -1f, 1f, 0f, box.lowerLeftX, box.lowerLeftY + ph)
                else -> Matrix(1f, 0f, 0f, 1f, box.lowerLeftX, box.lowerLeftY)
            }
            cs.transform(m)
            block(cs, dw, dh)
            cs.restoreGraphicsState()
        }
    }

    fun decodeImage(uri: Uri, maxDim: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val longest = max(bmp.width, bmp.height)
        if (longest > maxDim) {
            val s = maxDim.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(bmp, (bmp.width * s).roundToInt(), (bmp.height * s).roundToInt(), true)
            if (scaled !== bmp) bmp.recycle()
            bmp = scaled
        }
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees != 0f) {
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, AndroidMatrix().apply { postRotate(degrees) }, true)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }
        return bmp
    }
}
