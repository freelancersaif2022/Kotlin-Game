package com.pulse.messenger.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Format {
    private val time = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val weekday = SimpleDateFormat("EEEE", Locale.getDefault())
    private val shortDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    private val longDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val monthDay = SimpleDateFormat("MMMM d", Locale.getDefault())

    private fun dayIndex(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun daysAgo(ts: Long): Int {
        val a = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val b = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return ((b.timeInMillis - a.timeInMillis) / 86_400_000L).toInt()
    }

    fun sameDay(a: Long, b: Long) = dayIndex(a) == dayIndex(b)

    fun clock(ts: Long): String = time.format(Date(ts))

    /** Chat list style: time today, "Yesterday", weekday within a week, then a date. */
    fun chatListTime(ts: Long): String = when (val d = daysAgo(ts)) {
        0 -> clock(ts)
        1 -> "Yesterday"
        in 2..6 -> weekday.format(Date(ts))
        else -> shortDate.format(Date(ts))
    }

    /** Separator chip inside a conversation. */
    fun daySeparator(ts: Long): String = when (val d = daysAgo(ts)) {
        0 -> "Today"
        1 -> "Yesterday"
        in 2..6 -> weekday.format(Date(ts))
        else -> longDate.format(Date(ts))
    }

    fun relative(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} minutes ago"
            daysAgo(ts) == 0 -> "Today, ${clock(ts)}"
            daysAgo(ts) == 1 -> "Yesterday, ${clock(ts)}"
            else -> "${monthDay.format(Date(ts))}, ${clock(ts)}"
        }
    }

    fun lastSeen(ts: Long): String = when (daysAgo(ts)) {
        0 -> "last seen today at ${clock(ts)}"
        1 -> "last seen yesterday at ${clock(ts)}"
        else -> "last seen ${shortDate.format(Date(ts))}"
    }

    fun date(ts: Long): String = longDate.format(Date(ts))

    fun duration(sec: Int): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun callDuration(sec: Int): String = when {
        sec <= 0 -> ""
        sec < 60 -> "$sec sec"
        sec < 3600 -> "${sec / 60} min"
        else -> "${sec / 3600} hr ${(sec % 3600) / 60} min"
    }

    fun compact(n: Int): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f).replace(".0M", "M")
        n >= 1_000 -> "%.1fK".format(n / 1_000f).replace(".0K", "K")
        else -> n.toString()
    }
}
