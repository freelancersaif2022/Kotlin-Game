package com.novacleaner.app.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Format {

    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    /** Returns value and unit separately, e.g. ("1.24", "GB"), so headers can style them apart. */
    fun bytesParts(bytes: Long): Pair<String, String> {
        if (bytes <= 0) return "0" to "B"
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        val text = when {
            unit == 0 -> value.toLong().toString()
            value >= 100 -> String.format(Locale.US, "%.0f", value)
            value >= 10 -> String.format(Locale.US, "%.1f", value)
            else -> String.format(Locale.US, "%.2f", value)
        }
        return text to units[unit]
    }

    fun bytes(bytes: Long): String = bytesParts(bytes).let { "${it.first} ${it.second}" }

    fun speed(bytesPerSecond: Long): String = "${bytes(bytesPerSecond)}/s"

    fun date(time: Long): String =
        if (time <= 0) "—" else SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(time))

    fun dateTime(time: Long): String =
        if (time <= 0) "—" else SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(time))

    fun monthYear(time: Long): String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(time))

    fun dayShort(time: Long): String = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(time))

    fun relative(time: Long, now: Long = System.currentTimeMillis()): String {
        if (time <= 0) return "Never"
        val diff = abs(now - time)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "Yesterday"
            days < 30 -> "$days days ago"
            days < 365 -> "${days / 30} months ago"
            else -> "${days / 365} years ago"
        }
    }

    fun percent(fraction: Float): String = "${(fraction * 100).toInt().coerceIn(0, 100)}%"
}
