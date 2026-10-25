package com.saif.imagetopdf.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.sqrt

class Pixels(val w: Int, val h: Int, val data: IntArray) {
    operator fun get(x: Int, y: Int) = data[y * w + x]

    fun clamp(r: Rect): Rect = Rect(
        r.left.coerceIn(0, w - 1),
        r.top.coerceIn(0, h - 1),
        r.right.coerceIn(r.left.coerceIn(0, w - 1) + 1, w),
        r.bottom.coerceIn(r.top.coerceIn(0, h - 1) + 1, h),
    )

    companion object {
        fun of(b: Bitmap) = Pixels(
            b.width, b.height,
            IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) },
        )
    }
}

fun colorDist(a: Int, b: Int): Float {
    val dr = Color.red(a) - Color.red(b)
    val dg = Color.green(a) - Color.green(b)
    val db = Color.blue(a) - Color.blue(b)
    return sqrt((dr * dr + dg * dg + db * db).toFloat())
}

/** Per-channel median, robust against the odd stray pixel. */
fun medianColor(colors: IntArray, count: Int = colors.size): Int {
    if (count == 0) return Color.WHITE
    val r = IntArray(count) { Color.red(colors[it]) }.apply { sort() }
    val g = IntArray(count) { Color.green(colors[it]) }.apply { sort() }
    val b = IntArray(count) { Color.blue(colors[it]) }.apply { sort() }
    return Color.rgb(r[count / 2], g[count / 2], b[count / 2])
}

fun FloatArray.median(): Float = if (isEmpty()) Float.NaN else copyOf().apply { sort() }[size / 2]
fun List<Float>.median(): Float = if (isEmpty()) Float.NaN else sorted()[size / 2]
