package com.smartisland.app.widget

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioManager
import android.os.Environment
import android.os.StatFs
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import com.smartisland.app.MainActivity
import com.smartisland.app.R
import com.smartisland.app.container
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.system.BatterySnapshot
import com.smartisland.app.system.Permissions
import com.smartisland.app.system.SystemActions
import com.smartisland.app.system.VolumeStream
import com.smartisland.app.system.systemService
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Builds the RemoteViews for every widget kind. Also used by the in-app gallery for live previews. */
object WidgetRenderer {

    fun render(context: Context, kind: WidgetKind, forceUnlocked: Boolean = false): RemoteViews {
        val unlocked = forceUnlocked || context.container.premium.current.isWidgetUnlocked(kind.id, kind.premium)
        if (!unlocked) return locked(context, kind)
        val views = RemoteViews(context.packageName, kind.layout)
        when (kind) {
            WidgetKind.DIGITAL_CLOCK -> digitalClock(context, views)
            WidgetKind.ANALOG_CLOCK -> views.setOnClickPendingIntent(R.id.root, openClockApp(context))
            WidgetKind.BATTERY_RING -> batteryRing(context, views)
            WidgetKind.CALENDAR -> calendar(context, views)
            WidgetKind.POWER -> power(context, views)
            WidgetKind.VOLUME -> volume(context, views)
            WidgetKind.CHARGING -> charging(context, views)
            WidgetKind.WORLD_CLOCK -> views.setOnClickPendingIntent(R.id.root, openClockApp(context))
            WidgetKind.MEDIA -> media(context, views)
            WidgetKind.STOPWATCH -> stopwatch(context, views)
            WidgetKind.SYSTEM_STATS -> stats(context, views)
            WidgetKind.QUICK_TOGGLES -> toggles(context, views)
            WidgetKind.ISLAND_GLANCE -> islandGlance(context, views)
            WidgetKind.DAY_PROGRESS -> dayProgress(context, views)
            WidgetKind.STORAGE_RING -> storageRing(context, views)
            WidgetKind.DUAL_CLOCK -> views.setOnClickPendingIntent(R.id.root, openClockApp(context))
            WidgetKind.SMART_GLANCE -> smartGlance(context, views)
            WidgetKind.KITTY, WidgetKind.ORBIT -> views.setOnClickPendingIntent(R.id.root, openClockApp(context))
            WidgetKind.PUPPY -> views.setOnClickPendingIntent(R.id.root, openCalendar(context))
            WidgetKind.CAMPFIRE -> campfire(context, views)
            WidgetKind.AQUARIUM -> views.setOnClickPendingIntent(R.id.root, openApp(context, "home"))
        }
        return views
    }

