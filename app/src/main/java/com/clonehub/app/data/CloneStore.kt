package com.clonehub.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "clonehub")

/**
 * Durable storage for clones. Records are encoded as a single string so the app
 * needs no database and no serialization library, keeping the APK tiny while the
 * data survives reboots and app restarts for the lifetime of the install.
 *
 * Encoding: records joined by '\u0002'; fields within a record joined by '\u0001'
 * in the order id, packageName, label, accent, createdAt. These control characters
 * cannot appear in a package name or a user-typed label, so parsing is unambiguous.
 */
class CloneStore(private val context: Context) {

    private val key = stringPreferencesKey("clones_v1")

    val clones: Flow<List<Clone>> = context.dataStore.data.map { prefs ->
        decode(prefs[key].orEmpty())
    }

    suspend fun save(clones: List<Clone>) {
        context.dataStore.edit { it[key] = encode(clones) }
    }

    private fun encode(list: List<Clone>): String =
        list.joinToString("\u0002") { c ->
            listOf(c.id, c.packageName, c.label, c.accent, c.createdAt)
                .joinToString("\u0001")
        }

    private fun decode(raw: String): List<Clone> {
        if (raw.isEmpty()) return emptyList()
        return raw.split("\u0002").mapNotNull { rec ->
            val f = rec.split("\u0001")
            if (f.size != 5) return@mapNotNull null
            runCatching {
                Clone(
                    id = f[0].toLong(),
                    packageName = f[1],
                    label = f[2],
                    accent = f[3].toLong(),
                    createdAt = f[4].toLong(),
                )
            }.getOrNull()
        }
    }
}
