package com.smartisland.app.system

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build

object PendingIntents {

    /**
     * Fires another app's PendingIntent (call answer, notification tap...). Android 14+ requires the
     * sender to opt in before the target may start an activity from the background.
     */
    fun send(context: Context, pi: PendingIntent?): Boolean {
        if (pi == null) return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
                pi.send(context, 0, null, null, null, null, options)
            } else {
                pi.send()
            }
        }.isSuccess
    }
}
