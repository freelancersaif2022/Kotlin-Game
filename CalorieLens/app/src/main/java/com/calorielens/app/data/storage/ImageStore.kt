package com.calorielens.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** Stores meal photos privately inside the app's own storage. */
@Singleton
class ImageStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val mealsDir: File get() = File(context.filesDir, "meals").apply { mkdirs() }

    fun newCaptureFile(): File = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

    /** Normalises orientation and size of a captured photo and moves it to permanent storage. */
    suspend fun importCapture(file: File): File = withContext(Dispatchers.IO) {
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val bitmap = decodeSampled({ BitmapFactory.decodeFile(file.absolutePath, it) }, MAX_SIDE)
            ?: error("Could not read the photo")
        val out = save(rotate(bitmap, orientation))
        file.delete()
        out
    }

    /** Copies a gallery image into permanent storage. */
    suspend fun importUri(uri: Uri): File = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val bitmap = decodeSampled({ opts -> resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } }, MAX_SIDE)
            ?: error("Could not read the selected image")
        save(rotate(bitmap, orientation))
    }

    suspend fun loadForAnalysis(path: String): Bitmap = withContext(Dispatchers.IO) {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        BitmapFactory.decodeFile(path, opts) ?: error("Photo not found")
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) { runCatching { File(path).delete() } }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { runCatching { mealsDir.deleteRecursively() } }

    private fun save(bitmap: Bitmap): File {
        val out = File(mealsDir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        return out
    }

    private fun decodeSampled(decode: (BitmapFactory.Options) -> Bitmap?, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        var sample = 1
        val largest = max(bounds.outWidth, bounds.outHeight)
        while (largest / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = decode(opts) ?: return null
        val side = max(decoded.width, decoded.height)
        if (side <= maxSide) return decoded
        val scale = maxSide.toFloat() / side
        val scaled = Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun rotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private companion object {
        const val MAX_SIDE = 1280
    }
}
