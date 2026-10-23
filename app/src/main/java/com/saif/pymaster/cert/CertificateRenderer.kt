package com.saif.pymaster.cert

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
import com.saif.pymaster.R
import java.io.File
import java.io.FileOutputStream

data class CertificateData(
    val name: String,
    val date: String,
    val id: String,
    val modules: Int,
    val lessons: Int,
    val projects: Int,
)

/**
 * Draws the certificate. Every position is a fraction of the image height so the
 * on-screen preview and the high-resolution export look identical.
 */
object CertificateRenderer {

    fun render(context: Context, data: CertificateData, highRes: Boolean): Bitmap {
        val res = context.resources
        val opts = BitmapFactory.Options().apply {
            inScaled = false
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bg = try {
            BitmapFactory.decodeResource(res, if (highRes) R.drawable.certificate_bg_hd else R.drawable.certificate_bg, opts)
        } catch (_: OutOfMemoryError) {
            BitmapFactory.decodeResource(res, R.drawable.certificate_bg, opts)
        }
        val bitmap = if (bg.isMutable) bg else bg.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        fun font(id: Int): Typeface = ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT
        val cinzelBold = font(R.font.cinzel_bold)
        val cinzel = font(R.font.cinzel_regular)
        val poppins = font(R.font.poppins_regular)
        val poppinsMedium = font(R.font.poppins_medium)
        val poppinsSemi = font(R.font.poppins_semibold)
        val playfair = font(R.font.playfair_bold)
        val script = font(R.font.great_vibes)

        val gold = 0xFFFFD43B.toInt()
        val white = 0xFFFFFFFF.toInt()

        fun paint(tf: Typeface, sizeFactor: Float, color: Int, alpha: Int = 255, spacingEm: Float = 0f) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = tf
                textSize = h * sizeFactor
                this.color = color
                this.alpha = alpha
                textAlign = Paint.Align.CENTER
                letterSpacing = spacingEm
            }

        fun line(x1: Float, x2: Float, y: Float, color: Int, stroke: Float) {
            canvas.drawLine(x1, y, x2, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; strokeWidth = stroke })
        }

        val cx = w / 2f

        // Logo
        ResourcesCompat.getDrawable(res, R.drawable.logo, null)?.let { d ->
            val size = h * 0.105f
            val top = h * 0.085f
            d.setBounds((cx - size / 2).toInt(), top.toInt(), (cx + size / 2).toInt(), (top + size).toInt())
            d.draw(canvas)
        }

        // Titles
        canvas.drawText("CERTIFICATE", cx, h * 0.295f, paint(cinzelBold, 0.082f, gold, spacingEm = 0.146f))
        canvas.drawText("OF COMPLETION", cx, h * 0.352f, paint(cinzel, 0.034f, white, spacingEm = 0.35f))
        canvas.drawText("This certificate is proudly presented to", cx, h * 0.43f, paint(poppins, 0.026f, white, 190))

        // Recipient name (auto-fit to 62% of the width)
        val namePaint = paint(playfair, 0.09f, white)
        val maxNameWidth = w * 0.62f
        while (namePaint.measureText(data.name) > maxNameWidth && namePaint.textSize > h * 0.03f) {
            namePaint.textSize *= 0.95f
        }
        canvas.drawText(data.name, cx, h * 0.535f, namePaint)
        line(w * 0.27f, w * 0.73f, h * 0.565f, gold, h * 0.0024f)

        // Description
        val body = paint(poppins, 0.024f, white, 215)
        canvas.drawText("for successfully completing the PyMaster Python Developer Program —", cx, h * 0.625f, body)
        canvas.drawText(
            "${data.modules} modules, ${data.lessons} lessons and ${data.projects} real-world projects in Python.",
            cx, h * 0.662f, body,
        )

        // Seal
        ResourcesCompat.getDrawable(res, R.drawable.certificate_seal, null)?.let { d ->
            val size = h * 0.175f
            val top = h * 0.705f
            d.setBounds((cx - size / 2).toInt(), top.toInt(), (cx + size / 2).toInt(), (top + size).toInt())
            d.draw(canvas)
        }

        // Date block (left) and signature block (right)
        val leftX = w * 0.255f
        val rightX = w * 0.745f
        val half = w * 0.1f
        canvas.drawText(data.date, leftX, h * 0.80f, paint(poppinsSemi, 0.028f, white))
        line(leftX - half, leftX + half, h * 0.818f, gold, h * 0.0016f)
        val label = paint(poppinsMedium, 0.019f, white, 165, spacingEm = 0.06f)
        canvas.drawText("DATE OF ISSUE", leftX, h * 0.852f, label)

        canvas.drawText("Saif A.", rightX, h * 0.805f, paint(script, 0.085f, white))
        line(rightX - half, rightX + half, h * 0.818f, gold, h * 0.0016f)
        canvas.drawText("SAIF A.  •  LEAD INSTRUCTOR", rightX, h * 0.852f, label)

        // Certificate ID
        canvas.drawText(
            "Certificate ID: ${data.id}   •   Issued by PyMaster",
            cx, h * 0.91f, paint(poppins, 0.017f, white, 140),
        )
        return bitmap
    }

    private fun exportDir(context: Context) = File(context.cacheDir, "certificates").apply { mkdirs() }

    private fun fileName(data: CertificateData) =
        "PyMaster_Certificate_" + data.name.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')

    fun writePng(context: Context, bitmap: Bitmap, data: CertificateData): File {
        val file = File(exportDir(context), fileName(data) + ".png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun writePdf(context: Context, bitmap: Bitmap, data: CertificateData): File {
        // A4 landscape in PostScript points
        val pageW = 842
        val pageH = 595
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageW.toFloat(), pageH.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        doc.finishPage(page)
        val file = File(exportDir(context), fileName(data) + ".pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, file: File, mime: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "I just earned my PyMaster Python Developer certificate! 🐍🎓")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share certificate").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves to Pictures/PyMaster. Returns false on Android 9 and older (use Share there). */
    fun saveToGallery(context: Context, bitmap: Bitmap, data: CertificateData): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName(data) + "_" + System.currentTimeMillis() + ".png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PyMaster")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }
}
