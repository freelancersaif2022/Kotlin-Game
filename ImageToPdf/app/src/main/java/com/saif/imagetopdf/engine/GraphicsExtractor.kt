package com.saif.imagetopdf.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * Finds the marks on the page that are neither text nor straight lines – seals, stamps,
 * signatures, logos, icons, handwriting – and cuts them out pixel-for-pixel with the paper made
 * transparent, so they appear in the PDF exactly as in the photo.
 */
object GraphicsExtractor {

    private const val BLOCK = 6

    /** Bounding boxes of connected non-text, non-rule marks. */
    fun regions(px: Pixels, textMask: BooleanArray, bg: BackgroundBuilder.Result): List<Rect> {
        val ruleMask = ruleMask(px, bg.rules)
        val bw = (px.w + BLOCK - 1) / BLOCK
        val bh = (px.h + BLOCK - 1) / BLOCK
        val count = IntArray(bw * bh)
        for (y in 0 until px.h) for (x in 0 until px.w) {
            val i = y * px.w + x
            if (textMask[i] || ruleMask[i]) continue
            if (colorDist(px.data[i], bg.backgroundAt(x, y)) > 42f) count[(y / BLOCK) * bw + x / BLOCK]++
        }

        // Blocks with real ink, grown by two blocks so the strokes of one mark join up.
        val marked = BooleanArray(bw * bh) { count[it] >= 3 }
        val grown = BooleanArray(bw * bh)
        for (by in 0 until bh) for (bx in 0 until bw) if (marked[by * bw + bx]) {
            for (dy in -2..2) for (dx in -2..2) {
                val nx = bx + dx
                val ny = by + dy
                if (nx in 0 until bw && ny in 0 until bh) grown[ny * bw + nx] = true
            }
        }

        val seen = BooleanArray(bw * bh)
        val out = ArrayList<Rect>()
        val queue = IntArray(bw * bh)
        for (start in grown.indices) {
            if (!grown[start] || seen[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            seen[start] = true
            var x0 = Int.MAX_VALUE; var y0 = Int.MAX_VALUE; var x1 = -1; var y1 = -1
            var ink = 0
            while (head < tail) {
                val b = queue[head++]
                val bx = b % bw
                val by = b / bw
                if (marked[b]) {
                    ink += count[b]
                    x0 = min(x0, bx); y0 = min(y0, by); x1 = max(x1, bx); y1 = max(y1, by)
                }
                for ((nx, ny) in arrayOf(bx - 1 to by, bx + 1 to by, bx to by - 1, bx to by + 1)) {
                    if (nx !in 0 until bw || ny !in 0 until bh) continue
                    val n = ny * bw + nx
                    if (grown[n] && !seen[n]) { seen[n] = true; queue[tail++] = n }
                }
            }
            if (x1 < 0 || ink < 40) continue // specks of dust / JPEG noise
            out += Rect(
                max(0, x0 * BLOCK - 2), max(0, y0 * BLOCK - 2),
                min(px.w, (x1 + 1) * BLOCK + 2), min(px.h, (y1 + 1) * BLOCK + 2),
            )
        }
        return out
    }

    /** Cuts each region out of the photo; paper becomes transparent, ink keeps its exact pixels. */
    fun cut(px: Pixels, textMask: BooleanArray, bg: BackgroundBuilder.Result, regions: List<Rect>): List<Graphic> {
        val ruleMask = ruleMask(px, bg.rules)
        return regions.map { r ->
            val w = r.width()
            val h = r.height()
            val pixels = IntArray(w * h)
            for (y in 0 until h) for (x in 0 until w) {
                val gx = r.left + x
                val gy = r.top + y
                val i = gy * px.w + gx
                if (textMask[i] || ruleMask[i]) continue
                val c = px.data[i]
                val d = colorDist(c, bg.backgroundAt(gx, gy))
                // Soft edge: faint antialiasing stays partly visible, the paper disappears.
                val a = ((d - 10f) * 255f / 28f).toInt().coerceIn(0, 255)
                if (a > 0) pixels[y * w + x] = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
            }
            Graphic(r.left.toFloat(), r.top.toFloat(), Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888))
        }
    }

    /** Fraction of [line] covered by [region]. */
    fun overlap(line: Rect, region: Rect): Float {
        val ix = min(line.right, region.right) - max(line.left, region.left)
        val iy = min(line.bottom, region.bottom) - max(line.top, region.top)
        if (ix <= 0 || iy <= 0) return 0f
        return (ix.toFloat() * iy) / (line.width().toFloat() * line.height()).coerceAtLeast(1f)
    }

    private fun ruleMask(px: Pixels, rules: List<ColorRect>): BooleanArray {
        val mask = BooleanArray(px.w * px.h)
        for (r in rules) {
            val l = (r.left.toInt() - 1).coerceAtLeast(0)
            val rt = (r.right.toInt() + 1).coerceAtMost(px.w)
            for (y in (r.top.toInt() - 1).coerceAtLeast(0) until (r.bottom.toInt() + 1).coerceAtMost(px.h)) {
                mask.fill(true, y * px.w + l, y * px.w + rt)
            }
        }
        return mask
    }
}
