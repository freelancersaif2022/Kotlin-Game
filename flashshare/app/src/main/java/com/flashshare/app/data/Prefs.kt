package com.flashshare.app.data

import android.content.Context
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private val Context.dataStore by preferencesDataStore("flashshare_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Wi-Fi Direct band preference. Auto still prefers 5 GHz; the forced modes fall back only if the chip refuses. */
enum class BandPref(val label: String, val detail: String) {
    GHZ5("5 GHz (Fast)", "Forces the 5 GHz band – 2–4× faster than 2.4 GHz"),
    GHZ6("6 GHz (Ultra)", "Wi-Fi 6E/7 phones only, falls back to 5 GHz"),
    AUTO("Auto", "Let the phones pick the band (most compatible)")
}

data class AppSettings(
    val onboarded: Boolean = false,
    val profileDone: Boolean = false,
    val deviceId: String = "",
    val name: String = "",
    val avatarId: Int = 0,
    val customAvatar: Boolean = false,
    val avatarVersion: Int = 0,
    val theme: ThemeMode = ThemeMode.LIGHT,
    val saveTreeUri: String? = null,
    val band: BandPref = BandPref.GHZ5,
    val encryption: Boolean = false,
    val keepScreenOn: Boolean = true,
    val vibrate: Boolean = true,
    val sound: Boolean = true,
    val premium: Boolean = false,
    val premiumPlan: String? = null,
)

class Prefs(private val context: Context, private val scope: CoroutineScope) {

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val profileDone = booleanPreferencesKey("profile_done")
        val deviceId = stringPreferencesKey("device_id")
        val name = stringPreferencesKey("name")
        val avatarId = intPreferencesKey("avatar_id")
        val customAvatar = booleanPreferencesKey("custom_avatar")
        val avatarVersion = intPreferencesKey("avatar_version")
        val theme = stringPreferencesKey("theme")
        val saveTree = stringPreferencesKey("save_tree")
        val band = stringPreferencesKey("band")
        val encryption = booleanPreferencesKey("encryption")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val vibrate = booleanPreferencesKey("vibrate")
        val sound = booleanPreferencesKey("sound")
        val premium = booleanPreferencesKey("premium")
        val premiumPlan = stringPreferencesKey("premium_plan")
    }

    private val _state = MutableStateFlow<AppSettings?>(null)

    /** Null until the first read from disk finishes (the splash screen waits for it). */
    val state: StateFlow<AppSettings?> = _state.asStateFlow()

    /** Always-available snapshot for non-UI code. */
    val current: AppSettings get() = _state.value ?: AppSettings(deviceId = "", name = defaultDeviceName())

    init {
        scope.launch {
            context.dataStore.data.collect { p ->
                val id = p[K.deviceId]
                if (id == null) {
                    context.dataStore.edit { it[K.deviceId] = UUID.randomUUID().toString() }
                    return@collect
                }
                _state.value = AppSettings(
                    onboarded = p[K.onboarded] ?: false,
                    profileDone = p[K.profileDone] ?: false,
                    deviceId = id,
                    name = p[K.name] ?: defaultDeviceName(),
                    avatarId = p[K.avatarId] ?: 0,
                    customAvatar = p[K.customAvatar] ?: false,
                    avatarVersion = p[K.avatarVersion] ?: 0,
                    theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.LIGHT,
                    saveTreeUri = p[K.saveTree],
                    band = p[K.band]?.let { runCatching { BandPref.valueOf(it) }.getOrNull() } ?: BandPref.GHZ5,
                    encryption = p[K.encryption] ?: false,
                    keepScreenOn = p[K.keepScreenOn] ?: true,
                    vibrate = p[K.vibrate] ?: true,
                    sound = p[K.sound] ?: true,
                    premium = p[K.premium] ?: false,
                    premiumPlan = p[K.premiumPlan],
                )
            }
        }
    }

    private fun edit(block: (MutablePreferences) -> Unit) {
        scope.launch { context.dataStore.edit { block(it) } }
    }

    fun setOnboarded() = edit { it[K.onboarded] = true }
    fun saveProfile(name: String, avatarId: Int, custom: Boolean) = edit {
        it[K.name] = name.trim().ifEmpty { defaultDeviceName() }
        it[K.avatarId] = avatarId
        it[K.customAvatar] = custom
        it[K.avatarVersion] = (it[K.avatarVersion] ?: 0) + 1
        it[K.profileDone] = true
    }
    fun setTheme(t: ThemeMode) = edit { it[K.theme] = t.name }
    fun setSaveTree(uri: String?) = edit { if (uri == null) it.remove(K.saveTree) else it[K.saveTree] = uri }
    fun setBand(b: BandPref) = edit { it[K.band] = b.name }
    fun setEncryption(on: Boolean) = edit { it[K.encryption] = on }
    fun setKeepScreenOn(on: Boolean) = edit { it[K.keepScreenOn] = on }
    fun setVibrate(on: Boolean) = edit { it[K.vibrate] = on }
    fun setSound(on: Boolean) = edit { it[K.sound] = on }
    fun setPremium(on: Boolean, plan: String?) = edit {
        it[K.premium] = on
        if (plan == null) it.remove(K.premiumPlan) else it[K.premiumPlan] = plan
    }

    val avatarFile: File get() = File(context.filesDir, "avatar.jpg")

    fun avatarBytes(): ByteArray? = if (current.customAvatar && avatarFile.exists()) avatarFile.readBytes() else null

    fun defaultDeviceName(): String {
        val sys = runCatching { SystemSettings.Global.getString(context.contentResolver, "device_name") }.getOrNull()
        return sys?.takeIf { it.isNotBlank() } ?: Build.MODEL ?: "Android"
    }
}
