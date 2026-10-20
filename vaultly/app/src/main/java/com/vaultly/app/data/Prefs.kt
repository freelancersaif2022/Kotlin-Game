package com.vaultly.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small app settings store backed by SharedPreferences, exposed as StateFlows for Compose. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("vaultly", Context.MODE_PRIVATE)

    private val _onboarded = MutableStateFlow(sp.getBoolean(K_ONBOARDED, false))
    val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()

    private val _pro = MutableStateFlow(sp.getBoolean(K_PRO, false))
    val pro: StateFlow<Boolean> = _pro.asStateFlow()

    private val _removeAds = MutableStateFlow(sp.getBoolean(K_REMOVE_ADS, false))
    val removeAds: StateFlow<Boolean> = _removeAds.asStateFlow()

    private val _bonusSlots = MutableStateFlow(sp.getInt(K_BONUS_SLOTS, 0))
    val bonusSlots: StateFlow<Int> = _bonusSlots.asStateFlow()

    private val _currency = MutableStateFlow(sp.getString(K_CURRENCY, null) ?: defaultCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _remindReturn = MutableStateFlow(sp.getBoolean(K_REMIND_RETURN, true))
    val remindReturn: StateFlow<Boolean> = _remindReturn.asStateFlow()

    private val _remindWarranty = MutableStateFlow(sp.getBoolean(K_REMIND_WARRANTY, true))
    val remindWarranty: StateFlow<Boolean> = _remindWarranty.asStateFlow()

    val isPro: Boolean get() = _pro.value
    val adsEnabled: Boolean get() = !_pro.value && !_removeAds.value

    fun setOnboarded() = put(K_ONBOARDED, true).also { _onboarded.value = true }
    fun setPro(value: Boolean) = put(K_PRO, value).also { _pro.value = value }
    fun setRemoveAds(value: Boolean) = put(K_REMOVE_ADS, value).also { _removeAds.value = value }
    fun setCurrency(code: String) { sp.edit().putString(K_CURRENCY, code).apply(); _currency.value = code }
    fun setRemindReturn(value: Boolean) = put(K_REMIND_RETURN, value).also { _remindReturn.value = value }
    fun setRemindWarranty(value: Boolean) = put(K_REMIND_WARRANTY, value).also { _remindWarranty.value = value }

    fun addBonusSlots(count: Int) {
        val next = _bonusSlots.value + count
        sp.edit().putInt(K_BONUS_SLOTS, next).apply()
        _bonusSlots.value = next
    }

    /** Number of active items a free user can keep. */
    val freeLimit: Int get() = FREE_ITEMS + _bonusSlots.value

    var savesCount: Int
        get() = sp.getInt(K_SAVES, 0)
        set(value) = sp.edit().putInt(K_SAVES, value).apply()

    var lastInterstitialAt: Long
        get() = sp.getLong(K_LAST_INTERSTITIAL, 0L)
        set(value) = sp.edit().putLong(K_LAST_INTERSTITIAL, value).apply()

    /** Keys of reminders already shown, so a deadline never notifies twice for the same step. */
    fun wasNotified(key: String): Boolean = sp.getStringSet(K_NOTIFIED, emptySet())!!.contains(key)

    fun markNotified(key: String) {
        val set = sp.getStringSet(K_NOTIFIED, emptySet())!!.toMutableSet()
        set += key
        // Keep the set bounded; old keys refer to past deadlines anyway.
        val trimmed = if (set.size > 600) set.toList().takeLast(400).toSet() else set
        sp.edit().putStringSet(K_NOTIFIED, trimmed).apply()
    }

    private fun put(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()

    private fun defaultCurrency(): String =
        runCatching { java.util.Currency.getInstance(java.util.Locale.getDefault()).currencyCode }.getOrNull() ?: "USD"

    companion object {
        const val FREE_ITEMS = 15
        const val REWARD_SLOTS = 5
        private const val K_ONBOARDED = "onboarded"
        private const val K_PRO = "pro"
        private const val K_REMOVE_ADS = "remove_ads"
        private const val K_BONUS_SLOTS = "bonus_slots"
        private const val K_CURRENCY = "currency"
        private const val K_REMIND_RETURN = "remind_return"
        private const val K_REMIND_WARRANTY = "remind_warranty"
        private const val K_SAVES = "saves"
        private const val K_LAST_INTERSTITIAL = "last_interstitial"
        private const val K_NOTIFIED = "notified"
    }
}
