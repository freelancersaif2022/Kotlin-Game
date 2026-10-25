package com.twinspace.app.core

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes app icons off the main thread and keeps them in memory so scrolling stays smooth. */
class IconCache(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)!!
    private val density = context.resources.displayMetrics.densityDpi
    private val sizePx = (context.resources.displayMetrics.density * ICON_DP).toInt().coerceAtLeast(48)

    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun peek(entry: AppEntry): ImageBitmap? = cache.get(entry.key)

    suspend fun load(entry: AppEntry): ImageBitmap? {
        cache.get(entry.key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bitmap = drawable(entry.packageName, entry.user)?.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                ?: return@withContext null
            bitmap.asImageBitmap().also { cache.put(entry.key, it) }
        }
    }

    /** Raw, unbadged icon. Falls back to the personal copy (used for frozen clones). */
    fun drawable(packageName: String, user: UserHandle): Drawable? =
        runCatching { launcherApps.getActivityList(packageName, user).firstOrNull()?.getIcon(density) }.getOrNull()
            ?: runCatching {
                if (user == Process.myUserHandle()) null else context.packageManager.getApplicationIcon(packageName)
            }.getOrNull()
            ?: runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()

    fun invalidate(packageName: String) {
        cache.snapshot().keys.filter { it.endsWith("/$packageName") }.forEach(cache::remove)
    }

    private companion object {
        const val ICON_DP = 56
        const val MAX_BYTES = 24 * 1024 * 1024
    }
}
