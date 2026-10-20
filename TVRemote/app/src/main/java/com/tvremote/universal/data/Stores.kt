package com.tvremote.universal.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray

class TvStore(context: Context) {
    private val prefs = context.getSharedPreferences("tvs", Context.MODE_PRIVATE)
    private val _devices = MutableStateFlow(load())
    val devices: StateFlow<List<TvDevice>> = _devices.asStateFlow()

    private fun load(): List<TvDevice> {
        val raw = prefs.getString("devices", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { TvDevice.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<TvDevice>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("devices", arr.toString()).apply()
    }

    fun get(id: String): TvDevice? = _devices.value.firstOrNull { it.id == id }

    fun findByHost(platform: TvPlatform, host: String): TvDevice? =
        _devices.value.firstOrNull { it.platform == platform && it.host == host }

    fun upsert(device: TvDevice) {
        _devices.update { list ->
            val idx = list.indexOfFirst { it.id == device.id }
            val newList = if (idx >= 0) list.toMutableList().also { it[idx] = device } else list + device
            persist(newList)
            newList
        }
    }

    fun update(id: String, transform: (TvDevice) -> TvDevice) {
        _devices.update { list ->
            val newList = list.map { if (it.id == id) transform(it) else it }
            persist(newList)
            newList
        }
    }

    fun remove(id: String) {
        _devices.update { list ->
            val newList = list.filterNot { it.id == id }
            persist(newList)
            newList
        }
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", ThemeMode.LIGHT.name)!!) }.getOrDefault(ThemeMode.LIGHT),
    )
    val onboardingDone = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val haptics = MutableStateFlow(prefs.getBoolean("haptics", true))
    val keepScreenOn = MutableStateFlow(prefs.getBoolean("keep_screen_on", true))
    val volumeKeys = MutableStateFlow(prefs.getBoolean("volume_keys", true))
    val lastDeviceId = MutableStateFlow(prefs.getString("last_device", null))

    // Premium state cached so ads never flash before Google Play answers.
    val purchasedCache = MutableStateFlow(prefs.getBoolean("purchased", false))
    val rewardProgress = MutableStateFlow(prefs.getInt("reward_progress", 0))
    val rewardUntil = MutableStateFlow(prefs.getLong("reward_until", 0L))

    fun setTheme(mode: ThemeMode) { themeMode.value = mode; prefs.edit().putString("theme", mode.name).apply() }
    fun setOnboardingDone() { onboardingDone.value = true; prefs.edit().putBoolean("onboarding_done", true).apply() }
    fun setHaptics(v: Boolean) { haptics.value = v; prefs.edit().putBoolean("haptics", v).apply() }
    fun setKeepScreenOn(v: Boolean) { keepScreenOn.value = v; prefs.edit().putBoolean("keep_screen_on", v).apply() }
    fun setVolumeKeys(v: Boolean) { volumeKeys.value = v; prefs.edit().putBoolean("volume_keys", v).apply() }
    fun setLastDevice(id: String?) { lastDeviceId.value = id; prefs.edit().putString("last_device", id).apply() }
    fun setPurchased(v: Boolean) { purchasedCache.value = v; prefs.edit().putBoolean("purchased", v).apply() }

    fun setReward(progress: Int, until: Long) {
        rewardProgress.value = progress
        rewardUntil.value = until
        prefs.edit().putInt("reward_progress", progress).putLong("reward_until", until).apply()
    }
}
