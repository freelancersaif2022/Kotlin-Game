package com.saif.imagetopdf.engine

import android.graphics.Color
import kotlin.math.max

/**
 * Rebuilds everything that is not text as vector rectangles: page colour, coloured bands,
 * boxes, table cells, and thin rule/border lines.
 */
object BackgroundBuilder {

    class Result(val pageColor: Int, val shapes: List<ColorRect>)

    fun build(px: Pixels, textMask: BooleanArray): Result {
        val cell = max(4, px.w / 160)
        val cols = (px.w + cell - 1) / cell
        val rows = (px.h + cell - 1) / cell
        val color = IntArray(cols * rows)
        val key = IntArray(cols * rows) { -1 }

        // 1. Dominant colour of each cell, ignoring the letters.
        val keys = IntArray(cell * cell)
        val all = IntArray(cell * cell)
        for (cy in 0 until rows) for (cx in 0 until cols) {
            var n = 0
            val x0 = cx * cell
            val y0 = cy * cell
            for (y in y0 until minOf(y0 + cell, px.h)) for (x in x0 until minOf(x0 + cell, px.w)) {
                if (textMask[y * px.w + x]) continue
                val c = px[x, y]
                all[n] = c
                keys[n] = quant(c)
                n++
            }
            if (n < cell * cell / 5) continue
            val sorted = keys.copyOf(n).apply { sort() }
            var bestKey = sorted[0]
            var bestRun = 0
            var run = 0
            for (i in 0 until n) {
                run = if (i > 0 && sorted[i] == sorted[i - 1]) run + 1 else 1
                if (run > bestRun) { bestRun = run; bestKey = sorted[i] }
            }
            var r = 0; var g = 0; var b = 0; var m = 0
            for (i in 0 until n) if (keys[i] == bestKey) {
                r += Color.red(all[i]); g += Color.green(all[i]); b += Color.blue(all[i]); m++
            }
            color[cy * cols + cx] = Color.rgb(r / m, g / m, b / m)
            key[cy * cols + cx] = bestKey
        }

        // Cells hidden under text take their neighbour's colour.
        for (cy in 0 until rows) for (cx in 0 until cols) {
            val i = cy * cols + cx
            if (key[i] >= 0) continue
            val src = when {
                cx > 0 && key[i - 1] >= 0 -> i - 1
                cy > 0 && key[i - cols] >= 0 -> i - cols
                else -> -1
            }
            if (src >= 0) { key[i] = key[src]; color[i] = color[src] }
        }
        for (cy in rows - 1 downTo 0) for (cx in cols - 1 downTo 0) {
            val i = cy * cols + cx
            if (key[i] >= 0) continue
            val src = when {
                cx < cols - 1 && key[i + 1] >= 0 -> i + 1
                cy < rows - 1 && key[i + cols] >= 0 -> i + cols
                else -> -1
            }
            if (src >= 0) { key[i] = key[src]; color[i] = color[src] }
        }

        // 2. Page colour = most common cell colour.
        val counts = HashMap<Int, Int>()
        for (k in key) if (k >= 0) counts[k] = (counts[k] ?: 0) + 1
        val pageKey = counts.maxByOrNull { it.value }?.key ?: quant(Color.WHITE)
        val pageColor = averageOf(color, key, pageKey)

        // 3. Merge runs of equal cells into rectangles (horizontal runs, then stacked rows).
        val shapes = ArrayList<ColorRect>()
        data class Open(val c0: Int, val c1: Int, val key: Int, val color: Int, val r0: Int)
        var open = HashMap<Triple<Int, Int, Int>, Open>()
        fun close(o: Open, rEnd: Int) {
            shapes += ColorRect(
                (o.c0 * cell).toFloat(), (o.r0 * cell).toFloat(),
                minOf((o.c1 + 1) * cell, px.w).toFloat(), minOf(rEnd * cell, px.h).toFloat(), o.color,
            )
        }
        for (cy in 0 until rows) {
            val next = HashMap<Triple<Int, Int, Int>, Open>()
            var cx = 0
            while (cx < cols) {
                val i = cy * cols + cx
                val k = key[i]
                var end = cx
                while (end + 1 < cols && key[cy * cols + end + 1] == k) end++
                val isPage = k < 0 || k == pageKey || colorDist(color[i], pageColor) < 10f
                if (!isPage) {
                    val id = Triple(cx, end, k)
                    next[id] = open.remove(id) ?: Open(cx, end, k, color[i], cy)
                }
                cx = end + 1
            }
            open.values.forEach { close(it, cy) }
            open = next
        }
        open.values.forEach { close(it, rows) }

        // 4. Thin lines (underlines, table borders, dividers) that are thinner than a cell.
        shapes += rules(px, textMask, color, cols, cell, horizontal = true)
        shapes += rules(px, textMask, color, cols, cell, horizontal = false)
        return Result(pageColor, shapes)
    }

