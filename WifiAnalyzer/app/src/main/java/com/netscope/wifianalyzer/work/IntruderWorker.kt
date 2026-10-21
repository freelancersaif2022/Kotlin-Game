package com.netscope.wifianalyzer.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.net.NetworkScanner
import com.netscope.wifianalyzer.net.WifiUtils
import kotlinx.coroutines.flow.last
import java.util.concurrent.TimeUnit

/** Periodically scans the Wi-Fi network and notifies about devices never seen before. */
class IntruderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppGraph.prefs
        if (!prefs.intruderEnabled.value || !AppGraph.premium.isPremium) return Result.success()
        val snap = WifiUtils.snapshot(applicationContext)
        val key = snap.networkKey ?: return Result.success()
        if (!snap.wifiConnected) return Result.success()

        val result = runCatching { NetworkScanner(applicationContext).scan(withMdns = true).last() }.getOrNull()
            ?: return Result.retry()
        if (result.error != null) return Result.success()

        val repo = AppGraph.knownDevices
        val fresh = repo.merge(key, result.devices.map { Triple(it.key, it.displayName, it.ip) })
        prefs.lastIntruderCheck = System.currentTimeMillis()
        if (fresh.isNotEmpty()) {
            val names = result.devices.filter { it.key in fresh }.map { "${it.displayName} (${it.ip})" }
            Notifications.showNewDevices(applicationContext, names)
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "intruder_scan"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<IntruderWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
