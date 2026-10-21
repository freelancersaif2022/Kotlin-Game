package com.novacleaner.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.novacleaner.app.MainActivity
import com.novacleaner.app.R
import com.novacleaner.app.core.Format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/** Foreground service that shows the live network speed as a status-bar icon. */
class NetSpeedService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        runCatching { ServiceCompat.startForeground(this, Notifications.ID_SPEED, build(0, 0), type) }
            .onFailure { stopSelf(); return START_NOT_STICKY }
        if (job?.isActive != true) job = loop()
        return START_STICKY
    }

    private fun loop() = scope.launch {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        val nm = NotificationManagerCompat.from(this@NetSpeedService)
        while (isActive) {
            delay(1000)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val down = (rx - lastRx).coerceAtLeast(0)
            val up = (tx - lastTx).coerceAtLeast(0)
            lastRx = rx
            lastTx = tx
            runCatching { nm.notify(Notifications.ID_SPEED, build(down, up)) }
        }
    }

    private fun build(down: Long, up: Long) = NotificationCompat.Builder(this, Notifications.CHANNEL_SPEED)
        .setSmallIcon(IconCompat.createWithBitmap(speedIcon(down + up)))
        .setContentTitle("↓ ${Format.speed(down)}   ↑ ${Format.speed(up)}")
        .setContentText("Tap to open network monitor")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(false)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setColor(ContextCompat.getColor(this, R.color.brand_emerald))
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .addAction(
            0, "Stop",
            PendingIntent.getService(
                this, 1, Intent(this, NetSpeedService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .build()

    /** Draws "1.2 / MB" style text into a bitmap so the status bar shows the live number. */
    private fun speedIcon(bytesPerSec: Long): Bitmap {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val (value, unit) = when {
            bytesPerSec >= 1024L * 1024 -> String.format(Locale.US, if (bytesPerSec >= 10L * 1024 * 1024) "%.0f" else "%.1f", bytesPerSec / 1048576.0) to "MB/s"
            else -> (bytesPerSec / 1024).toString() to "KB/s"
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        paint.textSize = if (value.length >= 4) 44f else 54f
        canvas.drawText(value, size / 2f, 52f, paint)
        paint.textSize = 32f
        canvas.drawText(unit, size / 2f, 90f, paint)
        return bmp
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.novacleaner.app.STOP_SPEED"

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, NetSpeedService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NetSpeedService::class.java))
        }
    }
}
