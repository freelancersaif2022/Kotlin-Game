package com.saif.imagetopdf.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Finds the font style of a word by actually drawing it: every candidate (family × weight ×
 * italic) is rendered into the word's exact ink box and compared with the photo's ink, pixel by
 * pixel. The candidate whose letters overlap the original best wins. Not thread-safe.
 */
object FontMatcher {

    /** Comparison height in pixels; words are scaled to this before matching. */
    private const val H = 44

    val FAMILIES = listOf(Family.SANS, Family.SERIF, Family.CONDENSED, Family.MONO)
    private val WEIGHTS = intArrayOf(300, 400, 500, 700, 900)

    /** A word's ink, cropped tight and scaled to height [H]. */
    class Target(val mask: BooleanArray, val w: Int, val h: Int, val ink: Int)

    data class Style(val weight: Int, val italic: Boolean, val score: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = Rect()

    fun target(ink: Ink): Target? {
        if (!ink.hasInk) return null
        var x0 = ink.w; var x1 = -1; var y0 = ink.h; var y1 = -1
        for (y in 0 until ink.h) for (x in 0 until ink.w) if (ink.at(x, y)) {
            if (x < x0) x0 = x
            if (x > x1) x1 = x
            if (y < y0) y0 = y
            if (y > y1) y1 = y
        }
        if (x1 < 0) return null
        val tw = x1 - x0 + 1
        val th = y1 - y0 + 1
        if (th < 7 || tw < 4) return null
        val s = H / th.toFloat()
        val w = (tw * s).roundToInt().coerceIn(4, 900)
        val mask = BooleanArray(w * H)
        var n = 0
        for (v in 0 until H) for (u in 0 until w) {
            // 2×2 supersampling keeps thin strokes when shrinking and smooth edges when enlarging.
            var c = 0
            for (sy in 0..1) for (sx in 0..1) {
                val x = x0 + ((u + 0.25f + 0.5f * sx) / s).toInt()
                val y = y0 + ((v + 0.25f + 0.5f * sy) / s).toInt()
                if (x <= x1 && y <= y1 && ink.at(x, y)) c++
            }
            if (c >= 2) { mask[v * w + u] = true; n++ }
        }
        if (n < 12) return null
        return Target(mask, w, H, n)
    }

    /** Average match of each family over the given words (weighted by word size). */
    fun familyScores(words: List<Pair<String, Target>>): Map<Family, Float>? {
        val usable = words.filter { (t, _) -> t.count { it.isLetter() } >= 3 }
        if (usable.isEmpty()) return null
        val total = usable.sumOf { it.second.ink }.toFloat()
        return FAMILIES.associateWith { family ->
            usable.sumOf { (text, t) ->
                withCanvas(t) { bmp, buf ->
                    maxOf(score(text, family, 400, false, t, bmp, buf), score(text, family, 700, false, t, bmp, buf))
                }.toDouble() * t.ink
            }.toFloat() / total
        }
    }

    /** Best weight and slant for one word in the given family. */
    fun bestStyle(text: String, t: Target, family: Family): Style = withCanvas(t) { bmp, buf ->
        var best = Style(400, false, -1f)
        var bestAdjusted = -1f
        for (italic in booleanArrayOf(false, true)) for (weight in WEIGHTS) {
            val s = score(text, family, weight, italic, t, bmp, buf)
            // Small bias towards Regular and upright so noise never invents emphasis.
            val adjusted = s - (if (weight != 400) 0.006f else 0f) - (if (italic) 0.012f else 0f)
            if (adjusted > bestAdjusted) { bestAdjusted = adjusted; best = Style(weight, italic, s) }
        }
        best
    }

    private inline fun <T> withCanvas(t: Target, block: (Bitmap, ByteBuffer) -> T): T {
        val bmp = Bitmap.createBitmap(t.w, t.h, Bitmap.Config.ALPHA_8)
        val buf = ByteBuffer.allocate(bmp.rowBytes * bmp.height)
        try {
            return block(bmp, buf)
        } finally {
            bmp.recycle()
        }
    }

    /** Dice overlap (0…1) between the rendered candidate and the photo's ink. */
    private fun score(text: String, family: Family, weight: Int, italic: Boolean, t: Target, bmp: Bitmap, buf: ByteBuffer): Float {
        Fonts.apply(paint, family, weight, italic)
        paint.textSize = 100f
        paint.textScaleX = 1f
        paint.getTextBounds(text, 0, text.length, bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return 0f
        bmp.eraseColor(0)
        val canvas = Canvas(bmp)
        canvas.scale(t.w / bounds.width().toFloat(), t.h / bounds.height().toFloat())
        canvas.drawText(text, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)
        buf.rewind()
        bmp.copyPixelsToBuffer(buf)
        val row = bmp.rowBytes
        var drawn = 0
        var overlap = 0
        for (v in 0 until t.h) for (u in 0 until t.w) {
            if ((buf.get(v * row + u).toInt() and 0xff) >= 128) {
                drawn++
                if (t.mask[v * t.w + u]) overlap++
            }
        }
        return 2f * overlap / (drawn + t.ink).coerceAtLeast(1)
    }
}
