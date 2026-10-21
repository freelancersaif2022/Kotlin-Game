package com.novacleaner.app.data

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StorageInfo(val total: Long, val free: Long) {
    val used: Long get() = (total - free).coerceAtLeast(0)
    val usedFraction: Float get() = if (total == 0L) 0f else used.toFloat() / total
}

data class RamInfo(val total: Long, val available: Long) {
    val used: Long get() = (total - available).coerceAtLeast(0)
    val usedFraction: Float get() = if (total == 0L) 0f else used.toFloat() / total
}

enum class StorageCategory(val label: String) {
    IMAGES("Images"), VIDEOS("Videos"), AUDIO("Audio"), DOCUMENTS("Documents"), APKS("APKs"), OTHER("Apps & system")
}

object StorageRepository {

    suspend fun storage(context: Context): StorageInfo = withContext(Dispatchers.IO) {
        runCatching {
            val stats = context.getSystemService(StorageStatsManager::class.java)
            StorageInfo(stats.getTotalBytes(StorageManager.UUID_DEFAULT), stats.getFreeBytes(StorageManager.UUID_DEFAULT))
        }.getOrElse {
            val stat = StatFs(Environment.getDataDirectory().path)
            StorageInfo(stat.totalBytes, stat.availableBytes)
        }
    }

    fun ram(context: Context): RamInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return RamInfo(info.totalMem, info.availMem)
    }

    /** Size per media category, from the MediaStore index. Needs storage permission. */
    suspend fun breakdown(context: Context, storage: StorageInfo): Map<StorageCategory, Long> = withContext(Dispatchers.IO) {
        val sums = StorageCategory.entries.associateWith { 0L }.toMutableMap()
        runCatching {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, "${MediaStore.Files.FileColumns.SIZE} > 0", null, null)?.use { c ->
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val size = c.getLong(sizeCol)
                    val mime = c.getString(mimeCol).orEmpty()
                    val name = c.getString(nameCol).orEmpty()
                    val cat = FileKind.from(mime, name).storageCategory
                    if (cat != StorageCategory.OTHER) sums[cat] = (sums[cat] ?: 0L) + size
                }
            }
        }
        val known = sums.values.sum()
        sums[StorageCategory.OTHER] = (storage.used - known).coerceAtLeast(0)
        sums
    }
}
