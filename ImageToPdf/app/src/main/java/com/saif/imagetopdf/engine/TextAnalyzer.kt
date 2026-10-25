package com.saif.imagetopdf.engine

import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/** Turns OCR output plus the pixels into positioned, styled lines of text. */
object TextAnalyzer {

    private const val SERIF_PROBES = "nmhirlIHMNT"

    private class RawWord(
        val text: String, val x: Float, val width: Float, val color: Int, val stem: Float, val italic: Boolean,
    )

    private class RawLine(
        val text: String, val originX: Float, val originY: Float, val angle: Float, val height: Float,
        val family: Family?, val serifVote: Boolean?, val words: List<RawWord>,
    )

    fun analyze(px: Pixels, text: Text): List<TextLine> {
        val raws = ArrayList<RawLine>()
        for (block in text.textBlocks) for (line in block.lines) analyzeLine(px, line)?.let { raws += it }
        if (raws.isEmpty()) return emptyList()

        // Lines with too few clues borrow the page-wide serif/sans decision.
        val serifLines = raws.count { it.serifVote == true }
        val sansLines = raws.count { it.serifVote == false }
        val pageFamily = if (serifLines > sansLines) Family.SERIF else Family.SANS

        val sized = raws.map { raw ->
            val family = raw.family ?: pageFamily
            val (size, descent) = Fonts.fitSize(raw.text, family, raw.height)
            Triple(raw, family, size to descent)
        }

        // Weight: absolute stem/em ratio for the page's body text, relative contrast for emphasis.
        val ratios = sized.flatMap { (raw, _, s) -> raw.words.map { it.stem / s.first } }.filter { !it.isNaN() }
        val pageRatio = ratios.median()
        val baseWeight = when {
            pageRatio.isNaN() -> 400
            pageRatio < 0.068f -> 300
            pageRatio < 0.105f -> 400
            pageRatio < 0.125f -> 500
            pageRatio < 0.17f -> 700
            else -> 900
        }

        return sized.map { (raw, family, s) ->
            val (size, descent) = s
            TextLine(
                originX = raw.originX,
                originY = raw.originY,
                angle = raw.angle,
                height = raw.height,
                baseline = raw.height - descent,
                fontSize = size,
                family = family,
                words = raw.words.map { w ->
                    val rel = (w.stem / size) / pageRatio
                    val weight = when {
                        rel.isNaN() -> baseWeight
                        rel > 1.30f -> if (baseWeight >= 700) 900 else 700
                        rel > 1.15f && baseWeight <= 400 -> 500
                        rel < 0.75f && baseWeight >= 400 -> 300
                        else -> baseWeight
                    }
                    Word(w.text, w.x, w.width, w.color, weight, w.italic)
                },
            )
        }
    }

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
                text = e.text,
                x = x,
                width = width,
                color = if (ink.hasInk) ink.color else lineInk.color,
                stem = InkAnalyzer.stemWidth(ink),
                italic = InkAnalyzer.isItalic(ink),
            )
        }

        val serifVote = serifVote(px, elements, lineInk)
        val family = when {
            isMonospace(elements) -> Family.MONO
            serifVote == true -> Family.SERIF
            serifVote == false -> Family.SANS
            else -> null
        }
        return RawLine(line.text, originX, originY, if (straight) 0f else angle, height, family, serifVote, words)
    }

    /** true = serif, false = sans, null = not enough probe letters on this line. */
    private fun serifVote(px: Pixels, elements: List<Text.Element>, ink: Ink): Boolean? {
        var serif = 0
        var sans = 0
        for (e in elements) for (s in e.symbols) {
            if (s.text.length != 1 || s.text[0] !in SERIF_PROBES) continue
            val b = s.boundingBox ?: continue
            val ratio = InkAnalyzer.footRatio(px, b, ink) ?: continue
            if (ratio > 1.65f) serif++ else if (ratio < 1.35f) sans++
        }
        if (serif + sans < 2) return null
        return serif > sans
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

    /** Pixels covered by recognised text, so the background pass can look past the letters. */
    fun textMask(px: Pixels, text: Text): BooleanArray {
        val mask = BooleanArray(px.w * px.h)
        for (block in text.textBlocks) for (line in block.lines) {
            val b = line.boundingBox ?: continue
            val r = px.clamp(Rect(b.left - 2, b.top - 2, b.right + 2, b.bottom + 2))
            for (y in r.top until r.bottom) mask.fill(true, y * px.w + r.left, y * px.w + r.right)
        }
        return mask
    }

    private fun dist(a: Point, b: Point) = hypot((b.x - a.x).toFloat(), (b.y - a.y).toFloat())
}
