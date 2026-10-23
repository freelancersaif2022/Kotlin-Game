package com.voxcast.mic.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "voxcast_settings")

class SettingsRepository(private val context: Context, scope: CoroutineScope) {

    private object Keys {
        val deviceName = stringPreferencesKey("device_name")
        val noiseMode = stringPreferencesKey("noise_mode")
        val echoCancel = booleanPreferencesKey("echo_cancel")
        val autoGain = booleanPreferencesKey("auto_gain")
        val voiceEnhance = booleanPreferencesKey("voice_enhance")
        val micGainDb = floatPreferencesKey("mic_gain_db")
        val quality = stringPreferencesKey("quality")
        val pushToTalk = booleanPreferencesKey("push_to_talk")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val haptics = booleanPreferencesKey("haptics")
        val speakerBoost = floatPreferencesKey("speaker_boost")
        val premiumActive = booleanPreferencesKey("premium_active")
        val premiumAutoRenew = booleanPreferencesKey("premium_auto_renew")
        val premiumRenewsAt = longPreferencesKey("premium_renews_at")
        val premiumPurchasedAt = longPreferencesKey("premium_purchased_at")
    }

    val defaultDeviceName: String by lazy {
        runCatching { Settings.Global.getString(context.contentResolver, "device_name") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    val settings: StateFlow<AppSettings> = context.dataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings(deviceName = defaultDeviceName))

    private fun Preferences.toSettings(): AppSettings {
        val premium = PremiumState(
            active = this[Keys.premiumActive] ?: false,
            autoRenew = this[Keys.premiumAutoRenew] ?: false,
            renewsAt = this[Keys.premiumRenewsAt] ?: 0L,
            purchasedAt = this[Keys.premiumPurchasedAt] ?: 0L,
        )
        val quality = runCatching { AudioQuality.valueOf(this[Keys.quality] ?: "") }
            .getOrDefault(AudioQuality.HD)
            .let { if (it.premium && !premium.isEntitled) AudioQuality.HD else it }
        return AppSettings(
            deviceName = this[Keys.deviceName]?.takeIf { it.isNotBlank() } ?: defaultDeviceName,
            noiseMode = runCatching { NoiseMode.valueOf(this[Keys.noiseMode] ?: "") }.getOrDefault(NoiseMode.ULTRA),
            echoCancel = this[Keys.echoCancel] ?: true,
            autoGain = this[Keys.autoGain] ?: false,
            voiceEnhance = this[Keys.voiceEnhance] ?: true,
            micGainDb = this[Keys.micGainDb] ?: 0f,
            quality = quality,
            pushToTalk = this[Keys.pushToTalk] ?: false,
            keepScreenOn = this[Keys.keepScreenOn] ?: true,
            haptics = this[Keys.haptics] ?: true,
            speakerBoost = (this[Keys.speakerBoost] ?: 1f)
                .coerceIn(0f, if (premium.isEntitled) MAX_BOOST_PREMIUM else MAX_BOOST_FREE),
            premium = premium,
        )
    }

    suspend fun setDeviceName(v: String) = context.dataStore.edit { it[Keys.deviceName] = v.trim().take(32) }
    suspend fun setNoiseMode(v: NoiseMode) = context.dataStore.edit { it[Keys.noiseMode] = v.name }
    suspend fun setEchoCancel(v: Boolean) = context.dataStore.edit { it[Keys.echoCancel] = v }
    suspend fun setAutoGain(v: Boolean) = context.dataStore.edit { it[Keys.autoGain] = v }
    suspend fun setVoiceEnhance(v: Boolean) = context.dataStore.edit { it[Keys.voiceEnhance] = v }
    suspend fun setMicGainDb(v: Float) = context.dataStore.edit { it[Keys.micGainDb] = v.coerceIn(-6f, 12f) }
    suspend fun setQuality(v: AudioQuality) = context.dataStore.edit { it[Keys.quality] = v.name }
    suspend fun setPushToTalk(v: Boolean) = context.dataStore.edit { it[Keys.pushToTalk] = v }
    suspend fun setKeepScreenOn(v: Boolean) = context.dataStore.edit { it[Keys.keepScreenOn] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[Keys.haptics] = v }
    suspend fun setSpeakerBoost(v: Float) = context.dataStore.edit { it[Keys.speakerBoost] = v }

    suspend fun setPremium(state: PremiumState) = context.dataStore.edit {
        it[Keys.premiumActive] = state.active
        it[Keys.premiumAutoRenew] = state.autoRenew
        it[Keys.premiumRenewsAt] = state.renewsAt
        it[Keys.premiumPurchasedAt] = state.purchasedAt
    }

    companion object {
        const val MAX_BOOST_FREE = 1.5f
        const val MAX_BOOST_PREMIUM = 2.5f
    }
}
