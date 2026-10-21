package com.netscope.wifianalyzer.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.net.NetworkScanner
import com.netscope.wifianalyzer.net.WifiUtils
import kotlinx.coroutines.flow.last
import java.util.concurrent.TimeUnit

/** Scans the Wi-Fi network and notifies about devices never seen before. */
class IntruderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppGraph.prefs
        if (!prefs.intruderEnabled.value || !AppGraph.premium.isPremium) return Result.success()
        val snap = WifiUtils.snapshot(applicationContext)
        if (!snap.wifiConnected) return Result.success()
        val key = snap.networkKey ?: return Result.success()

        val result = runCatching { NetworkScanner(applicationContext).scan(withMdns = true).last() }.getOrNull()
            ?: return Result.retry()
        if (result.error != null) return Result.success()

        val repo = AppGraph.knownDevices
        val fresh = repo.merge(key, result.devices.map { Triple(it.key, it.displayName, it.ip) })
        prefs.lastIntruderCheck = System.currentTimeMillis()
        if (fresh.isNotEmpty()) {
            val devices = result.devices.filter { it.key in fresh }.map { it.key to "${it.displayName} • ${it.ip}" }
            Notifications.showNewDevices(applicationContext, key, snap.ssid ?: "your Wi-Fi", devices)
        }
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "intruder_scan"
        private const val NOW = "intruder_scan_now"

        private val wifiOnly = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()

        /** Every 15 minutes (the shortest interval Android allows for background work). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<IntruderWorker>(15, TimeUnit.MINUTES)
                .setConstraints(wifiOnly)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** An extra check right away, e.g. when turned on or when Wi-Fi reconnects. */
        fun runNow(context: Context, delaySeconds: Long = 0) {
            val request = OneTimeWorkRequestBuilder<IntruderWorker>()
                .setConstraints(wifiOnly)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(NOW)
        }

        private var callback: ConnectivityManager.NetworkCallback? = null

        /** While the app process is alive, check shortly after every Wi-Fi connection. */
        fun watchWifi(context: Context) {
            if (callback != null) return
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (AppGraph.prefs.intruderEnabled.value && AppGraph.premium.isPremium) runNow(context, delaySeconds = 20)
                }
            }
            val req = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            runCatching { cm.registerNetworkCallback(req, cb) }.onSuccess { callback = cb }
        }
    }
}
