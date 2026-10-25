package com.bornomala.keyboard.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bornomala.keyboard.ime.Dictionaries
import com.bornomala.keyboard.prefs.Prefs
import org.json.JSONObject
import java.io.StringWriter

/** Exports and restores settings plus learned words as a small JSON file. */
object Backup {

    private const val TAG = "Backup"
    private const val FORMAT = "bornomala-backup"

    fun export(context: Context, uri: Uri): Boolean = try {
        val settings = JSONObject()
        for ((key, value) in Prefs.get(context).all) {
            val typed = when (value) {
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is String -> JSONObject().put("t", "s").put("v", value)
                else -> null
            }
            if (typed != null) settings.put(key, typed)
        }
        val words = StringWriter().also { Dictionaries.user.write(it, markSaved = false) }.toString()
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", 1)
            .put("settings", settings)
            .put("userDictionary", words)
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) } != null
    } catch (e: Exception) {
        Log.w(TAG, "Export failed", e)
        false
    }

    fun import(context: Context, uri: Uri): Boolean = try {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("empty")
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT)
        val settings = root.getJSONObject("settings")
        val editor = Prefs.get(context).edit()
        for (key in settings.keys()) {
            val o = settings.getJSONObject(key)
            when (o.getString("t")) {
                "b" -> editor.putBoolean(key, o.getBoolean("v"))
                "i" -> editor.putInt(key, o.getInt("v"))
                "l" -> editor.putLong(key, o.getLong("v"))
                "f" -> editor.putFloat(key, o.getDouble("v").toFloat())
                "s" -> editor.putString(key, o.getString("v"))
            }
        }
        editor.apply()
        Dictionaries.user.read(root.optString("userDictionary").reader().buffered(), merge = true)
        Dictionaries.saveUser(context)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Import failed", e)
        false
    }
}
