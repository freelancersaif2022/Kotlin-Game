package com.saif.imagetopdf.engine

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build

/** Maps a detected style to a real font on the phone (Roboto, Noto Serif, Droid Sans Mono…). */
object Fonts {
    private val cache = HashMap<Triple<Family, Int, Boolean>, Typeface>()

    fun typeface(family: Family, weight: Int, italic: Boolean): Typeface = synchronized(cache) {
        cache.getOrPut(Triple(family, weight, italic)) { create(family, weight, italic) }
    }

    private fun create(family: Family, weight: Int, italic: Boolean): Typeface {
        val base = when (family) {
            Family.SANS -> Typeface.SANS_SERIF
            Family.SERIF -> Typeface.SERIF
            Family.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            Family.MONO -> Typeface.MONOSPACE
        }
        if (Build.VERSION.SDK_INT >= 28) return Typeface.create(base, weight, italic)

        val bold = weight >= 700
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        if (family == Family.SANS) {
            val name = when {
                weight <= 300 -> "sans-serif-light"
                weight == 500 -> "sans-serif-medium"
                weight >= 900 -> "sans-serif-black"
                else -> null
            }
            if (name != null) return Typeface.create(name, if (italic) Typeface.ITALIC else Typeface.NORMAL)
        }
        return Typeface.create(base, style)
    }

    /** Applies the typeface plus synthetic bold/slant when the phone has no real variant. */
    fun apply(paint: Paint, family: Family, weight: Int, italic: Boolean) {
        val tf = typeface(family, weight, italic)
        paint.typeface = tf
        paint.isFakeBoldText = weight >= 700 && family == Family.MONO && Build.VERSION.SDK_INT < 28
        paint.textSkewX = if (italic && !tf.isItalic) -0.22f else 0f
    }

    /**
     * Font size (px) whose rendered glyphs of [text] are exactly [inkHeight] tall, and how far
     * those glyphs reach below the baseline at that size (descenders).
     */
    fun fitSize(text: String, family: Family, inkHeight: Float): Pair<Float, Float> {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        apply(paint, family, 400, false)
        paint.textSize = 100f
        val b = Rect()
        paint.getTextBounds(text, 0, text.length, b)
        if (b.height() <= 0) return inkHeight * 0.8f to 0f
        val size = 100f * inkHeight / b.height()
        return size to b.bottom * size / 100f
    }
}
