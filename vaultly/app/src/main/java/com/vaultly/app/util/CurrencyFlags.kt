package com.vaultly.app.util

import java.util.Currency
import java.util.Locale

/** Current currencies with the country (or region) whose flag represents them. */
object CurrencyFlags {
    /** Fixed choices where several countries share a currency or the locale table picks an odd one. */
    private val PREFERRED = mapOf(
        "USD" to "US", "EUR" to "EU", "GBP" to "GB", "AUD" to "AU", "NZD" to "NZ", "CHF" to "CH",
        "INR" to "IN", "XAF" to "CM", "XOF" to "SN", "XCD" to "AG", "XPF" to "PF", "ANG" to "CW",
        "DKK" to "DK", "NOK" to "NO", "ILS" to "IL", "MAD" to "MA", "ZAR" to "ZA", "CAD" to "CA",
        "HKD" to "HK", "SGD" to "SG", "CNY" to "CN", "JPY" to "JP", "RUB" to "RU", "BTN" to "BT",
    )

    private val byCurrency: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        for (locale in Locale.getAvailableLocales()) {
            val country = locale.country
            if (country.length != 2 || !country.all { it in 'A'..'Z' }) continue
            val currency = runCatching { Currency.getInstance(locale) }.getOrNull() ?: continue
            map.putIfAbsent(currency.currencyCode, country)
        }
        map.putAll(PREFERRED)
        map
    }

    /** Every currency in use today, sorted by code. */
    val all: List<Currency> by lazy {
        byCurrency.keys.mapNotNull { runCatching { Currency.getInstance(it) }.getOrNull() }.sortedBy { it.currencyCode }
    }

    fun country(code: String): String? = byCurrency[code]

    /** Emoji flag for a currency, e.g. "🇺🇸" for USD and "🇪🇺" for EUR. */
    fun flag(code: String): String? = country(code)?.let { cc ->
        cc.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    }
}
