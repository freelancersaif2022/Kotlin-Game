package com.saif.imagetopdf.engine

import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/** Turns OCR output plus the pixels into positioned, styled lines of text. */
object TextAnalyzer {

    /** Below this overlap no phone font draws the word like the photo – keep its pixels instead. */
    private const val MIN_MATCH = 0.5f

    private class RawWord(
        val text: String, val box: Rect, val x: Float, val width: Float, val ink: Ink,
        val target: FontMatcher.Target?, val confidence: Float,
    )

    private class RawLine(
        val text: String, val originX: Float, val originY: Float, val angle: Float, val height: Float,
        val words: List<RawWord>, val mono: Boolean,
    )

    fun analyze(px: Pixels, lines: List<Text.Line>, progress: (Float) -> Unit): List<TextLine> {
        val raws = lines.mapNotNull { analyzeLine(px, it) }
        if (raws.isEmpty()) return emptyList()

        // 1. Font family per line, by rendering every family and comparing with the photo.
        val familyScores = raws.mapIndexed { i, raw ->
            progress(0.45f * i / raws.size)
            FontMatcher.familyScores(raw.words.mapNotNull { w -> w.target?.let { w.text to it } })
        }
        val pageTotals = FontMatcher.FAMILIES.associateWith { f -> familyScores.sumOf { (it?.get(f) ?: 0f).toDouble() } }
        val pageFamily = pageTotals.maxByOrNull { it.value }?.key ?: Family.SANS
        val families = raws.mapIndexed { i, raw ->
            val scores = familyScores[i]
            when {
                scores == null -> if (raw.mono) Family.MONO else pageFamily
                else -> {
                    val best = scores.maxBy { it.value }
                    // A line only leaves the page's main family when it clearly fits another one.
                    if (best.value > (scores[pageFamily] ?: 0f) + 0.02f) best.key else pageFamily
                }
            }
        }

        // 2. Weight and slant of every word in its line's family.
        val styles = raws.mapIndexed { i, raw ->
            progress(0.45f + 0.5f * i / raws.size)
            raw.words.map { w ->
                val t = w.target ?: return@map null
                if (w.text.count { it.isLetterOrDigit() } < 2) null else FontMatcher.bestStyle(w.text, t, families[i])
            }
        }
        val pageWeight = majority(styles.flatten().filterNotNull().map { it.weight }) ?: 400

        progress(1f)
        return raws.mapIndexed { i, raw ->
            val family = families[i]
            val lineStyles = styles[i].filterNotNull()
            val lineWeight = majority(lineStyles.map { it.weight }) ?: pageWeight
            val lineItalic = lineStyles.count { it.italic } * 2 > lineStyles.size
            val (size, descent) = Fonts.fitSize(raw.text, family, raw.height)
            TextLine(
                originX = raw.originX,
                originY = raw.originY,
                angle = raw.angle,
                height = raw.height,
                baseline = raw.height - descent,
                fontSize = size,
                family = family,
                words = raw.words.mapIndexed { j, w ->
                    val s = styles[i][j]
                    val longWord = w.text.count { it.isLetter() } >= 3
                    val exact = w.confidence < 0.4f || (s != null && longWord && s.score < MIN_MATCH)
                    Word(
                        text = w.text,
                        x = w.x,
                        width = w.width,
                        color = if (w.ink.hasInk) w.ink.color else Color.BLACK,
                        weight = s?.weight ?: lineWeight,
                        italic = s?.italic ?: lineItalic,
                        background = w.ink.background,
                        box = w.box,
                        exact = exact,
                    )
                },
            )
        }
    }

    private fun majority(values: List<Int>): Int? = values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

