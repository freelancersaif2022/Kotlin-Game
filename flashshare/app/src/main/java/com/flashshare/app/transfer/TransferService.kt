package com.flashshare.app.transfer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.flashshare.app.FlashShareApp
import com.flashshare.app.MainActivity
import com.flashshare.app.R
import com.flashshare.app.container
import com.flashshare.app.net.BatchStatus
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.formatSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

/**
 * Keeps the connection alive in the background: foreground notification with live progress,
 * a high-performance Wi-Fi lock (stops the radio from power-saving mid-transfer) and a CPU wake lock.
 */
class TransferService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wifiLock: WifiManager.WifiLock? = null
    private var lowLatencyLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var observer: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            container.transfers.disconnect()
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        runCatching { ServiceCompat.startForeground(this, NOTIF_ID, build("Ready to share", null, 0), type) }
            .onFailure { stopSelf(); return START_NOT_STICKY }
        acquireLocks()
        if (observer == null) observe()
        return START_NOT_STICKY
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observe() {
        val tm = container.transfers
        observer = scope.launch {
            combine(tm.phase, tm.batches, tm.peers) { phase, batches, peers -> Triple(phase, batches, peers) }
                .sample(1000)
                .collect { (phase, batches, peers) ->
                    if (phase is Phase.Idle) { stopSelf(); return@collect }
                    val active = batches.filter { it.status == BatchStatus.RUNNING || it.status == BatchStatus.WAITING }
                    val n = when {
                        active.isNotEmpty() -> {
                            val total = active.sumOf { it.total }
                            val done = active.sumOf { it.transferred }
                            val pct = if (total > 0) (done * 100 / total).toInt() else 0
                            val speed = active.sumOf { it.speed }
                            val dir = if (active.first().outgoing) "Sending to" else "Receiving from"
                            build("$dir ${active.first().peer.name}", "$pct% • ${formatSize(done)} of ${formatSize(total)} • ${formatSpeed(speed)}", pct)
                        }
                        phase is Phase.Hosting && peers.isEmpty() -> build("Waiting for a phone to scan your code", null, -1)
                        phase is Phase.Joining -> build("Connecting to ${phase.peerName}…", null, -1)
                        peers.isNotEmpty() -> build("Connected to ${peers.joinToString { it.name }}", "Tap to open", 0)
                        else -> build("FlashShare is ready", null, 0)
                    }
                    notifySafe(n)
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifySafe(n: Notification) {
        runCatching { NotificationManagerCompat.from(this).notify(NOTIF_ID, n) }
    }

    private fun build(title: String, text: String?, progress: Int): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, TransferService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, FlashShareApp.CHANNEL_TRANSFER)
            .setSmallIcon(R.drawable.ic_stat_flash)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Disconnect", stop)
            .apply {
                when {
                    progress < 0 -> setProgress(0, 0, true)
                    progress > 0 -> setProgress(100, progress, false)
                }
            }
            .build()
    }

    private fun acquireLocks() {
        val wm = applicationContext.getSystemService(WifiManager::class.java)
        if (wifiLock == null) {
            @Suppress("DEPRECATION")
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "FlashShare:transfer").apply {
                setReferenceCounted(false); acquire()
            }
        }
        if (lowLatencyLock == null && Build.VERSION.SDK_INT >= 29) {
            lowLatencyLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "FlashShare:latency").apply {
                setReferenceCounted(false); acquire()
            }
        }
        if (wakeLock == null) {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FlashShare:transfer").apply {
                setReferenceCounted(false); acquire(6 * 60 * 60 * 1000L)
            }
        }
    }

    override fun onDestroy() {
        runCatching { wifiLock?.release() }
        runCatching { lowLatencyLock?.release() }
        runCatching { wakeLock?.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 42
        private const val ACTION_STOP = "com.flashshare.app.STOP"

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, TransferService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TransferService::class.java))
        }
    }
}
