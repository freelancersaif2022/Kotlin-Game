package com.novacleaner.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val CHANNEL_SPEED = "net_speed"
    const val CHANNEL_REMINDERS = "reminders"
    const val ID_SPEED = 1001
    const val ID_REMINDER = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SPEED, "Network speed meter", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live upload and download speed in the status bar"
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Cleanup reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Occasional reminders to keep your storage tidy"
            },
        )
    }
}
