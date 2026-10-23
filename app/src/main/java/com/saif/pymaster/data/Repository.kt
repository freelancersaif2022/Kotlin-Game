package com.saif.pymaster.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "pymaster_state")

/** Single source of truth for everything the user does. All data stays on the phone. */
class Repository private constructor(private val context: Context) {

    private val key = stringPreferencesKey("app_state_v1")
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<AppState?>(null)
    val state: StateFlow<AppState?> = _state.asStateFlow()

    init {
        scope.launch { load() }
    }

    suspend fun load(): AppState {
        _state.value?.let { return it }
        return mutex.withLock {
            _state.value ?: run {
                val raw = context.dataStore.data.first()[key]
                val loaded = raw?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() } ?: AppState()
                _state.value = loaded
                loaded
            }
        }
    }

    /** Atomically transforms the state and persists it. Returns the new state. */
    suspend fun update(transform: (AppState) -> AppState): AppState {
        load()
        return mutex.withLock {
            val old = _state.value ?: AppState()
            val new = transform(old)
            if (new != old) {
                _state.value = new
                context.dataStore.edit { it[key] = json.encodeToString(AppState.serializer(), new) }
            }
            new
        }
    }

    fun launchUpdate(transform: (AppState) -> AppState) {
        scope.launch { update(transform) }
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }
}
