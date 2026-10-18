package com.smartisland.app.system

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.smartisland.app.island.IslandNotificationListener
import com.smartisland.app.overlay.IslandAccessibilityService

data class PermissionStatus(
    val accessibility: Boolean,
    val notificationAccess: Boolean,
    val batteryUnrestricted: Boolean,
    val dndAccess: Boolean,
) {
    /** The two permissions the island cannot work without. */
    val coreReady: Boolean get() = accessibility && notificationAccess
    val grantedCount: Int get() = listOf(accessibility, notificationAccess, batteryUnrestricted, dndAccess).count { it }
}

object Permissions {

    fun status(context: Context) = PermissionStatus(
        accessibility = isAccessibilityEnabled(context),
        notificationAccess = isNotificationAccessGranted(context),
        batteryUnrestricted = isIgnoringBatteryOptimizations(context),
        dndAccess = context.systemService<NotificationManager>().isNotificationPolicyAccessGranted,
    )

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, IslandAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val cn = ComponentName(context, IslandNotificationListener::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return context.systemService<NotificationManager>().isNotificationListenerAccessGranted(cn)
        }
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(':').any { ComponentName.unflattenFromString(it) == cn }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.systemService<PowerManager>().isIgnoringBatteryOptimizations(context.packageName)

    /** Android 13+ blocks accessibility for sideloaded apps until "Allow restricted settings" is tapped. */
    fun mayNeedRestrictedSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val installer = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
        return installer != "com.android.vending"
    }

    // Intents ---------------------------------------------------------------------------------

    /** Candidate intents, most specific first; pass them to [launch]. */
    fun accessibilitySettings(context: Context): Array<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    ComponentName(context, IslandAccessibilityService::class.java).flattenToString(),
                ),
            )
        }
        add(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }.toTypedArray()

    fun notificationAccessSettings(context: Context): Array<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, IslandNotificationListener::class.java).flattenToString(),
                ),
            )
        }
        add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }.toTypedArray()

    fun batteryOptimizationSettings(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun dndSettings(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** Starts the first intent that resolves; falls back to the app's details page. */
    /**
     * Starts the first intent that resolves; falls back to the app's details page. From an activity the
     * settings screen opens on top of the app's own task, so Back returns to the app instead of home.
     */
    fun launch(context: Context, vararg intents: Intent): Boolean {
        val fromActivity = context.findActivity() != null
        fun Intent.prepared() = if (fromActivity) this else Intent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        for (intent in intents) {
            if (runCatching { context.startActivity(intent.prepared()) }.isSuccess) return true
        }
        return runCatching { context.startActivity(appDetails(context).prepared()) }.isSuccess
    }

    /** Remembers that the user went to grant a permission, so the app can come back once it's on. */
    fun expectReturn(context: Context) {
        context.getSharedPreferences(RETURN_PREFS, Context.MODE_PRIVATE).edit()
            .putLong(RETURN_KEY, System.currentTimeMillis()).apply()
    }

    /** Called when a service gets enabled: reopens the setup screen if the user just came from it. */
    fun returnToAppIfExpected(context: Context) {
        val prefs = context.getSharedPreferences(RETURN_PREFS, Context.MODE_PRIVATE)
        val at = prefs.getLong(RETURN_KEY, 0L)
        if (at == 0L || System.currentTimeMillis() - at > 5 * 60_000L) return
        prefs.edit().remove(RETURN_KEY).apply()
        val intent = Intent(context, com.smartisland.app.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            .putExtra(com.smartisland.app.MainActivity.EXTRA_ROUTE, "permissions")
        runCatching { context.startActivity(intent) }
    }

    private fun Context.findActivity(): android.app.Activity? {
        var c: Context? = this
        while (c is android.content.ContextWrapper) {
            if (c is android.app.Activity) return c
            c = c.baseContext
        }
        return null
    }

    private const val RETURN_PREFS = "permission_return"
    private const val RETURN_KEY = "expected_at"
}
