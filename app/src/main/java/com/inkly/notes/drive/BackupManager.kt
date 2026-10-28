package com.inkly.notes.drive

import android.content.Context
import com.inkly.notes.data.AppPrefs
import com.inkly.notes.data.MergeResult
import com.inkly.notes.data.Note
import com.inkly.notes.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val repo: NoteRepository,
    private val prefs: AppPrefs,
) {
    private val lock = Mutex()

    /** Uploads a full snapshot of all notes, then prunes old snapshots (keeps the latest [KEEP]). */
    suspend fun backup(token: String): DriveFile = lock.withLock {
        DriveAuth.withToken(context, token) { t ->
            withContext(Dispatchers.IO) {
                val notes = repo.getAll()
                val api = DriveApi(t)
                val file = api.upload(encode(notes).toByteArray(), notes.size)
                api.listBackups().filter { it.id != file.id }.drop(KEEP - 1).forEach {
                    runCatching { api.delete(it.id) }
                }
                prefs.recordBackupSuccess(System.currentTimeMillis(), notes.size)
                file
            }
        }
    }

    suspend fun latestBackup(token: String): DriveFile? = DriveAuth.withToken(context, token) { t ->
        withContext(Dispatchers.IO) { DriveApi(t).listBackups().firstOrNull() }
    }

    suspend fun restore(token: String, file: DriveFile): MergeResult = lock.withLock {
        DriveAuth.withToken(context, token) { t ->
            withContext(Dispatchers.IO) {
                val notes = decode(String(DriveApi(t).download(file.id)))
                repo.merge(notes)
            }
        }
    }

    private fun encode(notes: List<Note>): String {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(
                JSONObject()
                    .put("uid", n.uid)
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("color", n.color)
                    .put("pinned", n.pinned)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
            )
        }
        return JSONObject()
            .put("app", "inkly")
            .put("format", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("notes", arr)
            .toString()
    }

    private fun decode(json: String): List<Note> {
        val arr = JSONObject(json).optJSONArray("notes") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Note(
                uid = o.getString("uid"),
                title = o.optString("title"),
                content = o.optString("content"),
                color = o.optInt("color"),
                pinned = o.optBoolean("pinned"),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt"),
            )
        }
    }

    private companion object {
        const val KEEP = 5
    }
}
