package com.novacleaner.app.data

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import com.novacleaner.app.core.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class AppInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val isSystem: Boolean,
    val installTime: Long,
    val updateTime: Long,
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
    val lastUsed: Long,
    val sizesExact: Boolean,
) {
    val totalBytes: Long get() = appBytes + dataBytes
}

object AppsRepository {

    @Suppress("DEPRECATION")
    private fun installedPackages(pm: PackageManager): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getInstalledPackages(0)
        }

    suspend fun load(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val hasUsage = Permissions.hasUsageAccess(context)
        val storageStats = context.getSystemService(StorageStatsManager::class.java)
        val user = Process.myUserHandle()
        val lastUsedMap: Map<String, Long> = if (hasUsage) {
            runCatching {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val now = System.currentTimeMillis()
                usm.queryAndAggregateUsageStats(now - TimeUnit.DAYS.toMillis(730), now)
                    .mapValues { it.value.lastTimeUsed }
            }.getOrDefault(emptyMap())
        } else emptyMap()

        installedPackages(pm).mapNotNull { pkg ->
            val ai: ApplicationInfo = pkg.applicationInfo ?: return@mapNotNull null
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            val stats = if (hasUsage) runCatching {
                storageStats.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg.packageName, user)
            }.getOrNull() else null
            val apkSize = stats?.appBytes ?: (listOfNotNull(ai.sourceDir) + ai.splitSourceDirs.orEmpty()).sumOf { File(it).length() }
            AppInfo(
                packageName = pkg.packageName,
                label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(pkg.packageName),
                versionName = pkg.versionName ?: "",
                isSystem = isSystem,
                installTime = pkg.firstInstallTime,
                updateTime = pkg.lastUpdateTime,
                appBytes = apkSize,
                dataBytes = stats?.dataBytes ?: 0L,
                cacheBytes = stats?.cacheBytes ?: 0L,
                lastUsed = lastUsedMap[pkg.packageName] ?: 0L,
                sizesExact = stats != null,
            )
        }
    }

    /** Total cache held by all installed apps (system-managed, cleared from each app's settings). */
    suspend fun totalCache(context: Context): Long = withContext(Dispatchers.IO) {
        if (!Permissions.hasUsageAccess(context)) return@withContext 0L
        val storageStats = context.getSystemService(StorageStatsManager::class.java)
        val user = Process.myUserHandle()
        installedPackages(context.packageManager).sumOf { pkg ->
            runCatching { storageStats.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg.packageName, user).cacheBytes }.getOrDefault(0L)
        }
    }

    fun isInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
}