    private fun analyzeLine(px: Pixels, line: Text.Line): RawLine? {
        val box = line.boundingBox ?: return null
        val elements = line.elements.filter { it.boundingBox != null && it.text.isNotBlank() }
        if (elements.isEmpty() || box.width() < 2 || box.height() < 4) return null

        val corners = line.cornerPoints
        val angle = if (corners != null && corners.size == 4)
            Math.toDegrees(atan2((corners[1].y - corners[0].y).toDouble(), (corners[1].x - corners[0].x).toDouble())).toFloat()
        else 0f
        val straight = abs(angle) < 2.5f || corners == null || corners.size != 4

        val lineInk = InkAnalyzer.analyze(px, box)
        val originX: Float
        val originY: Float
        val height: Float
        var ux = 1f
        var uy = 0f
        if (straight) {
            val rows = lineInk.inkRows()
            originX = lineInk.box.left.toFloat()
            originY = lineInk.box.top + (rows?.first ?: 0).toFloat()
            height = (rows?.let { it.last - it.first + 1 } ?: lineInk.h).toFloat()
        } else {
            val c = corners!!
            val len = dist(c[0], c[1]).coerceAtLeast(1f)
            ux = (c[1].x - c[0].x) / len
            uy = (c[1].y - c[0].y) / len
            originX = c[0].x.toFloat()
            originY = c[0].y.toFloat()
            height = dist(c[0], c[3]).coerceAtLeast(4f)
        }

        val words = elements.map { e ->
            val eb = e.boundingBox!!
            val ink = InkAnalyzer.analyze(px, eb)
            val ec = e.cornerPoints
            val x: Float
            val width: Float
            if (straight || ec == null || ec.size != 4) {
                x = eb.left - originX
                width = eb.width().toFloat()
            } else {
                x = (ec[0].x - originX) * ux + (ec[0].y - originY) * uy
                width = dist(ec[0], ec[1])
            }
            RawWord(
                text = e.text, box = Rect(eb), x = x, width = width, ink = ink,
                // Rotated words would need a rotated crop; they keep the line's style.
                target = if (straight) FontMatcher.target(ink) else null,
                confidence = e.confidence,
            )
        }
        return RawLine(line.text, originX, originY, if (straight) 0f else angle, height, words, isMonospace(elements))
    }

    /** Monospaced fonts advance by the same amount for every letter. */
    private fun isMonospace(elements: List<Text.Element>): Boolean {
        val deltas = ArrayList<Float>()
        for (e in elements) {
            val centers = e.symbols.mapNotNull { it.boundingBox?.exactCenterX() }
            for (i in 1 until centers.size) deltas += centers[i] - centers[i - 1]
        }
        if (deltas.size < 6) return false
        val mean = deltas.average().toFloat()
        if (mean <= 0f) return false
        val sd = sqrt(deltas.sumOf { ((it - mean) * (it - mean)).toDouble() } / deltas.size).toFloat()
        return sd / mean < 0.14f
    }

    /** Whole boxes of recognised lines, so the background pass can look past the letters. */
    fun textMask(px: Pixels, lines: List<Text.Line>): BooleanArray {
        val mask = BooleanArray(px.w * px.h)
        for (line in lines) {
            val b = line.boundingBox ?: continue
            val r = px.clamp(Rect(b.left - 2, b.top - 2, b.right + 2, b.bottom + 2))
            for (y in r.top until r.bottom) mask.fill(true, y * px.w + r.left, y * px.w + r.right)
        }
        return mask
    }

    /**
     * Pixels that are explained by retyped text: its ink, its paper, or the antialiased blend
     * between them. Anything else inside a word box – a seal or signature crossing the text –
     * is left for the graphics pass, so overlapping marks are kept whole.
     */
    fun textInkMask(px: Pixels, lines: List<TextLine>): BooleanArray {
        val mask = BooleanArray(px.w * px.h)
        for (line in lines) for (w in line.words) {
            if (w.exact) continue
            val r = px.clamp(Rect(w.box.left - 2, w.box.top - 2, w.box.right + 2, w.box.bottom + 2))
            for (y in r.top until r.bottom) for (x in r.left until r.right) {
                val i = y * px.w + x
                if (blendDistance(px.data[i], w.color, w.background) < 40f) mask[i] = true
            }
        }
        return mask
    }

    /** RGB distance from [c] to the straight line of colours between [a] and [b]. */
    private fun blendDistance(c: Int, a: Int, b: Int): Float {
        val ar = Color.red(a).toFloat(); val ag = Color.green(a).toFloat(); val ab = Color.blue(a).toFloat()
        val dr = Color.red(b) - ar; val dg = Color.green(b) - ag; val db = Color.blue(b) - ab
        val cr = Color.red(c) - ar; val cg = Color.green(c) - ag; val cb = Color.blue(c) - ab
        val len = dr * dr + dg * dg + db * db
        val t = if (len <= 0f) 0f else ((cr * dr + cg * dg + cb * db) / len).coerceIn(0f, 1f)
        val er = cr - t * dr; val eg = cg - t * dg; val eb = cb - t * db
        return sqrt(er * er + eg * eg + eb * eb)
    }

    private fun dist(a: Point, b: Point) = hypot((b.x - a.x).toFloat(), (b.y - a.y).toFloat())
}
