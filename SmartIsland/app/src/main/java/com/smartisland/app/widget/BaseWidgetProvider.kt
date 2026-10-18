package com.smartisland.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

abstract class BaseWidgetProvider(private val kind: WidgetKind) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = WidgetRenderer.render(context, kind)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        manager.updateAppWidget(id, WidgetRenderer.render(context, kind))
    }
}
