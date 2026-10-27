package com.pdfstudio.app.data

import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.cos.COSName
import java.io.File

/** Crop rectangle as fractions (0..1) of the page as displayed, origin top-left. */
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val isFullPage: Boolean
        get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f

    companion object {
        val Full = CropRect(0f, 0f, 1f, 1f)
    }
}

/**
 * Lossless page operations. Pages are copied as PDF objects (text, vectors and images
 * stay untouched), so the output keeps the original quality.
 */
object PdfOps {
    private fun load(file: File): PDDocument =
        PDDocument.load(file, MemoryUsageSetting.setupMixed(64L * 1024 * 1024)).also {
            if (it.isEncrypted) it.isAllSecurityToBeRemoved = true
        }

    /** Writes the given 0-based [pages], in order, into [out]. */
    fun split(src: File, pages: List<Int>, out: File) {
        load(src).use { source ->
            PDDocument().use { target ->
                target.documentInformation = source.documentInformation
                for (index in pages) {
                    val page = source.getPage(index)
                    val imported = target.importPage(page)
                    imported.resources = page.resources
                }
                target.save(out)
            }
        }
    }

    /** Crops page [index] of [src] to [rect] and writes the whole document to [out]. */
    fun crop(src: File, index: Int, rect: CropRect, out: File) {
        load(src).use { doc ->
            val page = doc.getPage(index)
            val box = page.cropBox
            val llx = box.lowerLeftX
            val lly = box.lowerLeftY
            val urx = box.upperRightX
            val ury = box.upperRightY
            val w = box.width
            val h = box.height
            val (l, t, r, b) = listOf(rect.left, rect.top, rect.right, rect.bottom)

            // Map the on-screen rectangle back into unrotated PDF user space.
            val newBox = when (((page.rotation % 360) + 360) % 360) {
                90 -> PDRectangle(llx + t * w, lly + l * h, (b - t) * w, (r - l) * h)
                180 -> PDRectangle(urx - r * w, lly + t * h, (r - l) * w, (b - t) * h)
                270 -> PDRectangle(urx - b * w, ury - r * h, (b - t) * w, (r - l) * h)
                else -> PDRectangle(llx + l * w, ury - b * h, (r - l) * w, (b - t) * h)
            }
            page.mediaBox = newBox
            page.cropBox = newBox
            page.cosObject.removeItem(COSName.BLEED_BOX)
            page.cosObject.removeItem(COSName.TRIM_BOX)
            page.cosObject.removeItem(COSName.ART_BOX)
            doc.save(out)
        }
    }
}
