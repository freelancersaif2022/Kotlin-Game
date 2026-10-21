package com.novacleaner.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novacleaner.app.MainActivity
import com.novacleaner.app.R
import com.novacleaner.app.core.Permissions
import com.novacleaner.app.core.Prefs
import com.novacleaner.app.service.Notifications
import java.util.concurrent.TimeUnit

/** Gentle reminder when the user hasn't cleaned for a few days. No scare tactics. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = Prefs(applicationContext).snapshot()
        val sinceClean = System.currentTimeMillis() - settings.lastCleanAt
        if (!settings.remindersEnabled || sinceClean < TimeUnit.DAYS.toMillis(3)) return Result.success()
        if (!Permissions.hasNotifications(applicationContext)) return Result.success()
        val intent = PendingIntent.getActivity(
            applicationContext, 2, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(applicationContext, R.color.brand_emerald))
            .setContentTitle("Time for a quick tidy-up")
            .setContentText("It's been a few days since your last cleanup. Tap to scan for junk files.")
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(Notifications.ID_REMINDER, notification) }
        return Result.success()
    }

    companion object {
        private const val NAME = "cleanup_reminder"

        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(NAME); return
            }
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
