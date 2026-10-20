package com.vaultly.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Daily deadline check at 9:00 local time. Uses an inexact, doze-friendly alarm (no special
 * permission needed); the periodic WorkManager job in [ReminderWorker] is a second safety net.
 */
object ReminderScheduler {
    private const val HOUR = 9

    fun scheduleDaily(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val now = ZonedDateTime.now()
        var next = now.with(LocalTime.of(HOUR, 0))
        if (!next.isAfter(now)) next = ZonedDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(HOUR, 0), now.zone)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending(context))
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 7001, Intent(context, ReminderAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Fires every morning: run the check and book tomorrow's alarm. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderWorker.runNow(context)
        ReminderScheduler.scheduleDaily(context)
    }
}

/** Alarms are cleared on reboot, app update and clock changes; book them again. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.scheduleDaily(context)
        ReminderWorker.runNow(context)
    }
}
