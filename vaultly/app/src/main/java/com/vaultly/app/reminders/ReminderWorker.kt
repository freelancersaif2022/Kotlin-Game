package com.vaultly.app.reminders

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vaultly.app.MainActivity
import com.vaultly.app.R
import com.vaultly.app.VaultlyApp
import com.vaultly.app.data.Item
import com.vaultly.app.domain.Deadline
import com.vaultly.app.domain.DeadlineKind
import com.vaultly.app.domain.Deadlines
import com.vaultly.app.util.Format
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Runs about twice a day and posts a notification when a deadline reaches one of the reminder steps. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as VaultlyApp
        val prefs = app.prefs
        val today = LocalDate.now()
        app.repository.active().forEach { item ->
            Deadlines.all(item).forEach { d ->
                val enabled = if (d.kind == DeadlineKind.RETURN) prefs.remindReturn.value else prefs.remindWarranty.value
                if (!enabled) return@forEach
                val left = d.daysLeft(today)
                val steps = if (d.kind == DeadlineKind.RETURN) RETURN_STEPS else WARRANTY_STEPS
                // Fire the closest step we've reached but not yet announced (covers days the worker didn't run).
                val step = steps.filter { left in 0L..it.toLong() }.minOrNull() ?: return@forEach
                val key = "${item.id}:${d.kind}:${d.end}:$step"
                if (!prefs.wasNotified(key)) {
                    notify(applicationContext, item, d, left)
                    prefs.markNotified(key)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL = "deadlines"
        private val RETURN_STEPS = listOf(7, 3, 1, 0)
        private val WARRANTY_STEPS = listOf(30, 7, 0)
        private const val WORK = "deadline-reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL, context.getString(R.string.channel_deadlines), NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_deadlines_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        @SuppressLint("MissingPermission")
        private fun notify(context: Context, item: Item, d: Deadline, left: Long) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val title = when (d.kind) {
                DeadlineKind.RETURN -> if (left == 0L) "Last day to return ${item.title}" else "${Deadlines.describeLeft(left)} to return ${item.title}"
                DeadlineKind.WARRANTY -> if (left == 0L) "Warranty ends today: ${item.title}" else "Warranty ends in ${Deadlines.describeLeft(left).removeSuffix(" left")}: ${item.title}"
            }
            val body = when (d.kind) {
                DeadlineKind.RETURN -> "Bought at ${item.store}. Return by ${Format.date(d.end)} – your receipt is ready in Vaultly."
                DeadlineKind.WARRANTY -> "Check it still works properly. Covered until ${Format.date(d.end)}."
            }
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_ITEM_ID, item.id)
            }
            val pending = PendingIntent.getActivity(
                context, item.id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_vaultly)
                .setColor(0xFFC9A35A.toInt())
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            val id = (item.id * 10 + d.kind.ordinal).toInt()
            runCatching { NotificationManagerCompat.from(context).notify(id, n) }
        }
    }
}
