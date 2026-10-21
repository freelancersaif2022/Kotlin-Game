package com.netscope.wifianalyzer.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.netscope.wifianalyzer.MainActivity
import com.netscope.wifianalyzer.R
import com.netscope.wifianalyzer.net.WifiUtils

object Notifications {
    const val CHANNEL_INTRUDER = "intruder_alerts"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_INTRUDER,
            context.getString(R.string.notif_channel_intruder),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notif_channel_intruder_desc) }
        nm.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showNewDevices(context: Context, names: List<String>) {
        if (!WifiUtils.hasNotificationPermission(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "devices")
        }
        val pi = PendingIntent.getActivity(context, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = if (names.size == 1) "New device joined your Wi-Fi" else "${names.size} new devices joined your Wi-Fi"
        val text = names.take(3).joinToString(", ") + if (names.size > 3) " and more" else ""
        val n = NotificationCompat.Builder(context, CHANNEL_INTRUDER)
            .setSmallIcon(R.drawable.ic_stat_wifi)
            .setColor(0xFF22D3EE.toInt())
            .setContentTitle(title)
            .setContentText("$text. Tap to review who's on your network.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\nTap to review who's on your network and mark devices you trust."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33
        ) {
            NotificationManagerCompat.from(context).notify(1001, n)
        }
    }
}
