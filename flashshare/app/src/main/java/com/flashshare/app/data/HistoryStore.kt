package com.flashshare.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryFile(val name: String, val size: Long, val mime: String, val uri: String?, val thumb: String? = null)

data class HistoryEntry(
    val id: String,
    val time: Long,
    val sent: Boolean,
    val peerName: String,
    val peerAvatarId: Int,
    val files: List<HistoryFile>,
    val totalBytes: Long,
    val durationMs: Long,
    val status: String, // completed | failed | cancelled
) {
    val avgSpeed: Double get() = if (durationMs > 0) totalBytes * 1000.0 / durationMs else 0.0
}

class HistoryStore(private val context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "history.json")
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            mutex.withLock { _entries.value = runCatching { parse(file.readText()) }.getOrDefault(emptyList()) }
        }
    }

    fun add(entry: HistoryEntry) {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                _entries.value = (listOf(entry) + _entries.value).take(MAX)
                persist()
            }
        }
    }

    val thumbDir = File(context.filesDir, "thumbs")

    fun clear() {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                thumbDir.deleteRecursively()
                _entries.value = emptyList()
                persist()
            }
        }
    }

    private fun persist() {
        val arr = JSONArray()
        _entries.value.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("time", e.time); put("sent", e.sent)
                put("peer", e.peerName); put("av", e.peerAvatarId)
                put("total", e.totalBytes); put("dur", e.durationMs); put("status", e.status)
                put("files", JSONArray().apply {
                    e.files.take(500).forEach { f ->
                        put(JSONObject().apply {
                            put("n", f.name); put("s", f.size); put("m", f.mime); f.uri?.let { put("u", it) }; f.thumb?.let { put("t", it) }
                        })
                    }
                })
            })
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }

    private fun parse(text: String): List<HistoryEntry> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val fa = o.optJSONArray("files") ?: JSONArray()
            HistoryEntry(
                id = o.getString("id"),
                time = o.getLong("time"),
                sent = o.getBoolean("sent"),
                peerName = o.optString("peer"),
                peerAvatarId = o.optInt("av"),
                files = (0 until fa.length()).map { j ->
                    val f = fa.getJSONObject(j)
                    HistoryFile(f.optString("n"), f.optLong("s"), f.optString("m"), if (f.has("u")) f.getString("u") else null, if (f.has("t")) f.getString("t") else null)
                },
                totalBytes = o.optLong("total"),
                durationMs = o.optLong("dur"),
                status = o.optString("status", "completed"),
            )
        }
    }

    private companion object {
        const val MAX = 300
    }
}
