package com.vaultly.app.scan

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** A point in normalized image coordinates (0..1 on both axes). */
data class Pt(val x: Float, val y: Float)

/** Four document corners in normalized coordinates, ordered top-left, top-right, bottom-right, bottom-left. */
data class Quad(val tl: Pt, val tr: Pt, val br: Pt, val bl: Pt) {
    val points: List<Pt> get() = listOf(tl, tr, br, bl)

    fun area(): Float = polygonArea(points)

    /** True when the four corners form a convex shape (no crossed edges). */
    fun isConvex(): Boolean {
        var sign = 0
        for (i in 0 until 4) {
            val a = points[i]
            val b = points[(i + 1) % 4]
            val c = points[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            val s = if (cross > 0) 1 else if (cross < 0) -1 else 0
            if (s != 0) {
                if (sign == 0) sign = s else if (s != sign) return false
            }
        }
        return true
    }

    fun withCorner(index: Int, p: Pt): Quad = when (index) {
        0 -> copy(tl = p)
        1 -> copy(tr = p)
        2 -> copy(br = p)
        else -> copy(bl = p)
    }

    fun encode(): String = points.joinToString(",") { "%.4f,%.4f".format(java.util.Locale.US, it.x, it.y) }

    /** Largest corner movement compared to [other], in normalized units. */
    fun distanceTo(other: Quad): Float =
        points.zip(other.points).maxOf { (a, b) -> hypot(a.x - b.x, a.y - b.y) }

    fun lerp(other: Quad, t: Float): Quad = Quad(
        lerp(tl, other.tl, t), lerp(tr, other.tr, t), lerp(br, other.br, t), lerp(bl, other.bl, t),
    )

    /** Rotates normalized coordinates clockwise by [degrees] (0, 90, 180, 270). */
    fun rotated(degrees: Int): Quad {
        fun r(p: Pt) = when ((degrees % 360 + 360) % 360) {
            90 -> Pt(1f - p.y, p.x)
            180 -> Pt(1f - p.x, 1f - p.y)
            270 -> Pt(p.y, 1f - p.x)
            else -> p
        }
        return fromUnordered(points.map(::r))
    }

    companion object {
        val FULL = Quad(Pt(0f, 0f), Pt(1f, 0f), Pt(1f, 1f), Pt(0f, 1f))

        fun decode(s: String?): Quad? {
            val v = s?.split(',')?.mapNotNull { it.toFloatOrNull() } ?: return null
            if (v.size != 8) return null
            return Quad(Pt(v[0], v[1]), Pt(v[2], v[3]), Pt(v[4], v[5]), Pt(v[6], v[7]))
        }

        private fun lerp(a: Pt, b: Pt, t: Float) = Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

        /** Orders four arbitrary corners clockwise starting at the top-left. */
        fun fromUnordered(pts: List<Pt>): Quad {
            val cx = pts.sumOf { it.x.toDouble() } / 4
            val cy = pts.sumOf { it.y.toDouble() } / 4
            val clockwise = pts.sortedBy { atan2(it.y - cy, it.x - cx) }
            val start = clockwise.indices.minBy { clockwise[it].x + clockwise[it].y }
            val o = List(4) { clockwise[(start + it) % 4] }
            return Quad(o[0], o[1], o[2], o[3])
        }
    }
}

fun polygonArea(pts: List<Pt>): Float {
    var s = 0f
    for (i in pts.indices) {
        val a = pts[i]
        val b = pts[(i + 1) % pts.size]
        s += a.x * b.y - b.x * a.y
    }
    return abs(s) / 2f
}

/**
 * Finds a receipt/document in a small luminance image without native libraries:
 * blur → Otsu threshold → largest bright region → convex hull → best-fitting quadrilateral.
 * Works for the common case of light paper on a darker or textured surface.
 */
object QuadDetector {

