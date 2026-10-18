package com.smartisland.app.widget

import android.content.Context
import android.os.SystemClock

/** Stopwatch state shared by every stopwatch widget; the Chronometer view does the live ticking. */
class StopwatchState(context: Context) {

    private val prefs = context.getSharedPreferences("stopwatch", Context.MODE_PRIVATE)

    val running: Boolean get() = prefs.getBoolean(K_RUNNING, false)

    /** Elapsed-realtime base for a running Chronometer. */
    val base: Long get() = prefs.getLong(K_BASE, SystemClock.elapsedRealtime())

    /** Accumulated time while paused. */
    val pausedElapsed: Long get() = prefs.getLong(K_PAUSED, 0L)

    fun toggle() {
        val now = SystemClock.elapsedRealtime()
        if (running) {
            prefs.edit().putBoolean(K_RUNNING, false).putLong(K_PAUSED, now - base).apply()
        } else {
            prefs.edit().putBoolean(K_RUNNING, true).putLong(K_BASE, now - pausedElapsed).apply()
        }
    }

    fun reset() {
        prefs.edit().putBoolean(K_RUNNING, false).putLong(K_PAUSED, 0L).putLong(K_BASE, SystemClock.elapsedRealtime()).apply()
    }

    /** Base to display: running uses the stored base, paused shows the frozen elapsed time. */
    fun displayBase(): Long = if (running) base else SystemClock.elapsedRealtime() - pausedElapsed

    private companion object {
        const val K_RUNNING = "running"
        const val K_BASE = "base"
        const val K_PAUSED = "paused"
    }
}
