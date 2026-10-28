package com.inkly.notes.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class PrefsState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val tutorialDone: Boolean = false,
    val driveConnected: Boolean = false,
    val accountEmail: String? = null,
    val accountName: String? = null,
    val autoBackup: Boolean = true,
    val lastBackupAt: Long = 0L,
    val lastBackupCount: Int = 0,
    val lastBackupError: String? = null,
    val needsReauth: Boolean = false,
)

/** Small, synchronous settings store exposed as a [StateFlow] (read instantly at startup, no theme flash). */
class AppPrefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("inkly_prefs", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<PrefsState> = _state.asStateFlow()

    private fun read() = PrefsState(
        themeMode = runCatching { ThemeMode.valueOf(sp.getString(THEME, null) ?: "LIGHT") }
            .getOrDefault(ThemeMode.LIGHT),
        tutorialDone = sp.getBoolean(TUTORIAL, false),
        driveConnected = sp.getBoolean(CONNECTED, false),
        accountEmail = sp.getString(EMAIL, null),
        accountName = sp.getString(NAME, null),
        autoBackup = sp.getBoolean(AUTO, true),
        lastBackupAt = sp.getLong(LAST_AT, 0L),
        lastBackupCount = sp.getInt(LAST_COUNT, 0),
        lastBackupError = sp.getString(LAST_ERROR, null),
        needsReauth = sp.getBoolean(REAUTH, false),
    )

    @Synchronized
    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sp.edit().apply(block).apply()
        _state.value = read()
    }

    fun setTheme(mode: ThemeMode) = edit { putString(THEME, mode.name) }
    fun setTutorialDone(done: Boolean) = edit { putBoolean(TUTORIAL, done) }
    fun setAutoBackup(enabled: Boolean) = edit { putBoolean(AUTO, enabled) }
    fun setNeedsReauth(value: Boolean) = edit { putBoolean(REAUTH, value) }

    fun setConnected(name: String?, email: String?) = edit {
        putBoolean(CONNECTED, true)
        putString(NAME, name)
        putString(EMAIL, email)
        putBoolean(REAUTH, false)
    }

    fun setDisconnected() = edit {
        putBoolean(CONNECTED, false)
        remove(NAME); remove(EMAIL); remove(LAST_AT); remove(LAST_COUNT); remove(LAST_ERROR)
        putBoolean(REAUTH, false)
    }

    fun recordBackupSuccess(at: Long, count: Int) = edit {
        putLong(LAST_AT, at)
        putInt(LAST_COUNT, count)
        remove(LAST_ERROR)
        putBoolean(REAUTH, false)
    }

    fun recordBackupError(message: String) = edit { putString(LAST_ERROR, message) }

    private companion object {
        const val THEME = "theme"
        const val TUTORIAL = "tutorial_done"
        const val CONNECTED = "drive_connected"
        const val EMAIL = "drive_email"
        const val NAME = "drive_name"
        const val AUTO = "auto_backup"
        const val LAST_AT = "last_backup_at"
        const val LAST_COUNT = "last_backup_count"
        const val LAST_ERROR = "last_backup_error"
        const val REAUTH = "needs_reauth"
    }
}
