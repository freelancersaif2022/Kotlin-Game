package com.saif.pymaster.notify

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
import com.saif.pymaster.MainActivity
import com.saif.pymaster.R
import com.saif.pymaster.data.AppNotification
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.NotificationType

object Notifier {
    private const val CHANNEL_PROGRESS = "progress"
    private const val CHANNEL_QUIZ = "daily_quiz"
    private const val MAX_STORED = 150

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Learning progress", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lesson, module, project and certificate updates"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_QUIZ, "Daily quiz", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "A new daily Python quiz is ready"
            }
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Adds a notification to the in-app inbox (always) and shows it in the
     * system tray when the user allows notifications.
     */
    fun add(state: AppState, title: String, body: String, type: String): AppState {
        val now = System.currentTimeMillis()
        val id = (state.notifications.maxOfOrNull { it.id } ?: 0L) + 1
        val item = AppNotification(id = id, title = title, body = body, type = type, time = now)
        return state.copy(notifications = (listOf(item) + state.notifications).take(MAX_STORED))
    }

    fun show(context: Context, state: AppState, title: String, body: String, type: String) {
        if (!state.settings.notifications) return
        if (type == NotificationType.QUIZ && !state.settings.dailyQuizReminder) return
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN, if (type == NotificationType.QUIZ) "daily_quiz" else "notifications")
        }
        val pending = PendingIntent.getActivity(
            context, type.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = if (type == NotificationType.QUIZ) CHANNEL_QUIZ else CHANNEL_PROGRESS
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(0xFF3776AB.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }
}
