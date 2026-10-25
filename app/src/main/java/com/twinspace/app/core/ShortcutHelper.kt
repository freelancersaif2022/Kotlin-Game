package com.twinspace.app.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.UserManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.twinspace.app.ShortcutLauncherActivity

/** Home-screen shortcuts for clones, with the user's custom name and a "2" badge. */
object ShortcutHelper {
    const val EXTRA_PACKAGE = "package"
    const val EXTRA_USER_SERIAL = "user_serial"

    private fun id(packageName: String) = "clone:$packageName"

    fun isSupported(context: Context) = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    private fun build(context: Context, entry: AppEntry, label: String, icons: IconCache): ShortcutInfoCompat {
        val serial = context.getSystemService(UserManager::class.java)!!.getSerialNumberForUser(entry.user)
        val intent = Intent(context, ShortcutLauncherActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(EXTRA_PACKAGE, entry.packageName)
            .putExtra(EXTRA_USER_SERIAL, serial)
        return ShortcutInfoCompat.Builder(context, id(entry.packageName))
            .setShortLabel(label.take(24))
            .setLongLabel(label)
            .setIcon(IconCompat.createWithAdaptiveBitmap(badgedIcon(icons, entry)))
            .setIntent(intent)
            .build()
    }

    /** Pins a new shortcut. Returns false when the launcher doesn't support pinning. */
    fun requestPin(context: Context, entry: AppEntry, label: String, icons: IconCache): Boolean {
        if (!isSupported(context)) return false
        val info = build(context, entry, label, icons)
        // If it's already pinned, refresh it so a rename shows up immediately.
        runCatching { ShortcutManagerCompat.updateShortcuts(context, listOf(info)) }
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    /** Keeps an already pinned shortcut in sync after a rename. Never removes anything. */
    fun refreshIfPinned(context: Context, entry: AppEntry, label: String, icons: IconCache) {
        val pinned = runCatching {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        }.getOrDefault(emptyList())
        if (pinned.none { it.id == id(entry.packageName) }) return
        runCatching { ShortcutManagerCompat.updateShortcuts(context, listOf(build(context, entry, label, icons))) }
    }

    /**
     * Draws the app icon on an adaptive-icon canvas (108dp grid with the 72dp safe zone)
     * and adds a small gradient "2" badge.
     */
    private fun badgedIcon(icons: IconCache, entry: AppEntry): Bitmap {
        val size = 432
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val safe = (size * 66f / 108f).toInt()
        val offset = (size - safe) / 2f
        icons.drawable(entry.packageName, entry.user)?.toBitmap(safe, safe, Bitmap.Config.ARGB_8888)?.let {
            canvas.drawBitmap(it, offset, offset, null)
        }

        val r = size * 0.095f
        val cx = size * 0.66f
        val cy = size * 0.66f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, r + size * 0.014f, ring)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xFF5B6CFF.toInt(), 0xFF8E5CFF.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r, fill)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = r * 1.25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("2", cx, cy - (text.descent() + text.ascent()) / 2f, text)
        return out
    }
}
