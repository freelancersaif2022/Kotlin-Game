package com.netscope.wifianalyzer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.RemoteViews
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.MainActivity
import com.netscope.wifianalyzer.R
import com.netscope.wifianalyzer.net.WifiUtils

class SignalWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, WidgetUpdater.signalViews(context)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdater.ACTION_REFRESH) WidgetUpdater.updateAll(context)
    }
}

class SpeedWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, WidgetUpdater.speedViews(context)) }
    }
}

object WidgetUpdater {
    const val ACTION_REFRESH = "com.netscope.wifianalyzer.widget.REFRESH"

    fun hasWidgets(context: Context): Boolean {
        val m = AppWidgetManager.getInstance(context)
        return m.getAppWidgetIds(ComponentName(context, SignalWidgetProvider::class.java)).isNotEmpty() ||
            m.getAppWidgetIds(ComponentName(context, SpeedWidgetProvider::class.java)).isNotEmpty()
    }

    fun updateAll(context: Context) {
        val m = AppWidgetManager.getInstance(context)
        m.getAppWidgetIds(ComponentName(context, SignalWidgetProvider::class.java)).forEach {
            m.updateAppWidget(it, signalViews(context))
        }
        m.getAppWidgetIds(ComponentName(context, SpeedWidgetProvider::class.java)).forEach {
            m.updateAppWidget(it, speedViews(context))
        }
    }

    private fun openApp(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun lockedViews(context: Context, title: String): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_locked).apply {
            setTextViewText(R.id.locked_title, title)
            setOnClickPendingIntent(R.id.widget_root, openApp(context, "premium", 30))
        }

    fun signalViews(context: Context): RemoteViews {
        if (!AppGraph.premium.isPremium) return lockedViews(context, "Signal widget")
        val snap = WifiUtils.snapshot(context)
        return RemoteViews(context.packageName, R.layout.widget_signal).apply {
            if (snap.hasSignal) {
                setTextViewText(R.id.signal_ssid, snap.ssid ?: "Wi-Fi connected")
                setTextViewText(R.id.signal_dbm, snap.rssi.toString())
                setTextViewText(R.id.signal_percent, "${snap.signalPercent}%")
                setTextViewText(R.id.signal_quality, WifiUtils.qualityLabel(snap.rssi))
                setProgressBar(R.id.signal_progress, 100, snap.signalPercent, false)
                val meta = listOfNotNull(
                    snap.band.ifBlank { null },
                    snap.channel.takeIf { it > 0 }?.let { "Ch $it" },
                    snap.linkSpeed.takeIf { it > 0 }?.let { "$it Mbps link" },
                ).joinToString("  •  ")
                setTextViewText(R.id.signal_meta, meta)
            } else {
                setTextViewText(R.id.signal_ssid, "Not connected to Wi-Fi")
                setTextViewText(R.id.signal_dbm, "--")
                setTextViewText(R.id.signal_percent, "")
                setTextViewText(R.id.signal_quality, "Offline")
                setProgressBar(R.id.signal_progress, 100, 0, false)
                setTextViewText(R.id.signal_meta, "Tap to refresh")
            }
            val refresh = Intent(context, SignalWidgetProvider::class.java).setAction(ACTION_REFRESH)
            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getBroadcast(context, 31, refresh, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
    }

    fun speedViews(context: Context): RemoteViews {
        if (!AppGraph.premium.isPremium) return lockedViews(context, "Speed test widget")
        val last = AppGraph.history.results.value.firstOrNull()
        val unit = AppGraph.prefs.unit.value
        return RemoteViews(context.packageName, R.layout.widget_speed).apply {
            if (last != null) {
                setTextViewText(R.id.speed_down, WifiUtils.formatSpeed(last.downloadMbps, unit).first)
                setTextViewText(R.id.speed_up, WifiUtils.formatSpeed(last.uploadMbps, unit).first)
                setTextViewText(R.id.speed_ping, "${last.pingMs.toInt()}")
                setTextViewText(
                    R.id.speed_time,
                    DateUtils.getRelativeTimeSpanString(last.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                )
                setTextViewText(R.id.speed_unit, "${unit.label} • ping in ms • ${last.network}")
            } else {
                setTextViewText(R.id.speed_down, "--")
                setTextViewText(R.id.speed_up, "--")
                setTextViewText(R.id.speed_ping, "--")
                setTextViewText(R.id.speed_time, "")
                setTextViewText(R.id.speed_unit, "No tests yet")
            }
            setOnClickPendingIntent(R.id.speed_run, openApp(context, "speed?auto=true", 32))
            setOnClickPendingIntent(R.id.widget_root, openApp(context, "history", 33))
        }
    }
}
