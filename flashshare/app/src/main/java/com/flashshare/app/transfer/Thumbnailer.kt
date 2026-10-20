package com.flashshare.app.transfer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import com.flashshare.app.data.Category
import com.flashshare.app.data.ShareItem
import java.io.ByteArrayOutputStream
import java.io.File

/** Small previews sent along with a transfer so both phones (and History) show the real image of every file. */
object Thumbnailer {
    private const val SIZE = 160

    fun make(ctx: Context, item: ShareItem): ByteArray? = runCatching {
        when (item.category) {
            Category.PHOTO, Category.VIDEO, Category.MUSIC -> media(ctx, item.uri, item.category)?.let { jpeg(it) }
            Category.APP -> appIcon(ctx, item.packageName, item.uri)?.let { png(it) }
            else -> null
        }
    }.getOrNull()

    private fun media(ctx: Context, uri: Uri, cat: Category): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29 && uri.scheme == "content") {
            runCatching { return square(ctx.contentResolver.loadThumbnail(uri, Size(SIZE * 2, SIZE * 2), null)) }
        }
        return when (cat) {
            Category.PHOTO -> decodeSampled(ctx, uri)
            Category.VIDEO -> retriever(ctx, uri) { it.getFrameAtTime(1_000_000) }
            Category.MUSIC -> retriever(ctx, uri) { r -> r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }
            else -> null
        }?.let { square(it) }
    }

    private fun retriever(ctx: Context, uri: Uri, block: (MediaMetadataRetriever) -> Bitmap?): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file") r.setDataSource(uri.path) else r.setDataSource(ctx, uri)
            block(r)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    private fun decodeSampled(ctx: Context, uri: Uri): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        var sample = 1
        while (minOf(o.outWidth, o.outHeight) / (sample * 2) >= SIZE) sample *= 2
        return ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    /** Icon of an installed app, or of an APK file read straight from the archive. */
    fun appIcon(ctx: Context, packageName: String?, uri: Uri?): Bitmap? {
        val pm = ctx.packageManager
        val d: Drawable? = packageName?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }
            ?: apkPath(uri)?.let { path ->
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(path, 0)?.applicationInfo?.let { ai ->
                    ai.sourceDir = path; ai.publicSourceDir = path
                    runCatching { ai.loadIcon(pm) }.getOrNull()
                }
            }
        return d?.let { drawableToBitmap(it, 144) }
    }

    private fun apkPath(uri: Uri?): String? = when {
        uri == null -> null
        uri.scheme == "file" -> uri.path
        else -> null
    }

    fun drawableToBitmap(d: Drawable, px: Int): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        d.setBounds(0, 0, px, px)
        d.draw(c)
        return b
    }

    private fun square(src: Bitmap): Bitmap {
        val side = minOf(src.width, src.height)
        val crop = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        return if (side > SIZE) Bitmap.createScaledBitmap(crop, SIZE, SIZE, true) else crop
    }

    private fun jpeg(b: Bitmap): ByteArray = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, 74, it) }.toByteArray()
    private fun png(b: Bitmap): ByteArray = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    /** Saves a preview next to the history so it survives app restarts. */
    fun save(dir: File, name: String, bytes: ByteArray): String? = runCatching {
        dir.mkdirs()
        File(dir, name).also { it.writeBytes(bytes) }.absolutePath
    }.getOrNull()
}
