package com.inkly.notes.drive

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

class DriveException(val code: Int, message: String) : IOException(message)

data class DriveFile(val id: String, val modifiedTime: Long, val noteCount: Int)

/** Minimal Google Drive v3 REST client (no heavy SDK – keeps the APK tiny). Blocking; call off the main thread. */
class DriveApi(private val token: String) {

    fun aboutUser(): Pair<String?, String?> {
        val json = JSONObject(String(request("GET", "$API/about?fields=user(displayName,emailAddress)")))
        val user = json.optJSONObject("user") ?: return null to null
        return user.optString("displayName").ifBlank { null } to user.optString("emailAddress").ifBlank { null }
    }

    fun listBackups(): List<DriveFile> {
        val q = enc("name = '$BACKUP_NAME' and trashed = false")
        val fields = enc("files(id,modifiedTime,appProperties)")
        val url = "$API/files?spaces=appDataFolder&q=$q&fields=$fields&orderBy=${enc("modifiedTime desc")}&pageSize=50"
        val files = JSONObject(String(request("GET", url))).optJSONArray("files") ?: JSONArray()
        return (0 until files.length()).map { i ->
            val f = files.getJSONObject(i)
            DriveFile(
                id = f.getString("id"),
                modifiedTime = runCatching { Instant.parse(f.getString("modifiedTime")).toEpochMilli() }.getOrDefault(0L),
                noteCount = f.optJSONObject("appProperties")?.optString("notes")?.toIntOrNull() ?: -1,
            )
        }
    }

    fun upload(data: ByteArray, noteCount: Int): DriveFile {
        val meta = JSONObject()
            .put("name", BACKUP_NAME)
            .put("mimeType", "application/json")
            .put("parents", JSONArray().put("appDataFolder"))
            .put("appProperties", JSONObject().put("notes", noteCount.toString()))
        val boundary = "inkly_${System.nanoTime()}"
        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(meta.toString()).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json\r\n\r\n")
        }.toByteArray() + data + "\r\n--$boundary--\r\n".toByteArray()

        val res = JSONObject(
            String(
                request(
                    "POST", "$UPLOAD/files?uploadType=multipart&fields=id,modifiedTime",
                    "multipart/related; boundary=$boundary", body,
                )
            )
        )
        return DriveFile(
            id = res.getString("id"),
            modifiedTime = runCatching { Instant.parse(res.getString("modifiedTime")).toEpochMilli() }
                .getOrDefault(System.currentTimeMillis()),
            noteCount = noteCount,
        )
    }

    fun download(id: String): ByteArray = request("GET", "$API/files/$id?alt=media")

    fun delete(id: String) {
        request("DELETE", "$API/files/$id")
    }

    private fun request(method: String, url: String, contentType: String? = null, body: ByteArray? = null): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            if (code !in 200..299) throw DriveException(code, errorMessage(code, bytes))
            return bytes
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(code: Int, bytes: ByteArray): String {
        val msg = runCatching { JSONObject(String(bytes)).getJSONObject("error").getString("message") }.getOrNull()
        return msg ?: "Google Drive error ($code)"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    companion object {
        const val BACKUP_NAME = "inkly_backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
