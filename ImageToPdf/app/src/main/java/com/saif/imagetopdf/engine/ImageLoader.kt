package com.saif.imagetopdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

object ImageLoader {

    /**
     * Decodes the picked image upright (EXIF rotation applied) and scales it so the long side
     * is between [minSide] and [maxSide]: big enough for OCR, small enough for memory.
     */
    fun load(context: Context, uri: Uri, minSide: Int = 1600, maxSide: Int = 2600): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "This file is not a readable image." }

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("This file is not a readable image.")

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val longSide = max(decoded.width, decoded.height).toFloat()
        val scale = when {
            longSide > maxSide -> maxSide / longSide
            longSide < minSide -> (minSide / longSide).coerceAtMost(2.5f)
            else -> 1f
        }
        if (scale == 1f && rotation == 0) return decoded

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (out !== decoded) decoded.recycle()
        return out
    }
}
