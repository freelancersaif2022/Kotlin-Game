package com.smartisland.app.widget

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.smartisland.app.container
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.overlay.IslandAccessibilityService
import com.smartisland.app.system.SystemActions
import com.smartisland.app.system.VolumeStream
import com.smartisland.app.system.systemService

/** Handles every button tap on the home-screen widgets. */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            POWER -> SystemActions.powerMenu()
            LOCK -> SystemActions.lockScreen()
            SCREENSHOT -> SystemActions.screenshot()
            SHADE -> SystemActions.notifications()

            VOL_UP, VOL_DOWN -> {
                val service = IslandAccessibilityService.instance
                val hud = service?.canShowHud == true
                app.container.volume.step(VolumeStream.MEDIA, intent.action == VOL_UP, showSystemUi = !hud)
                if (hud) service?.showVolumeHud()
                WidgetUpdater.updateVolume(app)
            }
            MUTE -> {
                app.container.volume.toggleMute(VolumeStream.MEDIA)
                WidgetUpdater.updateVolume(app)
            }

            MEDIA_PLAY -> mediaKey(app, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { it.playPause() }
            MEDIA_NEXT -> mediaKey(app, KeyEvent.KEYCODE_MEDIA_NEXT) { it.next() }
            MEDIA_PREV -> mediaKey(app, KeyEvent.KEYCODE_MEDIA_PREVIOUS) { it.previous() }
            MEDIA_OPEN -> IslandEngine.mediaCommands?.open()

            SW_TOGGLE -> {
                StopwatchState(app).toggle()
                WidgetUpdater.update(app, WidgetKind.STOPWATCH)
            }
            SW_RESET -> {
                StopwatchState(app).reset()
                WidgetUpdater.update(app, WidgetKind.STOPWATCH)
            }

            TORCH -> {
                SystemActions.setTorch(app, !SystemActions.torchOn)
                WidgetUpdater.updateToggles(app)
            }
            RINGER -> {
                val vc = app.container.volume
                val next = when (vc.ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
                // Silent needs Do Not Disturb access on many devices; fall back to normal if refused.
                if (!vc.setRingerMode(next) && next == AudioManager.RINGER_MODE_SILENT) vc.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
                WidgetUpdater.updateToggles(app)
            }
            DND -> {
                val nm = app.systemService<NotificationManager>()
                if (nm.isNotificationPolicyAccessGranted) {
                    val on = nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
                    runCatching {
                        nm.setInterruptionFilter(
                            if (on) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                        )
                    }
                }
                WidgetUpdater.updateToggles(app)
            }
            ISLAND -> app.container.settings.update { it.copy(enabled = !it.enabled) }

            REFRESH -> WidgetUpdater.updateAll(app)
        }
    }

    /** Prefers the tracked session; otherwise sends a media key so any player responds. */
    private inline fun mediaKey(context: Context, keyCode: Int, viaSession: (com.smartisland.app.island.MediaCommands) -> Unit) {
        val commands = IslandEngine.mediaCommands
        if (commands != null && IslandEngine.mediaState.value != null) {
            viaSession(commands)
            return
        }
        val audio = context.systemService<AudioManager>()
        val t = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, keyCode, 0))
    }

    companion object {
        private const val PREFIX = "com.smartisland.app.widget."
        const val POWER = PREFIX + "POWER"
        const val LOCK = PREFIX + "LOCK"
        const val SCREENSHOT = PREFIX + "SCREENSHOT"
        const val SHADE = PREFIX + "SHADE"
        const val VOL_UP = PREFIX + "VOL_UP"
        const val VOL_DOWN = PREFIX + "VOL_DOWN"
        const val MUTE = PREFIX + "MUTE"
        const val MEDIA_PLAY = PREFIX + "MEDIA_PLAY"
        const val MEDIA_NEXT = PREFIX + "MEDIA_NEXT"
        const val MEDIA_PREV = PREFIX + "MEDIA_PREV"
        const val MEDIA_OPEN = PREFIX + "MEDIA_OPEN"
        const val SW_TOGGLE = PREFIX + "SW_TOGGLE"
        const val SW_RESET = PREFIX + "SW_RESET"
        const val TORCH = PREFIX + "TORCH"
        const val RINGER = PREFIX + "RINGER"
        const val DND = PREFIX + "DND"
        const val ISLAND = PREFIX + "ISLAND"
        const val REFRESH = PREFIX + "REFRESH"

        fun pending(context: Context, action: String): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context, action.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
