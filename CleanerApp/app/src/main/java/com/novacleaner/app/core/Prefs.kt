package com.novacleaner.app.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_prefs")

data class Settings(
    val onboardingDone: Boolean = false,
    val tourDone: Boolean = false,
    val premium: Boolean = false,
    val themeMode: Int = 0,
    val launchCount: Int = 0,
    val lastPaywallAt: Long = 0L,
    val dataLimitMb: Long = 0L,
    val remindersEnabled: Boolean = true,
    val speedMonitorEnabled: Boolean = false,
    val lastCleanAt: Long = 0L,
    val totalCleanedBytes: Long = 0L,
)

class Prefs(private val context: Context) {

    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val tour = booleanPreferencesKey("tour_done")
        val premium = booleanPreferencesKey("premium")
        val theme = intPreferencesKey("theme_mode")
        val launches = intPreferencesKey("launch_count")
        val lastPaywall = longPreferencesKey("last_paywall_at")
        val dataLimit = longPreferencesKey("data_limit_mb")
        val reminders = booleanPreferencesKey("reminders_enabled")
        val speedMonitor = booleanPreferencesKey("speed_monitor_enabled")
        val lastClean = longPreferencesKey("last_clean_at")
        val totalCleaned = longPreferencesKey("total_cleaned_bytes")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            onboardingDone = p[Keys.onboarding] ?: false,
            tourDone = p[Keys.tour] ?: false,
            premium = p[Keys.premium] ?: false,
            themeMode = p[Keys.theme] ?: 0,
            launchCount = p[Keys.launches] ?: 0,
            lastPaywallAt = p[Keys.lastPaywall] ?: 0L,
            dataLimitMb = p[Keys.dataLimit] ?: 0L,
            remindersEnabled = p[Keys.reminders] ?: true,
            speedMonitorEnabled = p[Keys.speedMonitor] ?: false,
            lastCleanAt = p[Keys.lastClean] ?: 0L,
            totalCleanedBytes = p[Keys.totalCleaned] ?: 0L,
        )
    }

    suspend fun snapshot(): Settings = settings.first()

    suspend fun setOnboardingDone() = context.dataStore.edit { it[Keys.onboarding] = true }
    suspend fun setTourDone() = context.dataStore.edit { it[Keys.tour] = true }
    suspend fun setPremium(value: Boolean) = context.dataStore.edit { it[Keys.premium] = value }
    suspend fun setThemeMode(mode: Int) = context.dataStore.edit { it[Keys.theme] = mode }
    suspend fun incrementLaunchCount() = context.dataStore.edit { it[Keys.launches] = (it[Keys.launches] ?: 0) + 1 }
    suspend fun setLastPaywallAt(time: Long) = context.dataStore.edit { it[Keys.lastPaywall] = time }
    suspend fun setDataLimitMb(mb: Long) = context.dataStore.edit { it[Keys.dataLimit] = mb }
    suspend fun setRemindersEnabled(value: Boolean) = context.dataStore.edit { it[Keys.reminders] = value }
    suspend fun setSpeedMonitorEnabled(value: Boolean) = context.dataStore.edit { it[Keys.speedMonitor] = value }

    suspend fun recordClean(bytes: Long) = context.dataStore.edit {
        it[Keys.lastClean] = System.currentTimeMillis()
        it[Keys.totalCleaned] = (it[Keys.totalCleaned] ?: 0L) + bytes
    }
}