    /**
     * [lum] holds 0..255 luminance values, row-major, [w] x [h]. Returns null when no confident document is found.
     * With [strict] (live auto-capture) the region must also look like a receipt: bright paper with
     * printed text on it, clearly brighter than its surroundings, and a plausible shape.
     */
    fun detect(lum: IntArray, w: Int, h: Int, strict: Boolean = false): Quad? {
        if (w < 16 || h < 16) return null
        val blurred = boxBlur(lum, w, h, 2)
        val threshold = otsu(blurred)
        val mask = BooleanArray(w * h) { blurred[it] > threshold }

        val (pixels, count) = largestComponent(mask, w, h) ?: return null
        val total = w * h
        if (count < total * 0.08f || count > total * 0.97f) return null

        val hull = convexHull(boundary(pixels, mask, w, h))
        if (hull.size < 4) return null
        val corners = bestQuad(hull)
        val quad = Quad.fromUnordered(corners.map { Pt(it.first / (w - 1f), it.second / (h - 1f)) })

        // Reject shapes that don't look like a document.
        val area = quad.area()
        if (area < 0.10f) return null
        val fill = count.toFloat() / (area * total)
        if (fill < 0.62f) return null
        val sides = quad.points.indices.map { i ->
            val a = quad.points[i]
            val b = quad.points[(i + 1) % 4]
            hypot(a.x - b.x, a.y - b.y)
        }
        if (sides.min() < 0.08f) return null
        if (!isConvex(quad.points)) return null
        if (strict && !looksLikeReceipt(lum, w, h, quad, threshold)) return null
        return quad
    }

