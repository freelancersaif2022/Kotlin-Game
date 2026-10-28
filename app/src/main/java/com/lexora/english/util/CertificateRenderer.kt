package com.lexora.english.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.lexora.english.R
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Draws the certificate on top of the designed template. Positions are fractions of the template
 * size, measured against the template's empty areas, so the layout stays exact at any resolution.
 */
object CertificateRenderer {

    private val Navy = 0xFF1B2A6B.toInt()
    private val Gold = 0xFFA8841E.toInt()
    private val Body = 0xFF3A4050.toInt()
    private val Muted = 0xFF6B7080.toInt()

    fun formatDate(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    fun render(context: Context, name: String, issuedAt: Long, certificateId: String, width: Int = 2480): Bitmap {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val template = BitmapFactory.decodeResource(context.resources, R.drawable.certificate_template, opts)
        val height = (width * template.height.toFloat() / template.width).toInt()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawBitmap(template, null, RectF(0f, 0f, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        template.recycle()

        val cinzel = ResourcesCompat.getFont(context, R.font.cinzel) ?: Typeface.SERIF
        val playfair = ResourcesCompat.getFont(context, R.font.playfair_display) ?: Typeface.SERIF
        val poppins = ResourcesCompat.getFont(context, R.font.poppins_regular) ?: Typeface.SANS_SERIF
        val poppinsMedium = ResourcesCompat.getFont(context, R.font.poppins_medium) ?: Typeface.SANS_SERIF
        val poppinsSemi = ResourcesCompat.getFont(context, R.font.poppins_semibold) ?: Typeface.DEFAULT_BOLD

        fun paint(tf: Typeface, sizeFrac: Float, color: Int, spacing: Float = 0f) =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                typeface = tf
                textSize = h * sizeFrac
                this.color = color
                textAlign = Paint.Align.CENTER
                letterSpacing = spacing
            }

        val cx = w / 2f
        c.drawText("CERTIFICATE", cx, h * 0.300f, paint(cinzel, 0.068f, Navy, 0.12f))
        c.drawText("OF COMPLETION", cx, h * 0.348f, paint(cinzel, 0.028f, Gold, 0.38f))
        c.drawText("This certificate is proudly presented to", cx, h * 0.415f, paint(poppins, 0.022f, Muted, 0.02f))

        // Recipient name: shrink to fit the space between the two decorative swirls.
        val namePaint = paint(playfair, 0.084f, Navy, 0.01f)
        val maxNameWidth = w * 0.62f
        while (namePaint.measureText(name) > maxNameWidth && namePaint.textSize > h * 0.04f) {
            namePaint.textSize *= 0.95f
        }
        c.drawText(name, cx, h * 0.505f, namePaint)

        val lineWidth = maxOf(namePaint.measureText(name) + w * 0.06f, w * 0.34f).coerceAtMost(w * 0.66f)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Gold; strokeWidth = h * 0.0022f }
        c.drawLine(cx - lineWidth / 2, h * 0.528f, cx + lineWidth / 2, h * 0.528f, linePaint)
        val diamond = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Gold }
        c.save()
        c.rotate(45f, cx, h * 0.528f)
        val d = h * 0.007f
        c.drawRect(cx - d, h * 0.528f - d, cx + d, h * 0.528f + d, diamond)
        c.restore()

        val desc = paint(poppins, 0.0215f, Body, 0.01f)
        c.drawText("for successfully completing the Lexora Complete English Course (A to Z)", cx, h * 0.578f, desc)
        c.drawText("and all 15 Real-Life English Projects with distinction.", cx, h * 0.612f, desc)

        // Left: date of issue.
        val leftX = w * 0.2205f
        c.drawText(formatDate(issuedAt), leftX, h * 0.775f, paint(poppinsMedium, 0.026f, Navy))
        c.drawText("DATE OF ISSUE", leftX, h * 0.826f, paint(poppinsSemi, 0.0165f, Gold, 0.18f))
        c.drawText("Certificate ID: $certificateId", leftX, h * 0.855f, paint(poppins, 0.0145f, Muted, 0.02f))

        // Right: signature of Saif A.
        val rightX = w * 0.7785f
        val sig = BitmapFactory.decodeResource(context.resources, R.drawable.signature_saif, opts)
        val sigH = h * 0.105f
        val sigW = sigH * sig.width / sig.height
        c.drawBitmap(sig, null, RectF(rightX - sigW / 2, h * 0.792f - sigH, rightX + sigW / 2, h * 0.792f), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        sig.recycle()
        c.drawText("SAIF A.", rightX, h * 0.826f, paint(poppinsSemi, 0.0165f, Gold, 0.18f))
        c.drawText("Founder & Lead Instructor, Lexora", rightX, h * 0.855f, paint(poppins, 0.0145f, Muted, 0.02f))
        return bmp
    }

    private fun fileName(name: String) =
        "Lexora_Certificate_" + name.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "Learner" }

    /** Writes a PNG into the app cache and returns a shareable content Uri. */
    fun cachePng(context: Context, bitmap: Bitmap, name: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName(name) + ".png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    fun shareIntent(context: Context, uri: Uri): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "I just earned my Lexora English certificate! 🎓")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share certificate",
        )

    /** Saves to Pictures/Lexora. Returns true on success. Android 10+ needs no permission. */
    fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName(name) + "_" + System.currentTimeMillis() + ".png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lexora")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri != null && (context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Lexora").apply { mkdirs() }
            FileOutputStream(File(dir, fileName(name) + ".png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    } catch (e: Exception) {
        false
    }

    /** Saves an A4-landscape PDF to Downloads. Returns true on success. */
    fun savePdf(context: Context, bitmap: Bitmap, name: String): Boolean = try {
        val pdf = PdfDocument()
        val pageW = 842
        val pageH = 595
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageW.toFloat(), pageH.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        pdf.finishPage(page)
        val ok = if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName(name) + "_" + System.currentTimeMillis() + ".pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri != null && (context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it); true } ?: false)
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            FileOutputStream(File(dir, fileName(name) + ".pdf")).use { pdf.writeTo(it) }
            true
        }
        pdf.close()
        ok
    } catch (e: Exception) {
        false
    }
}