    private fun locked(context: Context, kind: WidgetKind): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_locked).apply {
            setTextViewText(R.id.locked_title, kind.title)
            setOnClickPendingIntent(R.id.root, openApp(context, "widgets", kind.id))
        }

    // Individual widgets ----------------------------------------------------------------------

    private fun digitalClock(context: Context, v: RemoteViews) {
        val b = BatterySnapshot.from(context)
        v.setTextViewText(R.id.battery_text, "${b.level}%")
        v.setInt(R.id.battery_icon, "setColorFilter", if (b.isCharging) GREEN else levelColor(b.level))
        v.setTextViewText(R.id.alarm_text, nextAlarm(context) ?: "No alarm")
        v.setViewVisibility(R.id.clock_ampm, if (DateFormat.is24HourFormat(context)) View.GONE else View.VISIBLE)
        v.setOnClickPendingIntent(R.id.root, openClockApp(context))
    }

    private fun batteryRing(context: Context, v: RemoteViews) {
        val b = BatterySnapshot.from(context)
        val active = when {
            b.isCharging -> R.id.ring_green
            b.level <= 20 -> R.id.ring_red
            else -> R.id.ring
        }
        listOf(R.id.ring, R.id.ring_green, R.id.ring_red).forEach {
            v.setViewVisibility(it, if (it == active) View.VISIBLE else View.GONE)
        }
        v.setProgressBar(active, 100, b.level, false)
        v.setTextViewText(R.id.level_text, "${b.level}%")
        v.setTextViewText(R.id.status_text, if (b.isCharging) (if (b.isFast) "Fast charging" else "Charging") else "Battery")
        v.setTextColor(R.id.status_text, if (b.isCharging) GREEN else MUTED)
        v.setViewVisibility(R.id.bolt, if (b.isCharging) View.VISIBLE else View.GONE)
        v.setInt(R.id.bolt, "setColorFilter", GREEN)
        v.setOnClickPendingIntent(R.id.root, openApp(context, "home"))
    }

    private fun calendar(context: Context, v: RemoteViews) {
        v.setTextViewText(R.id.alarm_text, nextAlarm(context)?.let { "⏰ $it" } ?: "No alarm")
        v.setOnClickPendingIntent(R.id.root, openCalendar(context))
    }

    private fun power(context: Context, v: RemoteViews) {
        // Without the accessibility service the buttons cannot work, so they open setup instead.
        val ready = Permissions.isAccessibilityEnabled(context)
        fun pi(action: String) = if (ready) WidgetActionReceiver.pending(context, action) else openApp(context, "permissions")
        v.setOnClickPendingIntent(R.id.btn_power, pi(WidgetActionReceiver.POWER))
        v.setOnClickPendingIntent(R.id.btn_lock, pi(WidgetActionReceiver.LOCK))
        v.setOnClickPendingIntent(R.id.btn_screenshot, pi(WidgetActionReceiver.SCREENSHOT))
        v.setOnClickPendingIntent(R.id.btn_shade, pi(WidgetActionReceiver.SHADE))
    }

    private fun volume(context: Context, v: RemoteViews) {
        val vc = context.container.volume
        val pct = vc.percent(VolumeStream.MEDIA)
        val muted = vc.isMuted(VolumeStream.MEDIA) || pct == 0
        v.setTextViewText(R.id.vol_text, if (muted) "Muted" else "$pct%")
        v.setProgressBar(R.id.vol_bar, 100, if (muted) 0 else pct, false)
        v.setImageViewResource(R.id.btn_mute, if (muted) R.drawable.ic_w_volume_off else R.drawable.ic_w_volume_up)
        v.setInt(R.id.btn_mute, "setBackgroundResource", if (muted) R.drawable.circle_red else R.drawable.circle_btn_active)
        v.setOnClickPendingIntent(R.id.btn_vol_down, WidgetActionReceiver.pending(context, WidgetActionReceiver.VOL_DOWN))
        v.setOnClickPendingIntent(R.id.btn_vol_up, WidgetActionReceiver.pending(context, WidgetActionReceiver.VOL_UP))
        v.setOnClickPendingIntent(R.id.btn_mute, WidgetActionReceiver.pending(context, WidgetActionReceiver.MUTE))
    }

    private fun charging(context: Context, v: RemoteViews) {
        val b = BatterySnapshot.from(context)
        v.setTextViewText(R.id.level_text, "${b.level}%")
        v.setProgressBar(R.id.charge_bar, 100, b.level, false)
        val status = when {
            !b.isCharging -> "On battery"
            b.isFast && b.watts >= 1f -> "Fast charging · %.0f W".format(b.watts)
            b.isFast -> "Fast charging"
            b.watts >= 1f -> "Charging · %.1f W".format(b.watts)
            else -> "Charging"
        }
        v.setTextViewText(R.id.status_text, status)
        v.setTextColor(R.id.status_text, if (b.isCharging) GREEN else MUTED)
        v.setInt(R.id.bolt_bg, "setBackgroundResource", if (b.isCharging) R.drawable.circle_green else R.drawable.circle_btn)
        v.setTextViewText(R.id.detail_text, "${b.source} · %.1f°C · ${b.healthLabel}".format(b.temperatureC))
        v.setOnClickPendingIntent(R.id.root, openApp(context, "home"))
    }

    private fun media(context: Context, v: RemoteViews) {
        val m = IslandEngine.mediaState.value
        if (m == null) {
            v.setTextViewText(R.id.media_app, "NOW PLAYING")
            v.setTextViewText(R.id.media_title, "Nothing playing")
            v.setTextViewText(R.id.media_artist, "Start music in any app")
            v.setImageViewResource(R.id.media_art, R.drawable.ic_w_music)
            v.setImageViewResource(R.id.btn_play, R.drawable.ic_w_play)
        } else {
            v.setTextViewText(R.id.media_app, appLabel(context, m.packageName).uppercase(Locale.getDefault()))
            v.setTextViewText(R.id.media_title, m.title)
            v.setTextViewText(R.id.media_artist, m.artist)
            val art = m.art?.let { rounded(it, 22f) }
            if (art != null) v.setImageViewBitmap(R.id.media_art, art) else v.setImageViewResource(R.id.media_art, R.drawable.ic_w_music)
            v.setImageViewResource(R.id.btn_play, if (m.isPlaying) R.drawable.ic_w_pause else R.drawable.ic_w_play)
        }
        v.setOnClickPendingIntent(R.id.btn_prev, WidgetActionReceiver.pending(context, WidgetActionReceiver.MEDIA_PREV))
        v.setOnClickPendingIntent(R.id.btn_play, WidgetActionReceiver.pending(context, WidgetActionReceiver.MEDIA_PLAY))
        v.setOnClickPendingIntent(R.id.btn_next, WidgetActionReceiver.pending(context, WidgetActionReceiver.MEDIA_NEXT))
        v.setOnClickPendingIntent(R.id.media_art, WidgetActionReceiver.pending(context, WidgetActionReceiver.MEDIA_OPEN))
    }

    private fun stopwatch(context: Context, v: RemoteViews) {
        val s = StopwatchState(context)
        v.setChronometer(R.id.chrono, s.displayBase(), null, s.running)
        v.setTextViewText(R.id.sw_status, if (s.running) "RUNNING" else if (s.pausedElapsed > 0) "PAUSED" else "STOPWATCH")
        v.setImageViewResource(R.id.btn_toggle, if (s.running) R.drawable.ic_w_pause else R.drawable.ic_w_play)
        v.setInt(R.id.btn_toggle, "setBackgroundResource", if (s.running) R.drawable.circle_orange else R.drawable.circle_green)
        v.setOnClickPendingIntent(R.id.btn_toggle, WidgetActionReceiver.pending(context, WidgetActionReceiver.SW_TOGGLE))
        v.setOnClickPendingIntent(R.id.btn_reset, WidgetActionReceiver.pending(context, WidgetActionReceiver.SW_RESET))
    }

    private fun stats(context: Context, v: RemoteViews) {
        val mi = ActivityManager.MemoryInfo()
        context.systemService<ActivityManager>().getMemoryInfo(mi)
        val usedRam = mi.totalMem - mi.availMem
        v.setTextViewText(R.id.ram_text, "${gb(usedRam)} / ${gb(mi.totalMem)} GB")
        v.setProgressBar(R.id.ram_bar, 100, percent(usedRam, mi.totalMem), false)

        val fs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        if (fs != null) {
            val total = fs.totalBytes
            val used = total - fs.availableBytes
            v.setTextViewText(R.id.storage_text, "${gb(used)} / ${gb(total)} GB")
            v.setProgressBar(R.id.storage_bar, 100, percent(used, total), false)
        }

        val b = BatterySnapshot.from(context)
        v.setTextViewText(R.id.battery_text, if (b.isCharging) "${b.level}% · charging" else "${b.level}%")
        v.setProgressBar(R.id.battery_bar, 100, b.level, false)
        v.setOnClickPendingIntent(R.id.root, WidgetActionReceiver.pending(context, WidgetActionReceiver.REFRESH))
    }

    private fun toggles(context: Context, v: RemoteViews) {
        val torch = SystemActions.torchOn
        v.setInt(R.id.t_torch_icon, "setBackgroundResource", if (torch) R.drawable.circle_btn_active else R.drawable.circle_btn)
        v.setTextViewText(R.id.t_torch_label, if (torch) "Light on" else "Flashlight")
        v.setOnClickPendingIntent(R.id.t_torch, WidgetActionReceiver.pending(context, WidgetActionReceiver.TORCH))

        val ringer = context.container.volume.ringerMode
        val (icon, label) = when (ringer) {
            AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_w_bell_off to "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_w_vibrate to "Vibrate"
            else -> R.drawable.ic_w_bell to "Ring"
        }
        v.setImageViewResource(R.id.t_ringer_icon, icon)
        v.setTextViewText(R.id.t_ringer_label, label)
        v.setInt(
            R.id.t_ringer_icon, "setBackgroundResource",
            if (ringer == AudioManager.RINGER_MODE_NORMAL) R.drawable.circle_btn else R.drawable.circle_orange,
        )
        v.setOnClickPendingIntent(R.id.t_ringer, WidgetActionReceiver.pending(context, WidgetActionReceiver.RINGER))

        val nm = context.systemService<NotificationManager>()
        val dndOn = nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
        v.setInt(R.id.t_dnd_icon, "setBackgroundResource", if (dndOn) R.drawable.circle_btn_active else R.drawable.circle_btn)
        v.setOnClickPendingIntent(
            R.id.t_dnd,
            if (nm.isNotificationPolicyAccessGranted) WidgetActionReceiver.pending(context, WidgetActionReceiver.DND)
            else openApp(context, "permissions"),
        )

        val islandOn = context.container.settings.current.enabled
        v.setInt(R.id.t_island_icon, "setBackgroundResource", if (islandOn) R.drawable.circle_btn_active else R.drawable.circle_btn)
        v.setTextViewText(R.id.t_island_label, if (islandOn) "Island on" else "Island off")
        v.setOnClickPendingIntent(R.id.t_island, WidgetActionReceiver.pending(context, WidgetActionReceiver.ISLAND))
    }

    private fun campfire(context: Context, v: RemoteViews) {
        val b = BatterySnapshot.from(context)
        v.setTextViewText(R.id.fire_battery, if (b.isCharging) "⚡ ${b.level}%" else "${b.level}%")
        v.setOnClickPendingIntent(R.id.root, openApp(context, "home"))
    }

    private fun islandGlance(context: Context, v: RemoteViews) {
        val b = BatterySnapshot.from(context)
        v.setTextViewText(R.id.ig_battery, "${b.level}%")
        v.setTextColor(R.id.ig_battery, if (b.isCharging) GREEN else levelColor(b.level))
        v.setViewVisibility(R.id.ig_bolt, if (b.isCharging) View.VISIBLE else View.GONE)
        v.setInt(R.id.ig_bolt, "setColorFilter", GREEN)
        val m = IslandEngine.mediaState.value
        v.setTextViewText(R.id.ig_media, m?.let { "${it.title} · ${it.artist}" } ?: "Nothing playing")
        v.setInt(R.id.ig_music, "setColorFilter", m?.accent ?: MUTED)
        v.setOnClickPendingIntent(R.id.root, openApp(context, "home"))
    }

    private fun dayProgress(context: Context, v: RemoteViews) {
        val now = Calendar.getInstance()
        fun fraction(field: Int): Float {
            val start = (now.clone() as Calendar).apply {
                when (field) {
                    Calendar.DAY_OF_YEAR -> Unit
                    Calendar.WEEK_OF_YEAR -> set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    Calendar.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
                    Calendar.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
                }
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis > now.timeInMillis) add(Calendar.WEEK_OF_YEAR, -1)
            }
            val end = (start.clone() as Calendar).apply {
                when (field) {
                    Calendar.DAY_OF_YEAR -> add(Calendar.DAY_OF_YEAR, 1)
                    Calendar.WEEK_OF_YEAR -> add(Calendar.WEEK_OF_YEAR, 1)
                    Calendar.MONTH -> add(Calendar.MONTH, 1)
                    Calendar.YEAR -> add(Calendar.YEAR, 1)
                }
            }
            return ((now.timeInMillis - start.timeInMillis).toFloat() / (end.timeInMillis - start.timeInMillis)).coerceIn(0f, 1f)
        }
        listOf(
            Triple(Calendar.DAY_OF_YEAR, R.id.dp_day_text, R.id.dp_day_bar),
            Triple(Calendar.WEEK_OF_YEAR, R.id.dp_week_text, R.id.dp_week_bar),
            Triple(Calendar.MONTH, R.id.dp_month_text, R.id.dp_month_bar),
            Triple(Calendar.YEAR, R.id.dp_year_text, R.id.dp_year_bar),
        ).forEach { (field, text, bar) ->
            val f = fraction(field)
            v.setTextViewText(text, "${(f * 100).toInt()}%")
            v.setProgressBar(bar, 1000, (f * 1000).toInt(), false)
        }
        v.setOnClickPendingIntent(R.id.root, WidgetActionReceiver.pending(context, WidgetActionReceiver.REFRESH))
    }

    private fun storageRing(context: Context, v: RemoteViews) {
        val fs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        if (fs != null) {
            val total = fs.totalBytes
            val used = total - fs.availableBytes
            v.setProgressBar(R.id.sr_ring, 100, percent(used, total), false)
            v.setTextViewText(R.id.sr_percent, "${percent(used, total)}%")
            v.setTextViewText(R.id.sr_free, "${gb(fs.availableBytes)} GB free")
        }
        v.setOnClickPendingIntent(R.id.root, WidgetActionReceiver.pending(context, WidgetActionReceiver.REFRESH))
    }

    private fun smartGlance(context: Context, v: RemoteViews) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        v.setTextViewText(
            R.id.sg_greeting,
            when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..21 -> "Good evening"
                else -> "Good night"
            },
        )
        val b = BatterySnapshot.from(context)
        v.setTextViewText(R.id.sg_battery, if (b.isCharging) "${b.level}% · charging" else "${b.level}%")
        v.setInt(R.id.sg_bolt, "setColorFilter", if (b.isCharging) GREEN else levelColor(b.level))
        v.setTextViewText(R.id.sg_alarm, nextAlarm(context) ?: "No alarm")
        val m = IslandEngine.mediaState.value
        v.setTextViewText(R.id.sg_media, "♪  " + (m?.let { "${it.title} · ${it.artist}" } ?: "Nothing playing"))
        v.setOnClickPendingIntent(R.id.root, openApp(context, "home"))
    }

    // Helpers ---------------------------------------------------------------------------------

    fun openApp(context: Context, route: String, highlight: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_ROUTE, route)
            .putExtra(MainActivity.EXTRA_HIGHLIGHT, highlight)
        return PendingIntent.getActivity(
            context, (route + highlight).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openClockApp(context: Context): PendingIntent {
        val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 9001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openCalendar(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(android.net.Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 9002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun nextAlarm(context: Context): String? {
        val next = context.systemService<AlarmManager>().nextAlarmClock ?: return null
        val pattern = if (DateFormat.is24HourFormat(context)) "EEE HH:mm" else "EEE h:mm a"
        return DateFormat.format(pattern, Date(next.triggerTime)).toString()
    }

    private fun appLabel(context: Context, pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault("Now playing")

    private fun rounded(src: Bitmap, radiusPx: Float): Bitmap? = runCatching {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        val r = radiusPx * src.width / 86f
        Canvas(out).drawRoundRect(RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), r, r, paint)
        out
    }.getOrNull()

    private fun gb(bytes: Long): String = "%.1f".format(bytes / 1_073_741_824.0)

    private fun percent(part: Long, total: Long): Int = if (total <= 0) 0 else ((part * 100) / total).toInt().coerceIn(0, 100)

    private fun levelColor(level: Int): Int = when {
        level <= 20 -> RED
        level <= 40 -> ORANGE
        else -> 0xFFFFFFFF.toInt()
    }

    private const val GREEN = 0xFF30D158.toInt()
    private const val RED = 0xFFFF453A.toInt()
    private const val ORANGE = 0xFFFF9F0A.toInt()
    private const val MUTED = 0x99FFFFFF.toInt()
}
