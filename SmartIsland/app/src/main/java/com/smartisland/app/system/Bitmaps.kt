package com.smartisland.app.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon

object Bitmaps {

    /** Downscales so the island never holds full-resolution album art in memory. */
    fun shrink(src: Bitmap?, maxPx: Int = 160): Bitmap? {
        if (src == null || src.isRecycled) return null
        if (src.width <= maxPx && src.height <= maxPx) return src.copySafe()
        val scale = maxPx.toFloat() / maxOf(src.width, src.height)
        return runCatching {
            Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
        }.getOrNull()
    }

    fun fromDrawable(d: Drawable?, sizePx: Int = 128): Bitmap? {
        if (d == null) return null
        if (d is BitmapDrawable && d.bitmap != null) return shrink(d.bitmap, sizePx)
        return runCatching {
            val w = d.intrinsicWidth.takeIf { it > 0 } ?: sizePx
            val h = d.intrinsicHeight.takeIf { it > 0 } ?: sizePx
            val b = Bitmap.createBitmap(minOf(w, sizePx), minOf(h, sizePx), Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            d.setBounds(0, 0, c.width, c.height)
            d.draw(c)
            b
        }.getOrNull()
    }

    fun fromIcon(context: Context, icon: Icon?, sizePx: Int = 128): Bitmap? =
        runCatching { fromDrawable(icon?.loadDrawable(context), sizePx) }.getOrNull()

    fun appIcon(context: Context, pkg: String, sizePx: Int = 96): Bitmap? =
        runCatching { fromDrawable(context.packageManager.getApplicationIcon(pkg), sizePx) }.getOrNull()

    /** Vivid average color of an image, used to tint the waveform like iOS does. */
    fun accentOf(b: Bitmap?, fallback: Int): Int {
        if (b == null) return fallback
        return runCatching {
            val px = Bitmap.createScaledBitmap(b, 1, 1, true).getPixel(0, 0)
            val hsv = FloatArray(3)
            Color.colorToHSV(px, hsv)
            hsv[1] = (hsv[1] * 1.4f).coerceIn(0.45f, 1f)
            hsv[2] = hsv[2].coerceAtLeast(0.85f)
            Color.HSVToColor(hsv)
        }.getOrDefault(fallback)
    }

    private fun Bitmap.copySafe(): Bitmap? = runCatching {
        copy(config ?: Bitmap.Config.ARGB_8888, false)
    }.getOrNull()
}
