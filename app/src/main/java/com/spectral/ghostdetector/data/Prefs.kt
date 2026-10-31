package com.spectral.ghostdetector.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ghost_prefs")

/** A single logged detection, persisted in the ghost log. */
data class Detection(
    val timestamp: Long,
    val name: String,
    val age: Int,
    val type: String,
    val threat: Int,
    val mood: String,
) {
    fun encode() = listOf(timestamp, name, age, type, threat, mood).joinToString("|")

    companion object {
        fun decode(line: String): Detection? {
            val p = line.split("|")
            if (p.size < 6) return null
            return runCatching {
                Detection(p[0].toLong(), p[1], p[2].toInt(), p[3], p[4].toInt(), p[5])
            }.getOrNull()
        }
    }
}

class Prefs(private val context: Context) {
    private object K {
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val PREMIUM = booleanPreferencesKey("premium")
        val NIGHT_VISION = booleanPreferencesKey("night_vision")
        val HISTORY = stringPreferencesKey("history")
        val TOTAL = intPreferencesKey("total_detections")
    }

    private val data get() = context.dataStore.data

    val sound: Flow<Boolean> = data.map { it[K.SOUND] ?: true }
    val vibration: Flow<Boolean> = data.map { it[K.VIBRATION] ?: true }
    val premium: Flow<Boolean> = data.map { it[K.PREMIUM] ?: false }
    val nightVision: Flow<Boolean> = data.map { it[K.NIGHT_VISION] ?: false }
    val totalDetections: Flow<Int> = data.map { it[K.TOTAL] ?: 0 }
    val history: Flow<List<Detection>> = data.map { p ->
        p[K.HISTORY].orEmpty().lineSequence().mapNotNull { Detection.decode(it) }.toList()
    }

    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[K.SOUND] = v }
    suspend fun setVibration(v: Boolean) = context.dataStore.edit { it[K.VIBRATION] = v }
    suspend fun setPremium(v: Boolean) = context.dataStore.edit { it[K.PREMIUM] = v }
    suspend fun setNightVision(v: Boolean) = context.dataStore.edit { it[K.NIGHT_VISION] = v }

    suspend fun addDetection(d: Detection) = context.dataStore.edit { p ->
        val lines = listOf(d.encode()) + p[K.HISTORY].orEmpty().lines().filter { it.isNotBlank() }
        p[K.HISTORY] = lines.take(MAX_HISTORY).joinToString("\n")
        p[K.TOTAL] = (p[K.TOTAL] ?: 0) + 1
    }

    suspend fun clearHistory() = context.dataStore.edit { it.remove(K.HISTORY) }

    companion object {
        const val MAX_HISTORY = 100
    }
}
