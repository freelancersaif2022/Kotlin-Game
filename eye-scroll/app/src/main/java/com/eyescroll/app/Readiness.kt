package com.eyescroll.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.service.EyeScrollAccessibilityService

/** Single place that answers "is everything set up?" for UI, bubble and tile. */
object Readiness {

    fun hasCamera(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun hasNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun isAccessibilityEnabled(context: Context): Boolean {
        if (EyeScrollAccessibilityService.isRunning) return true
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val me = ComponentName(context, EyeScrollAccessibilityService::class.java)
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.let { c -> c == me } ?: false
        }
    }

    fun isCalibrated(context: Context): Boolean {
        val store = SettingsStore.get(context)
        return store.calibration.value?.model(store.current.mode)?.isUsable == true
    }

    fun canStartTracking(context: Context): Boolean = hasCamera(context) && isCalibrated(context)
}
