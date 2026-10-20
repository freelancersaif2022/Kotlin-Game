package com.flashshare.app.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Category(val label: String, val folder: String) {
    APP("Apps", "Apps"),
    PHOTO("Photos", "Photos"),
    VIDEO("Videos", "Videos"),
    MUSIC("Music", "Music"),
    FILE("Files", "Files"),
    CONTACT("Contacts", "Contacts");

    companion object {
        fun fromMime(mime: String?, name: String = ""): Category {
            val m = mime.orEmpty()
            return when {
                m.startsWith("image/") -> PHOTO
                m.startsWith("video/") -> VIDEO
                m.startsWith("audio/") -> MUSIC
                m == "application/vnd.android.package-archive" || name.endsWith(".apk", true) -> APP
                m.contains("vcard") || name.endsWith(".vcf", true) -> CONTACT
                else -> FILE
            }
        }
    }
}

/** Something the user picked to send. [uri] may be content:// or file:// (APKs, exported contacts). */
data class ShareItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String,
    val category: Category,
    val relPath: String = "",
    val dateAdded: Long = 0,
    val durationMs: Long = 0,
    val subtitle: String = "",
    val packageName: String? = null,
) {
    val key: String get() = uri.toString() + "|" + relPath
}

/** Selected files, shared between the picker, clone, file manager and the transfer screens. */
class SelectionStore {
    private val _items = MutableStateFlow<Map<String, ShareItem>>(linkedMapOf())
    val items: StateFlow<Map<String, ShareItem>> = _items.asStateFlow()

    val list: List<ShareItem> get() = _items.value.values.toList()
    val totalSize: Long get() = _items.value.values.sumOf { it.size }

    fun isSelected(item: ShareItem) = _items.value.containsKey(item.key)

    fun toggle(item: ShareItem) {
        val m = LinkedHashMap(_items.value)
        if (m.remove(item.key) == null) m[item.key] = item
        _items.value = m
    }

    fun addAll(items: Collection<ShareItem>) {
        val m = LinkedHashMap(_items.value)
        items.forEach { m[it.key] = it }
        _items.value = m
    }

    fun removeAll(items: Collection<ShareItem>) {
        val m = LinkedHashMap(_items.value)
        items.forEach { m.remove(it.key) }
        _items.value = m
    }

    fun set(items: Collection<ShareItem>) {
        _items.value = LinkedHashMap<String, ShareItem>().apply { items.forEach { put(it.key, it) } }
    }

    fun clear() {
        _items.value = linkedMapOf()
    }
}
