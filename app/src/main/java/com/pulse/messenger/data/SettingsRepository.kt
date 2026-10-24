package com.pulse.messenger.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class Settings(
    val onboarded: Boolean = false,
    val name: String = "",
    val about: String = "Hey there! I am using Pulse.",
    val phone: String = "",
    val countryCode: String = "+1",
    val avatar: Int? = null,
    val email: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: Int = 1, // 0 small, 1 medium, 2 large
    val enterIsSend: Boolean = false,
    val mediaVisibility: Boolean = true,
    val keepArchived: Boolean = true,
    val wallpaperLight: Int? = null,
    val wallpaperDark: Int? = null,
    val wallpaperDim: Float = 0f,
    val readReceipts: Boolean = true,
    val lastSeen: Int = 0, // 0 everyone, 1 contacts, 2 except, 3 nobody
    val onlineVisibility: Int = 0, // 0 everyone, 1 same as last seen
    val profilePhoto: Int = 0,
    val aboutPrivacy: Int = 0,
    val statusPrivacy: Int = 1,
    val groupsPrivacy: Int = 0,
    val callsSilenceUnknown: Boolean = false,
    val defaultTimer: Long = 0L,
    val appLock: Boolean = false,
    val appLockPin: String = "",
    val twoStepPin: String = "",
    val securityNotifications: Boolean = true,
    val conversationTones: Boolean = true,
    val msgNotifications: Boolean = true,
    val msgTone: String = "Pulse Chime",
    val vibrate: Int = 1,
    val popupNotification: Boolean = false,
    val highPriority: Boolean = true,
    val reactionNotifications: Boolean = true,
    val groupNotifications: Boolean = true,
    val groupTone: String = "Pulse Ripple",
    val callRingtone: String = "Pulse Ring",
    val callVibrate: Int = 1,
    val lessDataCalls: Boolean = false,
    val autoDownloadMobile: Set<String> = setOf("Photos"),
    val autoDownloadWifi: Set<String> = setOf("Photos", "Audio", "Videos", "Documents"),
    val uploadQuality: Int = 0, // 0 standard, 1 HD
    val language: String = "English",
    val lastBackup: Long = 0L,
    val backupFrequency: Int = 1,
    val backupIncludeVideos: Boolean = false,
    val passkey: Boolean = false,
)

class SettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("pulse_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    val current: Settings get() = _settings.value

    fun update(transform: (Settings) -> Settings) {
        _settings.update(transform)
        save(_settings.value)
    }

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            onboarded = prefs.getBoolean("onboarded", d.onboarded),
            name = prefs.getString("name", d.name) ?: d.name,
            about = prefs.getString("about", d.about) ?: d.about,
            phone = prefs.getString("phone", d.phone) ?: d.phone,
            countryCode = prefs.getString("countryCode", d.countryCode) ?: d.countryCode,
            avatar = res("avatar"),
            email = prefs.getString("email", d.email) ?: d.email,
            themeMode = ThemeMode.entries.getOrElse(prefs.getInt("themeMode", 0)) { ThemeMode.SYSTEM },
            fontSize = prefs.getInt("fontSize", d.fontSize),
            enterIsSend = prefs.getBoolean("enterIsSend", d.enterIsSend),
            mediaVisibility = prefs.getBoolean("mediaVisibility", d.mediaVisibility),
            keepArchived = prefs.getBoolean("keepArchived", d.keepArchived),
            wallpaperLight = res("wallpaperLight"),
            wallpaperDark = res("wallpaperDark"),
            wallpaperDim = prefs.getFloat("wallpaperDim", d.wallpaperDim),
            readReceipts = prefs.getBoolean("readReceipts", d.readReceipts),
            lastSeen = prefs.getInt("lastSeen", d.lastSeen),
            onlineVisibility = prefs.getInt("onlineVisibility", d.onlineVisibility),
            profilePhoto = prefs.getInt("profilePhoto", d.profilePhoto),
            aboutPrivacy = prefs.getInt("aboutPrivacy", d.aboutPrivacy),
            statusPrivacy = prefs.getInt("statusPrivacy", d.statusPrivacy),
            groupsPrivacy = prefs.getInt("groupsPrivacy", d.groupsPrivacy),
            callsSilenceUnknown = prefs.getBoolean("callsSilenceUnknown", d.callsSilenceUnknown),
            defaultTimer = prefs.getLong("defaultTimer", d.defaultTimer),
            appLock = prefs.getBoolean("appLock", d.appLock),
            appLockPin = prefs.getString("appLockPin", d.appLockPin) ?: "",
            twoStepPin = prefs.getString("twoStepPin", d.twoStepPin) ?: "",
            securityNotifications = prefs.getBoolean("securityNotifications", d.securityNotifications),
            conversationTones = prefs.getBoolean("conversationTones", d.conversationTones),
            msgNotifications = prefs.getBoolean("msgNotifications", d.msgNotifications),
            msgTone = prefs.getString("msgTone", d.msgTone) ?: d.msgTone,
            vibrate = prefs.getInt("vibrate", d.vibrate),
            popupNotification = prefs.getBoolean("popupNotification", d.popupNotification),
            highPriority = prefs.getBoolean("highPriority", d.highPriority),
            reactionNotifications = prefs.getBoolean("reactionNotifications", d.reactionNotifications),
            groupNotifications = prefs.getBoolean("groupNotifications", d.groupNotifications),
            groupTone = prefs.getString("groupTone", d.groupTone) ?: d.groupTone,
            callRingtone = prefs.getString("callRingtone", d.callRingtone) ?: d.callRingtone,
            callVibrate = prefs.getInt("callVibrate", d.callVibrate),
            lessDataCalls = prefs.getBoolean("lessDataCalls", d.lessDataCalls),
            autoDownloadMobile = prefs.getStringSet("autoDownloadMobile", d.autoDownloadMobile) ?: d.autoDownloadMobile,
            autoDownloadWifi = prefs.getStringSet("autoDownloadWifi", d.autoDownloadWifi) ?: d.autoDownloadWifi,
            uploadQuality = prefs.getInt("uploadQuality", d.uploadQuality),
            language = prefs.getString("language", d.language) ?: d.language,
            lastBackup = prefs.getLong("lastBackup", d.lastBackup),
            backupFrequency = prefs.getInt("backupFrequency", d.backupFrequency),
            backupIncludeVideos = prefs.getBoolean("backupIncludeVideos", d.backupIncludeVideos),
            passkey = prefs.getBoolean("passkey", d.passkey),
        )
    }

    private fun save(s: Settings) = prefs.edit {
        putBoolean("onboarded", s.onboarded)
        putString("name", s.name)
        putString("about", s.about)
        putString("phone", s.phone)
        putString("countryCode", s.countryCode)
        putString("avatar", name(s.avatar))
        putString("email", s.email)
        putInt("themeMode", s.themeMode.ordinal)
        putInt("fontSize", s.fontSize)
        putBoolean("enterIsSend", s.enterIsSend)
        putBoolean("mediaVisibility", s.mediaVisibility)
        putBoolean("keepArchived", s.keepArchived)
        putString("wallpaperLight", name(s.wallpaperLight))
        putString("wallpaperDark", name(s.wallpaperDark))
        putFloat("wallpaperDim", s.wallpaperDim)
        putBoolean("readReceipts", s.readReceipts)
        putInt("lastSeen", s.lastSeen)
        putInt("onlineVisibility", s.onlineVisibility)
        putInt("profilePhoto", s.profilePhoto)
        putInt("aboutPrivacy", s.aboutPrivacy)
        putInt("statusPrivacy", s.statusPrivacy)
        putInt("groupsPrivacy", s.groupsPrivacy)
        putBoolean("callsSilenceUnknown", s.callsSilenceUnknown)
        putLong("defaultTimer", s.defaultTimer)
        putBoolean("appLock", s.appLock)
        putString("appLockPin", s.appLockPin)
        putString("twoStepPin", s.twoStepPin)
        putBoolean("securityNotifications", s.securityNotifications)
        putBoolean("conversationTones", s.conversationTones)
        putBoolean("msgNotifications", s.msgNotifications)
        putString("msgTone", s.msgTone)
        putInt("vibrate", s.vibrate)
        putBoolean("popupNotification", s.popupNotification)
        putBoolean("highPriority", s.highPriority)
        putBoolean("reactionNotifications", s.reactionNotifications)
        putBoolean("groupNotifications", s.groupNotifications)
        putString("groupTone", s.groupTone)
        putString("callRingtone", s.callRingtone)
        putInt("callVibrate", s.callVibrate)
        putBoolean("lessDataCalls", s.lessDataCalls)
        putStringSet("autoDownloadMobile", s.autoDownloadMobile)
        putStringSet("autoDownloadWifi", s.autoDownloadWifi)
        putInt("uploadQuality", s.uploadQuality)
        putString("language", s.language)
        putLong("lastBackup", s.lastBackup)
        putInt("backupFrequency", s.backupFrequency)
        putBoolean("backupIncludeVideos", s.backupIncludeVideos)
        putBoolean("passkey", s.passkey)
    }

    // Drawable ids change between builds, so images are persisted by resource name.
    private fun name(id: Int?): String? = id?.let { runCatching { context.resources.getResourceEntryName(it) }.getOrNull() }

    @android.annotation.SuppressLint("DiscouragedApi")
    private fun res(key: String): Int? {
        val n = runCatching { prefs.getString(key, null) }.getOrNull() ?: return null
        return context.resources.getIdentifier(n, "drawable", context.packageName).takeIf { it != 0 }
    }

    fun reset() {
        prefs.edit { clear() }
        _settings.value = Settings()
    }
}
