package com.lexora.english.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lexora.english.MainActivity
import com.lexora.english.R

object Notifier {
    const val CHANNEL_DAILY = "daily_quiz"
    const val CHANNEL_PROGRESS = "progress"
    const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "Daily quiz", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A new set of 5 questions every day"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Progress & achievements", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lessons, modules, projects and certificate updates"
            }
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, n: AppNotification) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
        }
        val pending = PendingIntent.getActivity(
            context, (n.id % Int.MAX_VALUE).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = if (n.type == "daily") CHANNEL_DAILY else CHANNEL_PROGRESS
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_lexora)
            .setColor(0xFF2F5BEA.toInt())
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((n.id % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }
}