    private fun rules(
        px: Pixels, textMask: BooleanArray, cellColor: IntArray, cols: Int, cell: Int, horizontal: Boolean,
    ): List<ColorRect> {
        val outer = if (horizontal) px.h else px.w
        val inner = if (horizontal) px.w else px.h
        val minLen = max(24, inner / 25)
        val maxThick = max(3, cell)

        class Seg(var a0: Int, var a1: Int, val b0: Int, var b1: Int, var r: Long, var g: Long, var b: Long, var n: Int)
        val active = ArrayList<Seg>()
        val done = ArrayList<Seg>()

        for (o in 0 until outer) {
            var start = -1
            var sr = 0L; var sg = 0L; var sb = 0L
            for (i in 0..inner) {
                val line = if (i < inner) {
                    val x = if (horizontal) i else o
                    val y = if (horizontal) o else i
                    val idx = y * px.w + x
                    if (textMask[idx]) false
                    else {
                        val c = px.data[idx]
                        val bg = cellColor[(y / cell) * cols + x / cell]
                        if (colorDist(c, bg) > 55f) {
                            if (start < 0) { start = i; sr = 0; sg = 0; sb = 0 }
                            sr += Color.red(c); sg += Color.green(c); sb += Color.blue(c)
                            true
                        } else false
                    }
                } else false
                if (!line && start >= 0) {
                    val len = i - start
                    if (len >= minLen) {
                        val match = active.firstOrNull { it.b1 == o - 1 && kotlin.math.abs(it.a0 - start) <= 3 && kotlin.math.abs(it.a1 - i) <= 3 }
                        if (match != null) {
                            match.b1 = o; match.a0 = minOf(match.a0, start); match.a1 = max(match.a1, i)
                            match.r += sr; match.g += sg; match.b += sb; match.n += len
                        } else active += Seg(start, i, o, o, sr, sg, sb, len)
                    }
                    start = -1
                }
            }
            val iter = active.iterator()
            while (iter.hasNext()) { val s = iter.next(); if (s.b1 < o) { done += s; iter.remove() } }
        }
        done += active

        return done.filter { it.b1 - it.b0 + 1 <= maxThick }.map { s ->
            val c = Color.rgb((s.r / s.n).toInt(), (s.g / s.n).toInt(), (s.b / s.n).toInt())
            if (horizontal) ColorRect(s.a0.toFloat(), s.b0.toFloat(), s.a1.toFloat(), (s.b1 + 1).toFloat(), c)
            else ColorRect(s.b0.toFloat(), s.a0.toFloat(), (s.b1 + 1).toFloat(), s.a1.toFloat(), c)
        }
    }

    private fun quant(c: Int) = (Color.red(c) shr 4 shl 8) or (Color.green(c) shr 4 shl 4) or (Color.blue(c) shr 4)

    private fun averageOf(color: IntArray, key: IntArray, k: Int): Int {
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (i in key.indices) if (key[i] == k) {
            r += Color.red(color[i]); g += Color.green(color[i]); b += Color.blue(color[i]); n++
        }
        return if (n == 0) Color.WHITE else Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }
}
