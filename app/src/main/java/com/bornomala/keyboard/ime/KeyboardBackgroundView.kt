package com.bornomala.keyboard.ime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.View
import com.bornomala.keyboard.theme.KeyboardTheme
import java.io.File

/**
 * Draws the keyboard background: a solid colour, or a photo scaled to fill the keyboard
 * (centre-crop with an adjustable vertical position) with an optional darkening layer.
 */
class KeyboardBackgroundView(context: Context) : View(context) {

    private var color = 0
    private var bitmap: Bitmap? = null
    private var bitmapKey: String? = null
    private var align = 0.5f
    private var dimColor = 0
    private val matrix = Matrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun apply(theme: KeyboardTheme) {
        color = theme.background
        align = theme.imageAlign
        dimColor = (theme.imageDim.coerceIn(0, 90) * 255 / 100) shl 24
        val key = when {
            theme.backgroundFile != null -> "file:" + theme.backgroundFile + ":" + File(theme.backgroundFile).lastModified()
            theme.backgroundImage != 0 -> "res:" + theme.backgroundImage
            else -> null
        }
        if (key != bitmapKey) {
            bitmapKey = key
            bitmap = when {
                theme.backgroundFile != null -> decodeFile(theme.backgroundFile)
                theme.backgroundImage != 0 -> decodeResource(theme.backgroundImage)
                else -> null
            }
        }
        updateMatrix()
        invalidate()
    }

    private fun bounds(): Pair<Int, Int> {
        val dm = resources.displayMetrics
        return maxOf(dm.widthPixels, dm.heightPixels) to maxOf(dm.widthPixels, dm.heightPixels) / 2
    }

    private fun sampleSize(w: Int, h: Int): Int {
        val (maxW, maxH) = bounds()
        var s = 1
        while (w / (s * 2) >= maxW && h / (s * 2) >= maxH) s *= 2
        return s
    }

    private fun decodeFile(path: String): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize(o.outWidth, o.outHeight) })
    } catch (e: Throwable) {
        null
    }

    private fun decodeResource(res: Int): Bitmap? = try {
        BitmapFactory.decodeResource(resources, res)
    } catch (e: Throwable) {
        null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = updateMatrix()

    private fun updateMatrix() {
        val b = bitmap ?: return
        if (width == 0 || height == 0) return
        val scale = maxOf(width.toFloat() / b.width, height.toFloat() / b.height)
        val dx = (width - b.width * scale) / 2f
        val dy = (height - b.height * scale) * align
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(color)
        bitmap?.let { canvas.drawBitmap(it, matrix, paint) }
        if (dimColor != 0) canvas.drawColor(dimColor)
    }
}
