package com.zyropay.app.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Fmt {
    private val symbols = DecimalFormatSymbols(Locale.US)
    private val money = DecimalFormat("#,##0.00", symbols)
    private val cache = HashMap<Int, DecimalFormat>()

    private fun pattern(decimals: Int): DecimalFormat = cache.getOrPut(decimals) {
        DecimalFormat(if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(2.coerceAtMost(decimals)) + "#".repeat((decimals - 2).coerceAtLeast(0)), symbols)
    }

    fun money(v: Double): String = money.format(v)

    /** Fiat amount in user's display currency, e.g. "$1,204.50". */
    fun fiat(usd: Double, currency: String = "USD", signed: Boolean = false): String {
        val f = MockData.fiat(currency)
        val v = usd * f.rate
        val sign = when {
            signed && v > 0.0049 -> "+"
            v < -0.0049 -> "-"
            else -> ""
        }
        return sign + f.symbol + money.format(abs(v))
    }

    fun coin(amount: Double, symbol: String, signed: Boolean = false, withSymbol: Boolean = true): String {
        val decimals = MockData.coin(symbol).decimals
        val sign = when {
            signed && amount > 0 -> "+"
            amount < 0 -> "-"
            else -> ""
        }
        return sign + pattern(decimals).format(abs(amount)) + if (withSymbol) " $symbol" else ""
    }

    fun plain(amount: Double, decimals: Int): String = pattern(decimals).format(amount)

    fun price(usd: Double): String = when {
        usd >= 1000 -> "$" + money.format(usd)
        usd >= 1 -> "$" + DecimalFormat("#,##0.00##", symbols).format(usd)
        else -> "$" + DecimalFormat("0.0000##", symbols).format(usd)
    }

    fun percent(v: Double, signed: Boolean = true): String =
        (if (signed && v >= 0) "+" else "") + DecimalFormat("0.00", symbols).format(v) + "%"

    fun compact(v: Double): String = when {
        v >= 1_000_000_000 -> DecimalFormat("0.##", symbols).format(v / 1_000_000_000) + "B"
        v >= 1_000_000 -> DecimalFormat("0.##", symbols).format(v / 1_000_000) + "M"
        v >= 10_000 -> DecimalFormat("0.#", symbols).format(v / 1_000) + "K"
        else -> money.format(v)
    }

    private val time = SimpleDateFormat("HH:mm", Locale.US)
    private val dayMonth = SimpleDateFormat("dd MMM", Locale.US)
    private val full = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US)
    private val dayFull = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US)
    private val monthYear = SimpleDateFormat("MMMM yyyy", Locale.US)

    fun time(ts: Long): String = time.format(Date(ts))
    fun dateTime(ts: Long): String = full.format(Date(ts))
    fun monthYear(ts: Long): String = monthYear.format(Date(ts))

    fun shortDate(ts: Long): String {
        val now = System.currentTimeMillis()
        return when {
            isSameDay(ts, now) -> "Today, " + time(ts)
            isSameDay(ts, now - DAY) -> "Yesterday, " + time(ts)
            else -> dayMonth.format(Date(ts)) + ", " + time(ts)
        }
    }

    fun dayHeader(ts: Long): String {
        val now = System.currentTimeMillis()
        return when {
            isSameDay(ts, now) -> "Today"
            isSameDay(ts, now - DAY) -> "Yesterday"
            else -> dayFull.format(Date(ts))
        }
    }

    fun relative(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < DAY -> "${diff / 3_600_000}h ago"
            diff < 7 * DAY -> "${diff / DAY}d ago"
            else -> dayMonth.format(Date(ts))
        }
    }

    fun dayKey(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return c.timeInMillis
    }

    fun isSameDay(a: Long, b: Long) = dayKey(a) == dayKey(b)

    fun shortAddress(a: String): String = if (a.length > 16) a.take(8) + "…" + a.takeLast(6) else a

    const val DAY = 86_400_000L
}
