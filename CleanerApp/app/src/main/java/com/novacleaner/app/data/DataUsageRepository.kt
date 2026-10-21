package com.novacleaner.app.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class UsageTotals(val wifi: Long, val mobile: Long) {
    val total: Long get() = wifi + mobile
}

data class AppDataUsage(val uid: Int, val packageName: String?, val label: String, val wifi: Long, val mobile: Long) {
    val total: Long get() = wifi + mobile
}

data class DailyUsage(val dayStart: Long, val wifi: Long, val mobile: Long)

enum class UsagePeriod(val label: String) { TODAY("Today"), WEEK("7 days"), MONTH("This month"), DAYS30("30 days") }

@Suppress("DEPRECATION")
object DataUsageRepository {

    private const val WIFI = ConnectivityManager.TYPE_WIFI
    private const val MOBILE = ConnectivityManager.TYPE_MOBILE

    fun range(period: UsagePeriod, now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = when (period) {
            UsagePeriod.TODAY -> cal.timeInMillis
            UsagePeriod.WEEK -> cal.apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
            UsagePeriod.MONTH -> cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            UsagePeriod.DAYS30 -> cal.apply { add(Calendar.DAY_OF_YEAR, -29) }.timeInMillis
        }
        return start to now
    }

    private fun manager(context: Context) = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    private fun device(nsm: NetworkStatsManager, type: Int, start: Long, end: Long): Long = runCatching {
        val b = nsm.querySummaryForDevice(type, null, start, end)
        b.rxBytes + b.txBytes
    }.getOrDefault(0L)

    suspend fun totals(context: Context, start: Long, end: Long): UsageTotals = withContext(Dispatchers.IO) {
        val nsm = manager(context)
        UsageTotals(wifi = device(nsm, WIFI, start, end), mobile = device(nsm, MOBILE, start, end))
    }

    suspend fun daily(context: Context, days: Int = 7): List<DailyUsage> = withContext(Dispatchers.IO) {
        val nsm = manager(context)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        (0 until days).map {
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val end = minOf(cal.timeInMillis, System.currentTimeMillis())
            DailyUsage(start, device(nsm, WIFI, start, end), device(nsm, MOBILE, start, end))
        }
    }

    suspend fun perApp(context: Context, start: Long, end: Long): List<AppDataUsage> = withContext(Dispatchers.IO) {
        val nsm = manager(context)
        val wifi = HashMap<Int, Long>()
        val mobile = HashMap<Int, Long>()
        fun collect(type: Int, into: HashMap<Int, Long>) = runCatching {
            val stats = nsm.querySummary(type, null, start, end)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                into[bucket.uid] = (into[bucket.uid] ?: 0L) + bucket.rxBytes + bucket.txBytes
            }
            stats.close()
        }
        collect(WIFI, wifi)
        collect(MOBILE, mobile)
        val pm = context.packageManager
        (wifi.keys + mobile.keys).map { uid ->
            val packages = pm.getPackagesForUid(uid)
            val pkg = packages?.firstOrNull()
            val label = when {
                uid == NetworkStats.Bucket.UID_REMOVED -> "Removed apps"
                uid == NetworkStats.Bucket.UID_TETHERING -> "Hotspot & tethering"
                uid == 1000 || (packages != null && packages.size > 1) -> "Android system"
                pkg != null -> runCatching { pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString() }.getOrDefault(pkg)
                else -> "System services"
            }
            AppDataUsage(uid, if (packages != null && packages.size == 1) pkg else null, label, wifi[uid] ?: 0L, mobile[uid] ?: 0L)
        }.filter { it.total > 0 }.sortedByDescending { it.total }
    }
}
