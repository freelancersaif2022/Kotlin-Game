package com.irremote.tv.data

import android.content.Context
import com.irremote.tv.ir.CodeSet
import org.json.JSONArray
import org.json.JSONObject

/** A remote the user has set up. The full code map is stored so it keeps working after app updates. */
class SavedRemote(
    val id: Long,
    val name: String,
    val brand: String,
    val label: String,
    val body: String,
) {
    val codeSet: CodeSet by lazy { CodeSet(brand, label, body, 0) }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("brand", brand).put("label", label).put("body", body)

    companion object {
        fun fromJson(o: JSONObject) = SavedRemote(
            o.getLong("id"), o.getString("name"), o.getString("brand"),
            o.optString("label"), o.getString("body"),
        )
    }
}

class RemoteStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("remotes", Context.MODE_PRIVATE)

    fun all(): List<SavedRemote> {
        val raw = prefs.getString(KEY_REMOTES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { SavedRemote.fromJson(arr.getJSONObject(it)) }
        } catch (e: org.json.JSONException) {
            emptyList()
        }
    }

    fun find(id: Long): SavedRemote? = all().firstOrNull { it.id == id }

    fun add(name: String, set: CodeSet): SavedRemote {
        val remote = SavedRemote(System.currentTimeMillis(), name, set.brand, set.label, set.body)
        write(all() + remote)
        lastUsed = remote.id
        return remote
    }

    fun rename(id: Long, name: String) =
        write(all().map { if (it.id == id) SavedRemote(it.id, name, it.brand, it.label, it.body) else it })

    fun delete(id: Long) {
        write(all().filter { it.id != id })
        if (lastUsed == id) lastUsed = 0
    }

    var lastUsed: Long
        get() = prefs.getLong(KEY_LAST, 0)
        set(v) = prefs.edit().putLong(KEY_LAST, v).apply()

    var vibration: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(v) = prefs.edit().putBoolean(KEY_VIBRATE, v).apply()

    private fun write(list: List<SavedRemote>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_REMOTES, arr.toString()).apply()
    }

    private companion object {
        const val KEY_REMOTES = "remotes"
        const val KEY_LAST = "last_used"
        const val KEY_VIBRATE = "vibration"
    }
}
