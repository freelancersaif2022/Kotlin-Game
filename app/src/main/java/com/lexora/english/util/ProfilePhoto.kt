package com.lexora.english.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Crops a picked or captured photo to a square and stores it privately on the phone. */
object ProfilePhoto {

    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture.jpg")
        if (file.exists()) file.delete()
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    private fun decode(context: Context, uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val max = maxOf(info.size.width, info.size.height)
                if (max > 1600) {
                    val scale = 1600f / max
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    } catch (e: Exception) {
        null
    }

    /** Returns the absolute path of the saved 512×512 JPEG, or null if the image couldn't be read. */
    fun save(context: Context, uri: Uri, previousPath: String?): String? {
        val src = decode(context, uri) ?: return null
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 512, 512, true)
        val dir = File(context.filesDir, "profile").apply { mkdirs() }
        val file = File(dir, "photo_" + System.currentTimeMillis() + ".jpg")
        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        previousPath?.let { old -> if (old != file.absolutePath) File(old).delete() }
        return file.absolutePath
    }
}
