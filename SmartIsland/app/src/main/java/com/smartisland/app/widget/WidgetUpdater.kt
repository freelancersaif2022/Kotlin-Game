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
        update(context, WidgetKind.ISLAND_GLANCE)
        update(context, WidgetKind.SMART_GLANCE)
    }

    fun updateCharging(context: Context) = update(context, WidgetKind.CHARGING)
    /** Called every minute while the screen is on. */
    fun updateStats(context: Context) {
        update(context, WidgetKind.SYSTEM_STATS)
        update(context, WidgetKind.STORAGE_RING)
        update(context, WidgetKind.DAY_PROGRESS)
        update(context, WidgetKind.SMART_GLANCE)
    }

    fun updateMedia(context: Context) {
        update(context, WidgetKind.MEDIA)
        update(context, WidgetKind.ISLAND_GLANCE)
        update(context, WidgetKind.SMART_GLANCE)
    }
    fun updateVolume(context: Context) = update(context, WidgetKind.VOLUME)
    fun updateToggles(context: Context) = update(context, WidgetKind.QUICK_TOGGLES)
    fun updatePower(context: Context) = update(context, WidgetKind.POWER)
}