    /** Paper + ink check used before auto-capturing. */
    internal fun looksLikeReceipt(lum: IntArray, w: Int, h: Int, quad: Quad, threshold: Int): Boolean {
        // Shape: not a sliver, not the whole frame.
        val area = quad.area()
        if (area > 0.92f) return false
        val sides = quad.points.indices.map { i ->
            val a = quad.points[i]
            val b = quad.points[(i + 1) % 4]
            hypot((a.x - b.x) * w, (a.y - b.y) * h)
        }
        val shortSide = minOf(sides[0], sides[2]).coerceAtMost(minOf(sides[1], sides[3]))
        val longSide = maxOf(maxOf(sides[0], sides[2]), maxOf(sides[1], sides[3]))
        if (shortSide / longSide < 0.18f) return false

        val pts = quad.points.map { Pt(it.x * (w - 1), it.y * (h - 1)) }
        var inside = 0
        var paperSum = 0L
        var paperCount = 0
        var ink = 0
        var outsideSum = 0L
        var outsideCount = 0
        // Ink is anything clearly darker than the paper around it.
        val inkLevel = threshold - 10
        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = lum[y * w + x]
                if (contains(pts, x + 0.5f, y + 0.5f)) {
                    // Ignore a thin border so the table edge doesn't count as ink.
                    if (!contains(pts, x + 0.5f, y + 0.5f, inset = 2.5f)) continue
                    inside++
                    if (v < inkLevel) ink++ else { paperSum += v; paperCount++ }
                } else {
                    outsideSum += v
                    outsideCount++
                }
            }
        }
        if (inside < 200 || paperCount == 0) return false
        val paper = paperSum / paperCount
        val outside = if (outsideCount > 0) outsideSum / outsideCount else 0
        val inkRatio = ink.toFloat() / inside
        return paper >= 135 && paper - outside >= 45 && inkRatio in 0.012f..0.40f
    }

    /** Point-in-convex-quad test; with [inset] the point must be at least that far inside every edge. */
    private fun contains(p: List<Pt>, x: Float, y: Float, inset: Float = 0f): Boolean {
        var sign = 0
        for (i in 0 until 4) {
            val a = p[i]
            val b = p[(i + 1) % 4]
            val cross = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x)
            val len = hypot(b.x - a.x, b.y - a.y).coerceAtLeast(1e-3f)
            val dist = cross / len
            val s = if (dist > inset) 1 else if (dist < -inset) -1 else 0
            if (s == 0) return false
            if (sign == 0) sign = s else if (s != sign) return false
        }
        return true
    }

    internal fun boxBlur(src: IntArray, w: Int, h: Int, r: Int): IntArray {
        val tmp = IntArray(src.size)
        val out = IntArray(src.size)
        for (y in 0 until h) {
            var sum = 0
            for (x in -r..r) sum += src[y * w + x.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                tmp[y * w + x] = sum / (2 * r + 1)
                sum += src[y * w + (x + r + 1).coerceAtMost(w - 1)] - src[y * w + (x - r).coerceAtLeast(0)]
            }
        }
        for (x in 0 until w) {
            var sum = 0
            for (y in -r..r) sum += tmp[y.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                out[y * w + x] = sum / (2 * r + 1)
                sum += tmp[(y + r + 1).coerceAtMost(h - 1) * w + x] - tmp[(y - r).coerceAtLeast(0) * w + x]
            }
        }
        return out
    }

    internal fun otsu(values: IntArray): Int {
        val hist = IntArray(256)
        values.forEach { hist[it.coerceIn(0, 255)]++ }
        val total = values.size
        var sumAll = 0.0
        for (i in 0..255) sumAll += i * hist[i].toDouble()
        var sumB = 0.0
        var wB = 0
        var best = 0.0
        var threshold = 127
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += t * hist[t].toDouble()
            val mB = sumB / wB
            val mF = (sumAll - sumB) / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > best) { best = between; threshold = t }
        }
        return threshold
    }

    /** Returns the pixel indices of the largest 4-connected true region and its size. */
    private fun largestComponent(mask: BooleanArray, w: Int, h: Int): Pair<IntArray, Int>? {
        val label = IntArray(mask.size)
        val stack = IntArray(mask.size)
        var bestLabel = 0
        var bestSize = 0
        var next = 1
        for (start in mask.indices) {
            if (!mask[start] || label[start] != 0) continue
            var sp = 0
            stack[sp++] = start
            label[start] = next
            var size = 0
            while (sp > 0) {
                val p = stack[--sp]
                size++
                val x = p % w
                val y = p / w
                if (x > 0) { val q = p - 1; if (mask[q] && label[q] == 0) { label[q] = next; stack[sp++] = q } }
                if (x < w - 1) { val q = p + 1; if (mask[q] && label[q] == 0) { label[q] = next; stack[sp++] = q } }
                if (y > 0) { val q = p - w; if (mask[q] && label[q] == 0) { label[q] = next; stack[sp++] = q } }
                if (y < h - 1) { val q = p + w; if (mask[q] && label[q] == 0) { label[q] = next; stack[sp++] = q } }
            }
            if (size > bestSize) { bestSize = size; bestLabel = next }
            next++
        }
        if (bestLabel == 0) return null
        val out = IntArray(bestSize)
        var i = 0
        for (p in label.indices) if (label[p] == bestLabel) out[i++] = p
        // Keep only this component in the mask for boundary extraction.
        for (p in mask.indices) mask[p] = label[p] == bestLabel
        return out to bestSize
    }

    private fun boundary(pixels: IntArray, mask: BooleanArray, w: Int, h: Int): List<Pair<Float, Float>> {
        val out = ArrayList<Pair<Float, Float>>()
        for (p in pixels) {
            val x = p % w
            val y = p / w
            val edge = x == 0 || y == 0 || x == w - 1 || y == h - 1 ||
                !mask[p - 1] || !mask[p + 1] || !mask[p - w] || !mask[p + w]
            if (edge) out += x.toFloat() to y.toFloat()
        }
        return out
    }

    internal fun convexHull(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val pts = points.distinct().sortedWith(compareBy({ it.first }, { it.second }))
        if (pts.size < 3) return pts
        fun cross(o: Pair<Float, Float>, a: Pair<Float, Float>, b: Pair<Float, Float>) =
            (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)
        val lower = ArrayList<Pair<Float, Float>>()
        for (p in pts) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) lower.removeAt(lower.size - 1)
            lower += p
        }
        val upper = ArrayList<Pair<Float, Float>>()
        for (p in pts.asReversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) upper.removeAt(upper.size - 1)
            upper += p
        }
        lower.removeAt(lower.size - 1)
        upper.removeAt(upper.size - 1)
        return lower + upper
    }

    /** Picks four hull vertices that enclose the largest area (local search from the diagonal extremes). */
    internal fun bestQuad(hull: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val n = hull.size
        val idx = intArrayOf(
            hull.indices.minBy { hull[it].first + hull[it].second },
            hull.indices.maxBy { hull[it].first - hull[it].second },
            hull.indices.maxBy { hull[it].first + hull[it].second },
            hull.indices.minBy { hull[it].first - hull[it].second },
        )
        fun area(ix: IntArray): Float = polygonArea(ix.map { Pt(hull[it].first, hull[it].second) })
        var best = area(idx)
        repeat(4) {
            for (k in 0 until 4) {
                for (c in 0 until n) {
                    if (c in idx) continue
                    val trial = idx.copyOf().also { it[k] = c }
                    // Keep corners in hull order so the polygon stays simple.
                    val sorted = trial.sortedArray()
                    val a = area(sorted)
                    if (a > best) { best = a; sorted.copyInto(idx) }
                }
            }
        }
        return idx.sorted().map { hull[it] }
    }

    private fun isConvex(pts: List<Pt>): Boolean {
        var sign = 0
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % 4]
            val c = pts[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            val s = if (cross > 0) 1 else if (cross < 0) -1 else 0
            if (s != 0) {
                if (sign == 0) sign = s else if (s != sign) return false
            }
        }
        return true
    }
}
