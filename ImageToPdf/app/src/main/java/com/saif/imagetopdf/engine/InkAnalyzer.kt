package com.saif.imagetopdf.engine

import android.graphics.Color
import android.graphics.Rect
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Separates text ink from its background inside a box and measures the strokes:
 * colours, stem thickness (weight), slant (italic) and serif feet.
 */
class Ink(
    val box: Rect,
    val background: Int,
    val color: Int,
    val threshold: Float,
    val mask: BooleanArray,
    val hasInk: Boolean,
) {
    val w get() = box.width()
    val h get() = box.height()
    fun at(x: Int, y: Int) = mask[y * w + x]

    /** First and last rows that really contain ink (ML Kit boxes carry some padding). */
    fun inkRows(): IntRange? {
        val minCount = max(1, w / 400)
        var first = -1
        var last = -1
        for (y in 0 until h) {
            var n = 0
            for (x in 0 until w) if (at(x, y)) n++
            if (n > minCount) {
                if (first < 0) first = y
                last = y
            }
        }
        return if (first < 0) null else first..last
    }
}

object InkAnalyzer {

    fun analyze(px: Pixels, rect: Rect): Ink {
        val r = px.clamp(rect)
        val w = r.width()
        val h = r.height()

        // Background: median of a ring just outside the box.
        val ring = IntArray(2 * (w + h) * 2 + 16)
        var n = 0
        for (d in 1..2) {
            val top = (r.top - d).coerceAtLeast(0)
            val bottom = (r.bottom - 1 + d).coerceAtMost(px.h - 1)
            val left = (r.left - d).coerceAtLeast(0)
            val right = (r.right - 1 + d).coerceAtMost(px.w - 1)
            var x = r.left
            while (x < r.right && n < ring.size - 2) {
                ring[n++] = px[x, top]; ring[n++] = px[x, bottom]; x += 2
            }
            var y = r.top
            while (y < r.bottom && n < ring.size - 2) {
                ring[n++] = px[left, y]; ring[n++] = px[right, y]; y += 2
            }
        }
        val bg = medianColor(ring, n)

        // Distance of every pixel from the background, with a histogram for percentiles.
        val dist = FloatArray(w * h)
        val hist = IntArray(443)
        for (y in 0 until h) for (x in 0 until w) {
            val d = colorDist(px[r.left + x, r.top + y], bg)
            dist[y * w + x] = d
            hist[d.toInt()]++
        }
        var acc = 0
        var p97 = 0
        val target = (dist.size * 0.97).toInt()
        for (i in hist.indices) {
            acc += hist[i]
            if (acc >= target) { p97 = i; break }
        }
        if (p97 < 45) {
            val fallback = if (luma(bg) > 128) Color.BLACK else Color.WHITE
            return Ink(r, bg, fallback, Float.MAX_VALUE, BooleanArray(w * h), false)
        }

        val threshold = p97 * 0.5f
        val core = p97 * 0.75f
        var sr = 0L; var sg = 0L; var sb = 0L; var cn = 0
        val mask = BooleanArray(w * h)
        for (i in dist.indices) {
            if (dist[i] >= threshold) mask[i] = true
            if (dist[i] >= core) {
                val c = px[r.left + i % w, r.top + i / w]
                sr += Color.red(c); sg += Color.green(c); sb += Color.blue(c); cn++
            }
        }
        val ink = if (cn == 0) Color.BLACK else Color.rgb((sr / cn).toInt(), (sg / cn).toInt(), (sb / cn).toInt())
        return Ink(r, bg, ink, threshold, mask, true)
    }

    /**
     * Average stroke thickness in pixels. A stroke of width w and length L has area w·L and
     * outline ≈ 2·L, so thickness = 2·area / outline – whatever the letter shapes are.
     */
    fun stemWidth(ink: Ink): Float {
        if (!ink.hasInk) return Float.NaN
        var area = 0
        var edges = 0
        for (y in 0 until ink.h) for (x in 0 until ink.w) {
            if (!ink.at(x, y)) continue
            area++
            if (x == 0 || !ink.at(x - 1, y)) edges++
            if (x == ink.w - 1 || !ink.at(x + 1, y)) edges++
            if (y == 0 || !ink.at(x, y - 1)) edges++
            if (y == ink.h - 1 || !ink.at(x, y + 1)) edges++
        }
        if (area < 8 || edges == 0) return Float.NaN
        return 2f * area / edges
    }

    /** Text is italic when shearing it upright makes the vertical strokes line up much better. */
    fun isItalic(ink: Ink): Boolean {
        val w = ink.w
        val h = ink.h
        if (!ink.hasInk || h < 10 || w < 6) return false
        val shears = floatArrayOf(-0.1f, 0f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.35f)
        val pad = ceil(0.35f * h).toInt() + 2
        val cols = IntArray(w + pad * 2)
        var upright = 0L
        var best = 0L
        var bestShear = 0f
        for (s in shears) {
            cols.fill(0)
            for (y in 0 until h) {
                val shift = s * (y - (h - 1))
                for (x in 0 until w) if (ink.at(x, y)) cols[(x + shift).roundToInt() + pad]++
            }
            var score = 0L
            for (c in cols) score += c.toLong() * c
            if (s == 0f) upright = score
            if (score > best) { best = score; bestShear = s }
        }
        return bestShear >= 0.15f && best > upright * 1.10
    }

    /**
     * Serif feet: at the bottom of stems like n, m, h, i, r, l, I, H, T the ink is much wider
     * than in the middle. Returns that width ratio or null when the glyph is too small.
     */
    fun footRatio(px: Pixels, symbolBox: Rect, ref: Ink): Float? {
        if (!ref.hasInk) return null
        val r = px.clamp(symbolBox)
        val w = r.width()
        if (w < 3 || r.height() < 10) return null
        fun ink(x: Int, y: Int) = colorDist(px[r.left + x, y], ref.background) >= ref.threshold

        var top = -1
        var bottom = -1
        for (y in r.top until r.bottom) {
            var any = false
            for (x in 0 until w) if (ink(x, y)) { any = true; break }
            if (any) { if (top < 0) top = y; bottom = y }
        }
        if (top < 0) return null
        val h = bottom - top + 1
        if (h < 10) return null

        fun runs(y: Int): List<Int> {
            val out = ArrayList<Int>()
            var run = 0
            for (x in 0..w) {
                if (x < w && ink(x, y)) run++ else if (run > 0) { out += run; run = 0 }
            }
            return out
        }
        val mid = ArrayList<Float>()
        for (y in top + (h * 0.35f).toInt()..top + (h * 0.65f).toInt()) runs(y).forEach { mid += it.toFloat() }
        val stem = mid.median()
        if (stem.isNaN() || stem <= 0f) return null
        var foot = 0
        val band = max(2, (h * 0.15f).toInt())
        for (y in bottom - band + 1..bottom) runs(y).maxOrNull()?.let { foot = max(foot, it) }
        return foot / stem
    }

    fun luma(c: Int) = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000
}
