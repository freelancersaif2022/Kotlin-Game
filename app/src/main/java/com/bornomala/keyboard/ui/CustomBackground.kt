package com.bornomala.keyboard.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import com.bornomala.keyboard.prefs.Keys
import com.bornomala.keyboard.prefs.Prefs
import com.bornomala.keyboard.theme.KeyboardThemes
import java.io.File

/** Copies a photo chosen by the user into private storage for the custom keyboard theme. */
object CustomBackground {

    private const val TAG = "CustomBackground"
    private const val MAX_EDGE = 1600

    /** Imports [uri]; returns true on success. Call from a background thread. */
    fun import(context: Context, uri: Uri): Boolean = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "not an image" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        var bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("decode failed")

        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } else 0f
        val scale = minOf(1f, MAX_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height))
        if (rotation != 0f || scale < 1f) {
            val m = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation)
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            if (rotated !== bitmap) bitmap.recycle()
            bitmap = rotated
        }

        val target = KeyboardThemes.customFile(context)
        val tmp = File(target.parentFile, target.name + ".tmp")
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        tmp.outputStream().use { bitmap.compress(format, 88, it) }
        bitmap.recycle()
        require(tmp.renameTo(target)) { "could not save" }

        Prefs.get(context).edit()
            .putLong(Keys.CUSTOM_BG_VERSION, System.currentTimeMillis())
            .putString(Keys.THEME, "custom")
            .apply()
        true
    } catch (e: Throwable) {
        Log.w(TAG, "Could not import background", e)
        false
    }

    fun exists(context: Context) = KeyboardThemes.customFile(context).exists()

    /** A small preview bitmap for the settings screen. */
    fun preview(context: Context, maxEdge: Int = 720): Bitmap? {
        val f = KeyboardThemes.customFile(context)
        if (!f.exists()) return null
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, o)
            var s = 1
            while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= maxEdge) s *= 2
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = s })
        } catch (e: Throwable) {
            null
        }
    }
}
