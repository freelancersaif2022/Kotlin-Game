package com.calorielens.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.Gender
import com.calorielens.app.domain.Goal
import com.calorielens.app.domain.ThemeMode
import com.calorielens.app.domain.UserProfile
import com.calorielens.app.domain.WeightUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calorielens_prefs")

enum class PremiumPlan(val label: String) { MONTHLY("Monthly"), YEARLY("Yearly"), LIFETIME("Lifetime") }

data class AppSettings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isPremium: Boolean = false,
    val premiumPlan: PremiumPlan? = null,
    val premiumSince: Long = 0L,
)

data class ScanQuota(
    val usedToday: Int,
    val bonusToday: Int,
) {
    val limit: Int get() = FREE_DAILY_SCANS + bonusToday
    val remaining: Int get() = (limit - usedToday).coerceAtLeast(0)

    companion object {
        const val FREE_DAILY_SCANS = 5
        const val REWARD_BONUS_SCANS = 3
    }
}

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val store = context.dataStore

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val THEME = stringPreferencesKey("theme_mode")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val PREMIUM = booleanPreferencesKey("premium")
        val PREMIUM_PLAN = stringPreferencesKey("premium_plan")
        val PREMIUM_SINCE = longPreferencesKey("premium_since")

        val NAME = stringPreferencesKey("name")
        val GENDER = stringPreferencesKey("gender")
        val AGE = intPreferencesKey("age")
        val HEIGHT = intPreferencesKey("height_cm")
        val WEIGHT = floatPreferencesKey("weight_kg")
        val ACTIVITY = stringPreferencesKey("activity")
        val GOAL = stringPreferencesKey("goal")

        val SCAN_DAY = stringPreferencesKey("scan_day")
        val SCANS_USED = intPreferencesKey("scans_used")
        val SCAN_BONUS = intPreferencesKey("scan_bonus")
        val TOTAL_SCANS = intPreferencesKey("total_scans")
    }

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<AppSettings> = data.map { p ->
        AppSettings(
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            themeMode = enumOr(p[Keys.THEME], ThemeMode.DARK),
            weightUnit = enumOr(p[Keys.WEIGHT_UNIT], WeightUnit.KG),
            isPremium = p[Keys.PREMIUM] ?: false,
            premiumPlan = p[Keys.PREMIUM_PLAN]?.let { runCatching { PremiumPlan.valueOf(it) }.getOrNull() },
            premiumSince = p[Keys.PREMIUM_SINCE] ?: 0L,
        )
    }.distinctUntilChanged()

    val profile: Flow<UserProfile> = data.map { p ->
        val d = UserProfile()
        UserProfile(
            name = p[Keys.NAME] ?: d.name,
            gender = enumOr(p[Keys.GENDER], d.gender),
            age = p[Keys.AGE] ?: d.age,
            heightCm = p[Keys.HEIGHT] ?: d.heightCm,
            weightKg = p[Keys.WEIGHT] ?: d.weightKg,
            activity = enumOr(p[Keys.ACTIVITY], d.activity),
            goal = enumOr(p[Keys.GOAL], d.goal),
        )
    }.distinctUntilChanged()

    val scanQuota: Flow<ScanQuota> = data.map { p ->
        if (p[Keys.SCAN_DAY] == today()) {
            ScanQuota(usedToday = p[Keys.SCANS_USED] ?: 0, bonusToday = p[Keys.SCAN_BONUS] ?: 0)
        } else {
            ScanQuota(0, 0)
        }
    }.distinctUntilChanged()

    val totalScans: Flow<Int> = data.map { it[Keys.TOTAL_SCANS] ?: 0 }.distinctUntilChanged()

    suspend fun currentSettings(): AppSettings = settings.first()
    suspend fun currentQuota(): ScanQuota = scanQuota.first()

    suspend fun saveProfile(profile: UserProfile) {
        store.edit { p ->
            p[Keys.NAME] = profile.name.trim()
            p[Keys.GENDER] = profile.gender.name
            p[Keys.AGE] = profile.age
            p[Keys.HEIGHT] = profile.heightCm
            p[Keys.WEIGHT] = profile.weightKg
            p[Keys.ACTIVITY] = profile.activity.name
            p[Keys.GOAL] = profile.goal.name
        }
    }

    suspend fun completeOnboarding(profile: UserProfile) {
        saveProfile(profile)
        store.edit { it[Keys.ONBOARDING] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.THEME] = mode.name }
    suspend fun setWeightUnit(unit: WeightUnit) = store.edit { it[Keys.WEIGHT_UNIT] = unit.name }

    suspend fun setPremium(plan: PremiumPlan?) = store.edit { p ->
        if (plan == null) {
            p[Keys.PREMIUM] = false
            p.remove(Keys.PREMIUM_PLAN)
            p.remove(Keys.PREMIUM_SINCE)
        } else {
            p[Keys.PREMIUM] = true
            p[Keys.PREMIUM_PLAN] = plan.name
            p[Keys.PREMIUM_SINCE] = System.currentTimeMillis()
        }
    }

    /** Records one successful scan against today's free quota. */
    suspend fun recordScan() = store.edit { p ->
        val today = today()
        if (p[Keys.SCAN_DAY] != today) {
            p[Keys.SCAN_DAY] = today
            p[Keys.SCANS_USED] = 0
            p[Keys.SCAN_BONUS] = 0
        }
        p[Keys.SCANS_USED] = (p[Keys.SCANS_USED] ?: 0) + 1
        p[Keys.TOTAL_SCANS] = (p[Keys.TOTAL_SCANS] ?: 0) + 1
    }

    suspend fun addBonusScans(count: Int) = store.edit { p ->
        val today = today()
        if (p[Keys.SCAN_DAY] != today) {
            p[Keys.SCAN_DAY] = today
            p[Keys.SCANS_USED] = 0
            p[Keys.SCAN_BONUS] = 0
        }
        p[Keys.SCAN_BONUS] = (p[Keys.SCAN_BONUS] ?: 0) + count
    }

    /** Clears profile, history counters and premium. Theme is kept. */
    suspend fun resetAll() = store.edit { p ->
        val theme = p[Keys.THEME]
        p.clear()
        if (theme != null) p[Keys.THEME] = theme
    }

    private fun today(): String = LocalDate.now().toString()

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default
}
