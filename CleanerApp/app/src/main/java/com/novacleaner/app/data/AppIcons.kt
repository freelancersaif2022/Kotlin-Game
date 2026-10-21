package com.novacleaner.app.data

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Memory cache of app / APK icons rendered as small bitmaps for smooth list scrolling. */
object AppIcons {
    private const val SIZE = 112
    private val cache = LruCache<String, ImageBitmap>(300)

    fun cached(key: String): ImageBitmap? = cache.get(key)

    suspend fun forPackage(context: Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(SIZE, SIZE).asImageBitmap()
            }.getOrNull()?.also { cache.put(packageName, it) }
        }
    }

    suspend fun forApk(context: Context, path: String): ImageBitmap? {
        val key = "apk:$path"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val info = pm.getPackageArchiveInfo(path, 0)?.applicationInfo ?: return@runCatching null
                info.sourceDir = path
                info.publicSourceDir = path
                info.loadIcon(pm).toBitmap(SIZE, SIZE).asImageBitmap()
            }.getOrNull()?.also { cache.put(key, it) }
        }
    }
}
