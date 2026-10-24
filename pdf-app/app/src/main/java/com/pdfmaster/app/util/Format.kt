package com.pdfmaster.app.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object Format {
    fun size(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val units = arrayOf("B", "KB", "MB", "GB")
        val group = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(group)
        return if (group <= 1) String.format(Locale.US, "%.0f %s", value, units[group])
        else String.format(Locale.US, "%.1f %s", value, units[group])
    }

    fun relativeDate(millis: Long): String {
        if (millis <= 0) return ""
        val now = System.currentTimeMillis()
        return when {
            now - millis < DateUtils.MINUTE_IN_MILLIS -> "Just now"
            DateUtils.isToday(millis) -> "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
            DateUtils.isToday(millis + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
            now - millis < 7 * DateUtils.DAY_IN_MILLIS -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(millis))
            else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    fun pages(count: Int): String = when {
        count < 0 -> "Protected"
        count == 1 -> "1 page"
        else -> "$count pages"
    }

    fun countdown(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    fun baseName(name: String): String = name.substringBeforeLast('.', name)
}
