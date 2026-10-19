package com.eyescroll.app

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.eyescroll.app.service.ScrollAccessibilityService

object Readiness {

    fun isAccessibilityEnabled(context: Context): Boolean {
        if (ScrollAccessibilityService.isRunning) return true
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val me = ComponentName(context, ScrollAccessibilityService::class.java)
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.let { c -> c == me } ?: false
        }
    }
}
