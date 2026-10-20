package com.vaultly.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

object Format {
    private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val shortFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    fun date(date: LocalDate): String = date.format(dateFmt)
    fun date(epochDay: Long): String = date(LocalDate.ofEpochDay(epochDay))
    fun shortDate(date: LocalDate): String =
        if (date.year == LocalDate.now().year) date.format(shortFmt) else date.format(dateFmt)

    fun money(amount: Double, currency: String): String = runCatching {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            this.currency = Currency.getInstance(currency)
            maximumFractionDigits = if (amount % 1.0 == 0.0 && amount >= 100) 0 else 2
        }.format(amount)
    }.getOrElse { "%s %.2f".format(currency, amount) }

    fun symbol(currency: String): String =
        runCatching { Currency.getInstance(currency).getSymbol(Locale.getDefault()) }.getOrDefault(currency)

    fun returnWindow(days: Int?): String = when (days) {
        null, 0 -> "No returns"
        else -> "$days days"
    }

    fun warranty(months: Int?): String = when {
        months == null || months == 0 -> "No warranty"
        months % 12 == 0 -> if (months == 12) "1 year" else "${months / 12} years"
        else -> if (months == 1) "1 month" else "$months months"
    }

    val currencies = listOf(
        "USD", "EUR", "GBP", "CAD", "AUD", "INR", "BDT", "PKR", "AED", "SAR",
        "JPY", "CNY", "SGD", "MYR", "IDR", "PHP", "TRY", "BRL", "MXN", "ZAR", "NGN", "CHF", "SEK",
    )
}
