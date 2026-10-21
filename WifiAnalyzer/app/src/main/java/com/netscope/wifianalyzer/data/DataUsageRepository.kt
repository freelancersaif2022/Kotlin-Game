package com.netscope.wifianalyzer.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import java.util.Calendar

enum class UsagePeriod(val label: String) { TODAY("Today"), WEEK("7 days"), MONTH("This month") }

data class AppUsage(val uid: Int, val name: String, val packageName: String?, val icon: Bitmap?, val rxBytes: Long, val txBytes: Long) {
    val total: Long get() = rxBytes + txBytes
}

data class DailyUsage(val dayLabel: String, val bytes: Long)

data class UsageReport(
    val rxBytes: Long,
    val txBytes: Long,
    val apps: List<AppUsage>,
    val daily: List<DailyUsage>,
) {
    val total: Long get() = rxBytes + txBytes
}

class DataUsageRepository(private val context: Context) {

    fun hasAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun periodStart(period: UsagePeriod): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        when (period) {
            UsagePeriod.TODAY -> Unit
            UsagePeriod.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -6)
            UsagePeriod.MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    @Suppress("DEPRECATION")
    fun load(wifi: Boolean, period: UsagePeriod): UsageReport {
        val nsm = context.getSystemService(NetworkStatsManager::class.java)
            ?: return UsageReport(0, 0, emptyList(), emptyList())
        val type = if (wifi) ConnectivityManager.TYPE_WIFI else ConnectivityManager.TYPE_MOBILE
        val start = periodStart(period)
        val end = System.currentTimeMillis()

        val device = runCatching { nsm.querySummaryForDevice(type, null, start, end) }.getOrNull()
        val perUid = mutableMapOf<Int, LongArray>()
        runCatching {
            val stats = nsm.querySummary(type, null, start, end)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val arr = perUid.getOrPut(bucket.uid) { LongArray(2) }
                arr[0] += bucket.rxBytes
                arr[1] += bucket.txBytes
            }
            stats.close()
        }

        val pm = context.packageManager
        val apps = perUid.filter { it.value[0] + it.value[1] > 0 }.map { (uid, arr) ->
            val (name, pkg, icon) = describeUid(pm, uid)
            AppUsage(uid, name, pkg, icon, arr[0], arr[1])
        }.sortedByDescending { it.total }.take(40)

        val daily = (6 downTo 0).map { back ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_YEAR, -back)
            val s = cal.timeInMillis
            val label = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val e = minOf(cal.timeInMillis, end)
            val b = runCatching { nsm.querySummaryForDevice(type, null, s, e) }.getOrNull()
            DailyUsage(label, (b?.rxBytes ?: 0) + (b?.txBytes ?: 0))
        }

        val rx = device?.rxBytes ?: apps.sumOf { it.rxBytes }
        val tx = device?.txBytes ?: apps.sumOf { it.txBytes }
        return UsageReport(rx, tx, apps, daily)
    }

    private fun describeUid(pm: PackageManager, uid: Int): Triple<String, String?, Bitmap?> {
        when (uid) {
            NetworkStats.Bucket.UID_REMOVED -> return Triple("Removed apps", null, null)
            NetworkStats.Bucket.UID_TETHERING -> return Triple("Hotspot & tethering", null, null)
            Process.SYSTEM_UID, 0 -> return Triple("Android system", null, null)
        }
        val pkg = pm.getPackagesForUid(uid)?.firstOrNull() ?: return Triple("System service", null, null)
        return runCatching {
            val info = pm.getApplicationInfo(pkg, 0)
            val icon = runCatching { pm.getApplicationIcon(info).toBitmap(96, 96) }.getOrNull()
            Triple(pm.getApplicationLabel(info).toString(), pkg, icon)
        }.getOrDefault(Triple(pkg, pkg, null))
    }
}
