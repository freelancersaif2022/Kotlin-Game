package com.pdfmaster.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SortOrder { DATE, NAME, SIZE }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pdfmaster_prefs")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val PAYWALL_SHOWN = booleanPreferencesKey("paywall_shown")
        val THEME = stringPreferencesKey("theme")
        val PURCHASED = booleanPreferencesKey("premium_purchased")
        val PASS_UNTIL = longPreferencesKey("reward_pass_until")
        val USAGE_DAY = stringPreferencesKey("usage_day")
        val USAGE_COUNT = intPreferencesKey("usage_count")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val NIGHT_READING = booleanPreferencesKey("night_reading")
        val SORT = stringPreferencesKey("sort")
        val OPEN_COUNT = intPreferencesKey("open_count")
        val RATE_PROMPTED = booleanPreferencesKey("rate_prompted")
    }

    private val data = context.dataStore.data

    val onboardingDone: Flow<Boolean> = data.map { it[Keys.ONBOARDING_DONE] ?: false }.distinctUntilChanged()
    val paywallShown: Flow<Boolean> = data.map { it[Keys.PAYWALL_SHOWN] ?: false }.distinctUntilChanged()
    val themeMode: Flow<ThemeMode> = data.map { p ->
        p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }.distinctUntilChanged()
    val purchased: Flow<Boolean> = data.map { it[Keys.PURCHASED] ?: false }.distinctUntilChanged()
    val passUntil: Flow<Long> = data.map { it[Keys.PASS_UNTIL] ?: 0L }.distinctUntilChanged()
    val keepScreenOn: Flow<Boolean> = data.map { it[Keys.KEEP_SCREEN_ON] ?: true }.distinctUntilChanged()
    val nightReading: Flow<Boolean> = data.map { it[Keys.NIGHT_READING] ?: false }.distinctUntilChanged()
    val sortOrder: Flow<SortOrder> = data.map { p ->
        p[Keys.SORT]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.DATE
    }.distinctUntilChanged()
    val usageToday: Flow<Int> = data.map { p ->
        if (p[Keys.USAGE_DAY] == today()) p[Keys.USAGE_COUNT] ?: 0 else 0
    }.distinctUntilChanged()

    suspend fun setOnboardingDone() = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    suspend fun setPaywallShown() = context.dataStore.edit { it[Keys.PAYWALL_SHOWN] = true }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setPurchased(value: Boolean) = context.dataStore.edit { it[Keys.PURCHASED] = value }
    suspend fun setPassUntil(value: Long) = context.dataStore.edit { it[Keys.PASS_UNTIL] = value }
    suspend fun setKeepScreenOn(value: Boolean) = context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = value }
    suspend fun setNightReading(value: Boolean) = context.dataStore.edit { it[Keys.NIGHT_READING] = value }
    suspend fun setSortOrder(value: SortOrder) = context.dataStore.edit { it[Keys.SORT] = value.name }

    suspend fun currentPassUntil(): Long = passUntil.first()

    suspend fun incrementUsage() = context.dataStore.edit { p ->
        val day = today()
        val count = if (p[Keys.USAGE_DAY] == day) p[Keys.USAGE_COUNT] ?: 0 else 0
        p[Keys.USAGE_DAY] = day
        p[Keys.USAGE_COUNT] = count + 1
    }

    /** Returns how many times the user has opened a document, used to time the rating prompt. */
    suspend fun incrementOpenCount(): Int {
        var result = 0
        context.dataStore.edit { p ->
            result = (p[Keys.OPEN_COUNT] ?: 0) + 1
            p[Keys.OPEN_COUNT] = result
        }
        return result
    }

    suspend fun rateDialogShown(): Boolean = data.first()[Keys.RATE_PROMPTED] ?: false
    suspend fun setRateDialogShown() = context.dataStore.edit { it[Keys.RATE_PROMPTED] = true }

    private fun today(): String = LocalDate.now().toString()
}
