package com.voxcast.mic.net

import android.net.Uri
import java.security.SecureRandom

enum class HostMode { LAN, WIFI_DIRECT }

/**
 * Everything the microphone phone needs to reach a speaker, carried inside the QR code.
 * A fresh random [token] is generated for every session, so each QR code is unique and
 * a stale or photographed code can't be reused.
 */
data class ConnectionInfo(
    val speakerName: String,
    val hosts: List<String>,
    val port: Int,
    val token: String,
    val mode: HostMode,
    val ssid: String? = null,
    val passphrase: String? = null,
) {
    fun toQrPayload(): String = Uri.Builder()
        .scheme(SCHEME)
        .authority("join")
        .appendQueryParameter("v", PROTOCOL_VERSION.toString())
        .appendQueryParameter("n", speakerName)
        .appendQueryParameter("h", hosts.joinToString(","))
        .appendQueryParameter("p", port.toString())
        .appendQueryParameter("t", token)
        .appendQueryParameter("m", mode.name)
        .apply {
            ssid?.let { appendQueryParameter("s", it) }
            passphrase?.let { appendQueryParameter("k", it) }
        }
        .build()
        .toString()

    companion object {
        const val SCHEME = "voxcast"
        const val PROTOCOL_VERSION = 1

        fun parse(raw: String): ConnectionInfo? = runCatching {
            val uri = Uri.parse(raw.trim())
            if (uri.scheme != SCHEME || uri.authority != "join") return null
            val hosts = uri.getQueryParameter("h").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val port = uri.getQueryParameter("p")?.toIntOrNull() ?: return null
            val token = uri.getQueryParameter("t") ?: return null
            if (hosts.isEmpty() || port !in 1..65535) return null
            ConnectionInfo(
                speakerName = uri.getQueryParameter("n") ?: "Speaker",
                hosts = hosts,
                port = port,
                token = token,
                mode = runCatching { HostMode.valueOf(uri.getQueryParameter("m") ?: "") }.getOrDefault(HostMode.LAN),
                ssid = uri.getQueryParameter("s"),
                passphrase = uri.getQueryParameter("k"),
            )
        }.getOrNull()

        private val random = SecureRandom()
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"

        fun randomToken(length: Int = 10): String =
            buildString(length) { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
    }
}
