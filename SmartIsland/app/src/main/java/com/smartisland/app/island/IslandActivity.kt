package com.smartisland.app.island

import android.app.PendingIntent
import android.graphics.Bitmap
import android.os.SystemClock

/** What the island is currently showing. */
sealed interface IslandActivity {

    data object Idle : IslandActivity

    data class Media(
        val title: String,
        val artist: String,
        val art: Bitmap?,
        val accent: Int,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val speed: Float,
        /** [SystemClock.elapsedRealtime] when [positionMs] was sampled. */
        val sampledAt: Long,
        val packageName: String,
    ) : IslandActivity {
        fun livePosition(now: Long = SystemClock.elapsedRealtime()): Long {
            if (!isPlaying) return positionMs
            val p = positionMs + ((now - sampledAt) * speed).toLong()
            return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
        }
    }

    data class Call(
        val key: String,
        val caller: String,
        val isIncoming: Boolean,
        /** Wall-clock start of the call when the dialer exposes it, else 0. */
        val startedAt: Long,
        val avatar: Bitmap?,
        val answer: PendingIntent?,
        val decline: PendingIntent?,
        val hangUp: PendingIntent?,
        val open: PendingIntent?,
        val isDemo: Boolean = false,
    ) : IslandActivity

    data class Charging(
        val level: Int,
        val isCharging: Boolean,
        val isFast: Boolean,
        val watts: Float,
    ) : IslandActivity

    data class LowBattery(val level: Int) : IslandActivity

    data class Volume(val percent: Int, val muted: Boolean) : IslandActivity

    data class Ringer(val mode: Int) : IslandActivity

    data class Notice(
        val key: String,
        val appName: String,
        val title: String,
        val text: String,
        val icon: Bitmap?,
        val open: PendingIntent?,
    ) : IslandActivity

    data class Info(val label: String, val detail: String) : IslandActivity
}
