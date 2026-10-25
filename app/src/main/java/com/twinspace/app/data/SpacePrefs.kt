package com.twinspace.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.SecureRandom

data class Settings(
    val aliases: Map<String, String> = emptyMap(),
    val frozen: Set<String> = emptySet(),
    val lockEnabled: Boolean = false,
    val spaceName: String = "",
)

/**
 * Small, synchronous settings store. Everything here is user-driven: nothing is ever removed
 * unless the user removes it.
 */
class SpacePrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("twinspace", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun read() = Settings(
        aliases = prefs.getString(KEY_ALIASES, null)?.let(::parseMap).orEmpty(),
        frozen = prefs.getStringSet(KEY_FROZEN, emptySet())!!.toSet(),
        lockEnabled = prefs.getBoolean(KEY_LOCK, false),
        spaceName = prefs.getString(KEY_SPACE_NAME, "").orEmpty(),
    )

    private inline fun update(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit(commit = true, action = block)
        _settings.value = read()
    }

    fun setAlias(packageName: String, alias: String?) {
        val map = _settings.value.aliases.toMutableMap()
        if (alias.isNullOrBlank()) map.remove(packageName) else map[packageName] = alias.trim()
        update { putString(KEY_ALIASES, JSONObject(map as Map<*, *>).toString()) }
    }

    fun setFrozen(packageName: String, frozen: Boolean) {
        val set = _settings.value.frozen.toMutableSet()
        if (frozen) set.add(packageName) else set.remove(packageName)
        update { putStringSet(KEY_FROZEN, set) }
    }

    /** Forget per-app data only when the user removed that clone. */
    fun forgetApp(packageName: String) {
        setAlias(packageName, null)
        setFrozen(packageName, false)
    }

    fun setLockEnabled(enabled: Boolean) = update { putBoolean(KEY_LOCK, enabled) }

    fun setSpaceName(name: String) = update { putString(KEY_SPACE_NAME, name.trim()) }

    /** Shared secret handed to the space at creation time; used to authenticate requests. */
    fun token(): String = prefs.getString(KEY_TOKEN, null) ?: newToken().also {
        prefs.edit(commit = true) { putString(KEY_TOKEN, it) }
    }

    fun clearSpaceState() = update {
        remove(KEY_ALIASES)
        remove(KEY_FROZEN)
    }

    private fun parseMap(json: String): Map<String, String> = runCatching {
        val obj = JSONObject(json)
        obj.keys().asSequence().associateWith { obj.getString(it) }
    }.getOrDefault(emptyMap())

    companion object {
        private const val KEY_ALIASES = "aliases"
        private const val KEY_FROZEN = "frozen"
        private const val KEY_LOCK = "lock"
        private const val KEY_SPACE_NAME = "space_name"
        private const val KEY_TOKEN = "token"

        fun newToken(): String {
            val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
