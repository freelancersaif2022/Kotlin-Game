package com.eyescroll.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.eyescroll.app.data.SettingsStore

class EyeScrollApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.get(this)
        val channel = NotificationChannel(
            CHANNEL_TRACKING,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_TRACKING = "tracking"
    }
}
