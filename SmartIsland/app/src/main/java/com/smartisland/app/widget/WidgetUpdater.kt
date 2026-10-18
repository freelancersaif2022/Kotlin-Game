package com.smartisland.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** Pushes fresh RemoteViews straight to placed widgets, skipping kinds with no instances. */
object WidgetUpdater {

    fun update(context: Context, kind: WidgetKind) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = runCatching { manager.getAppWidgetIds(ComponentName(context, kind.provider)) }.getOrNull()
        if (ids == null || ids.isEmpty()) return
        val views = runCatching { WidgetRenderer.render(context, kind) }.getOrNull() ?: return
        ids.forEach { runCatching { manager.updateAppWidget(it, views) } }
    }

    fun updateAll(context: Context) = WidgetKind.entries.forEach { update(context, it) }

    fun updateBattery(context: Context) {
        update(context, WidgetKind.BATTERY_RING)
        update(context, WidgetKind.CHARGING)
        update(context, WidgetKind.DIGITAL_CLOCK)
        update(context, WidgetKind.SYSTEM_STATS)
    }

    fun updateCharging(context: Context) = update(context, WidgetKind.CHARGING)
    fun updateStats(context: Context) = update(context, WidgetKind.SYSTEM_STATS)
    fun updateMedia(context: Context) = update(context, WidgetKind.MEDIA)
    fun updateVolume(context: Context) = update(context, WidgetKind.VOLUME)
    fun updateToggles(context: Context) = update(context, WidgetKind.QUICK_TOGGLES)
    fun updatePower(context: Context) = update(context, WidgetKind.POWER)
}
