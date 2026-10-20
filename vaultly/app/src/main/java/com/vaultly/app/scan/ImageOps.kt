package com.vaultly.app.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

enum class ScanFilter(val label: String) { AUTO("Enhance"), ORIGINAL("Original"), GRAY("Grayscale"), BW("B&W") }

/** Bitmap helpers for the scanner: decoding, document detection, perspective crop and filters. */
object ImageOps {

    /** Decodes [uri] upright (EXIF-aware) with its longest side at most [maxSide]. */
    fun decode(context: Context, uri: Uri, maxSide: Int = 2600): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return@runCatching null
        var sample = 1
        while (longest / (sample * 2) >= maxSide) sample *= 2
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        val upright = rotate(raw, degrees)
        scaleDown(upright, maxSide)
    }.getOrNull()

    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxSide) return src
        val r = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(src, (src.width * r).roundToInt(), (src.height * r).roundToInt(), true)
    }

    /** Runs [QuadDetector] on a small copy of [src]. */
    fun detect(src: Bitmap): Quad? {
        val tw = 200
        val th = (src.height * tw / src.width.toFloat()).roundToInt().coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(src, tw, th, true)
        val px = IntArray(tw * th)
        small.getPixels(px, 0, tw, 0, 0, tw, th)
        val lum = IntArray(px.size) { i ->
            val c = px[i]
            (((c shr 16) and 255) * 299 + ((c shr 8) and 255) * 587 + (c and 255) * 114) / 1000
        }
        return QuadDetector.detect(lum, tw, th)
    }

    /** Perspective-corrects the region inside [quad] into a flat rectangle. */
    fun warp(src: Bitmap, quad: Quad, maxSide: Int = 2400): Bitmap {
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val p = quad.points.map { it.x * w to it.y * h }
        fun d(a: Pair<Float, Float>, b: Pair<Float, Float>) = hypot(a.first - b.first, a.second - b.second)
        var outW = max(d(p[0], p[1]), d(p[3], p[2]))
        var outH = max(d(p[0], p[3]), d(p[1], p[2]))
        val scale = minOf(1f, maxSide / max(outW, outH))
        outW = (outW * scale).coerceAtLeast(32f)
        outH = (outH * scale).coerceAtLeast(32f)
        val srcPts = floatArrayOf(p[0].first, p[0].second, p[1].first, p[1].second, p[2].first, p[2].second, p[3].first, p[3].second)
        val dstPts = floatArrayOf(0f, 0f, outW, 0f, outW, outH, 0f, outH)
        val m = Matrix()
        m.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
        val out = Bitmap.createBitmap(outW.roundToInt(), outH.roundToInt(), Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    fun applyFilter(src: Bitmap, filter: ScanFilter): Bitmap = when (filter) {
        ScanFilter.ORIGINAL -> src
        ScanFilter.AUTO -> colorMatrix(src, enhanceMatrix(src, gray = false))
        ScanFilter.GRAY -> colorMatrix(src, enhanceMatrix(src, gray = true))
        ScanFilter.BW -> threshold(colorMatrix(src, enhanceMatrix(src, gray = true)))
    }

    private fun colorMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        return out
    }

    /**
     * Per-channel levels: the paper (bright percentile) becomes white and ink (dark percentile)
     * becomes deep black, which also removes colour casts from indoor lighting.
     */
    private fun enhanceMatrix(src: Bitmap, gray: Boolean): ColorMatrix {
        val sample = Bitmap.createScaledBitmap(src, 160, (160f * src.height / src.width).roundToInt().coerceAtLeast(8), true)
        val px = IntArray(sample.width * sample.height)
        sample.getPixels(px, 0, sample.width, 0, 0, sample.width, sample.height)
        val hist = Array(3) { IntArray(256) }
        px.forEach { c ->
            hist[0][(c shr 16) and 255]++
            hist[1][(c shr 8) and 255]++
            hist[2][c and 255]++
        }
        fun pct(h: IntArray, p: Float): Int {
            val target = (px.size * p).toInt()
            var acc = 0
            for (i in 0..255) { acc += h[i]; if (acc >= target) return i }
            return 255
        }
        val cm = ColorMatrix()
        if (gray) cm.setSaturation(0f)
        val lo = FloatArray(3) { pct(hist[it], 0.02f).toFloat() }
        val hi = FloatArray(3) { pct(hist[it], 0.90f).toFloat() }
        if (gray) {
            val l = lo.average().toFloat()
            val hh = hi.average().toFloat()
            lo.fill(l); hi.fill(hh)
        }
        val s = FloatArray(3) { 255f / (hi[it] - lo[it]).coerceAtLeast(40f) }
        val levels = ColorMatrix(
            floatArrayOf(
                s[0], 0f, 0f, 0f, -lo[0] * s[0],
                0f, s[1], 0f, 0f, -lo[1] * s[1],
                0f, 0f, s[2], 0f, -lo[2] * s[2],
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        cm.postConcat(levels)
        if (!gray) cm.postConcat(ColorMatrix().apply { setSaturation(1.12f) })
        return cm
    }

    /** Adaptive (Bradley) threshold: crisp black text on white, robust to shadows. */
    private fun threshold(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val lum = IntArray(px.size) { (px[it] shr 8) and 255 }
        val integral = LongArray((w + 1) * (h + 1))
        for (y in 0 until h) {
            var row = 0L
            for (x in 0 until w) {
                row += lum[y * w + x]
                integral[(y + 1) * (w + 1) + x + 1] = integral[y * (w + 1) + x + 1] + row
            }
        }
        val r = max(8, w / 16)
        for (y in 0 until h) {
            val y0 = (y - r).coerceAtLeast(0)
            val y1 = (y + r).coerceAtMost(h - 1)
            for (x in 0 until w) {
                val x0 = (x - r).coerceAtLeast(0)
                val x1 = (x + r).coerceAtMost(w - 1)
                val count = (x1 - x0 + 1) * (y1 - y0 + 1)
                val sum = integral[(y1 + 1) * (w + 1) + x1 + 1] - integral[y0 * (w + 1) + x1 + 1] -
                    integral[(y1 + 1) * (w + 1) + x0] + integral[y0 * (w + 1) + x0]
                val v = if (lum[y * w + x] * count < sum * 0.85) 0x10 else 0xFF
                px[y * w + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }
}
