package com.bornomala.keyboard.ime

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/**
 * Recent clipboard items kept on the device (never uploaded anywhere). Items are re-read from
 * storage for every operation so the settings screen and the keyboard always agree.
 */
class ClipboardHistory(context: Context) {

    data class Item(val text: String, val pinned: Boolean, val time: Long)

    private val prefs = context.applicationContext.getSharedPreferences("bornomala_clipboard", Context.MODE_PRIVATE)
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val items = ArrayList<Item>()
    var enabled = true

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { capture() }

    fun start() = clipboard.addPrimaryClipChangedListener(clipListener)

    fun stop() = clipboard.removePrimaryClipChangedListener(clipListener)

    private fun capture() {
        if (!enabled) return
        val clip = try {
            clipboard.primaryClip
        } catch (e: SecurityException) {
            null
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            clip.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
        ) return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).text?.toString() ?: return
        add(text)
    }

    fun add(text: String) {
        if (text.isBlank() || text.length > MAX_LENGTH) return
        load()
        val existing = items.firstOrNull { it.text == text }
        items.remove(existing)
        items.add(0, Item(text, existing?.pinned ?: false, System.currentTimeMillis()))
        trim()
        save()
    }

    fun all(): List<Item> {
        load()
        return items.sortedWith(compareByDescending<Item> { it.pinned }.thenByDescending { it.time })
    }

    fun togglePin(item: Item) {
        load()
        val i = items.indexOfFirst { it.text == item.text }
        if (i >= 0) items[i] = items[i].copy(pinned = !items[i].pinned)
        save()
    }

    fun remove(item: Item) {
        load()
        items.removeAll { it.text == item.text }
        save()
    }

    /** Removes unpinned items. */
    fun clear() {
        load()
        items.removeAll { !it.pinned }
        save()
    }

    private fun trim() {
        var unpinned = 0
        val it = items.iterator()
        while (it.hasNext()) {
            val item = it.next()
            if (!item.pinned && ++unpinned > MAX_ITEMS) it.remove()
        }
    }

    private fun load() {
        items.clear()
        val raw = prefs.getString(KEY, null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items += Item(o.getString("t"), o.optBoolean("p"), o.optLong("ts"))
            }
        } catch (e: Exception) {
            items.clear()
        }
    }

    private fun save() {
        val arr = JSONArray()
        for (item in items) arr.put(JSONObject().put("t", item.text).put("p", item.pinned).put("ts", item.time))
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "items"
        private const val MAX_ITEMS = 30
        private const val MAX_LENGTH = 5000

        fun clearAll(context: Context) {
            context.applicationContext.getSharedPreferences("bornomala_clipboard", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }
}
