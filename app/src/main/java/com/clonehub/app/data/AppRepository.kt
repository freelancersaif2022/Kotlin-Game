package com.clonehub.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Reads installed launchable apps and launches them. */
class AppRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    fun installedApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                InstalledApp(pkg, ri.loadLabel(pm).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun icon(packageName: String): ImageBitmap? = runCatching {
        pm.getApplicationIcon(packageName).toImageBitmap()
    }.getOrNull()

    fun launch(packageName: String): Boolean {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    fun isInstalled(packageName: String): Boolean =
        pm.getLaunchIntentForPackage(packageName) != null

    fun canPinShortcuts(): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /**
     * Asks the launcher to pin a home-screen shortcut for this clone. The shortcut
     * carries the clone's custom name and a generated icon (the app's icon on the
     * clone's accent), and launches the target app. The launcher shows the user a
     * confirmation on Android 8+. Returns false if no app or no launcher support.
     */
    fun pinShortcut(clone: Clone): Boolean {
        if (!canPinShortcuts()) return false
        val launch = pm.getLaunchIntentForPackage(clone.packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val label = clone.label.ifBlank { "Clone" }
        val info = ShortcutInfoCompat.Builder(context, "clone_${clone.id}")
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(buildShortcutIcon(clone))
            .setIntent(launch)
            .build()
        return runCatching {
            ShortcutManagerCompat.requestPinShortcut(context, info, null)
        }.getOrDefault(false)
    }

    private fun buildShortcutIcon(clone: Clone): IconCompat {
        val size = 216
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val accent = clone.accent.toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                accent, lighten(accent), Shader.TileMode.CLAMP,
            )
        }
        // Full-bleed background so the launcher can mask it to any shape.
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        // Draw the real app icon inside the safe zone.
        runCatching { pm.getApplicationIcon(clone.packageName) }.getOrNull()?.let { d ->
            val inset = (size * 0.28f).toInt()
            d.setBounds(inset, inset, size - inset, size - inset)
            d.draw(canvas)
        }
        return IconCompat.createWithAdaptiveBitmap(bmp)
    }

    private fun lighten(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF)
        val g = ((color ushr 8) and 0xFF)
        val b = (color and 0xFF)
        fun up(c: Int) = (c + (255 - c) * 0.35f).toInt().coerceIn(0, 255)
        return (a shl 24) or (up(r) shl 16) or (up(g) shl 8) or up(b)
    }

    private fun Drawable.toImageBitmap(): ImageBitmap {
        if (this is BitmapDrawable && bitmap != null) return bitmap.asImageBitmap()
        val w = intrinsicWidth.coerceIn(1, 192)
        val h = intrinsicHeight.coerceIn(1, 192)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bmp.asImageBitmap()
    }
}
