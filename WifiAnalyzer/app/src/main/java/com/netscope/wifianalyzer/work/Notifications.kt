package com.netscope.wifianalyzer.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.MainActivity
import com.netscope.wifianalyzer.R
import com.netscope.wifianalyzer.net.WifiUtils

object Notifications {
    const val CHANNEL_INTRUDER = "intruder_alerts"
    private const val GROUP_INTRUDER = "intruder_group"
    private const val ID_INTRUDER = 1001
    const val ACTION_TRUST_ALL = "com.netscope.wifianalyzer.action.TRUST_ALL"
    const val EXTRA_NETWORK = "network_key"
    const val EXTRA_KEYS = "device_keys"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_INTRUDER,
            context.getString(R.string.notif_channel_intruder),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_intruder_desc)
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFF22D3EE.toInt()
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        WifiUtils.hasNotificationPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun openDevices(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "devices")
        }
        return PendingIntent.getActivity(context, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Heads-up alert listing new devices, with "Review" and "Trust all" actions. */
    @SuppressLint("MissingPermission")
    fun showNewDevices(context: Context, networkKey: String, networkName: String, devices: List<Pair<String, String>>) {
        if (!canNotify(context) || devices.isEmpty()) return
        val names = devices.map { it.second }
        val title = if (names.size == 1) "New device joined $networkName" else "${names.size} new devices joined $networkName"
        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title)
        names.take(6).forEach { inbox.addLine(it) }
        if (names.size > 6) inbox.setSummaryText("+${names.size - 6} more")

        val trust = Intent(context, TrustDevicesReceiver::class.java).apply {
            action = ACTION_TRUST_ALL
            putExtra(EXTRA_NETWORK, networkKey)
            putExtra(EXTRA_KEYS, devices.map { it.first }.toTypedArray())
        }
        val trustPi = PendingIntent.getBroadcast(context, 8, trust, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val n = NotificationCompat.Builder(context, CHANNEL_INTRUDER)
            .setSmallIcon(R.drawable.ic_stat_wifi)
            .setColor(0xFF22D3EE.toInt())
            .setContentTitle(title)
            .setContentText(names.take(2).joinToString(", ") + if (names.size > 2) " and more" else "")
            .setStyle(inbox)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setGroup(GROUP_INTRUDER)
            .setAutoCancel(true)
            .setContentIntent(openDevices(context))
            .addAction(R.drawable.ic_stat_wifi, "Review devices", openDevices(context))
            .addAction(R.drawable.ic_stat_wifi, "Trust all", trustPi)
            .build()
        notify(context, ID_INTRUDER, n)
    }

    /** Lets people confirm alerts work on their phone. */
    @SuppressLint("MissingPermission")
    fun showTest(context: Context): Boolean {
        if (!canNotify(context)) return false
        val n = NotificationCompat.Builder(context, CHANNEL_INTRUDER)
            .setSmallIcon(R.drawable.ic_stat_wifi)
            .setColor(0xFF22D3EE.toInt())
            .setContentTitle("Intruder alert is working")
            .setContentText("You'll get an alert like this when an unknown device joins your Wi-Fi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openDevices(context))
            .build()
        notify(context, ID_INTRUDER + 1, n)
        return true
    }

    fun cancelIntruder(context: Context) = NotificationManagerCompat.from(context).cancel(ID_INTRUDER)

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, id: Int, n: android.app.Notification) {
        if (Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(id, n)
        }
    }
}

/** Handles the "Trust all" notification action. */
class TrustDevicesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Notifications.ACTION_TRUST_ALL) return
        val network = intent.getStringExtra(Notifications.EXTRA_NETWORK) ?: return
        intent.getStringArrayExtra(Notifications.EXTRA_KEYS)?.forEach { AppGraph.knownDevices.setTrusted(network, it, true) }
        Notifications.cancelIntruder(context)
    }
}
