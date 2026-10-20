package com.flashshare.app.net

import org.json.JSONObject

/**
 * Everything a scanner needs to connect instantly – encoded in the QR code shown by the hosting phone.
 * mode: "r" = a receiver waiting for a sender, "g" = a group sender waiting for receivers.
 */
data class QrPayload(
    val ssid: String,
    val pass: String,
    val freq: Int,
    val host: String,
    val port: Int,
    val name: String,
    val avatarId: Int,
    val deviceId: String,
    val mode: String,
    val encrypted: Boolean,
    val salt: String,
) {
    val isGroup: Boolean get() = mode == "g"

    fun encode(): String = PREFIX + JSONObject().apply {
        put("s", ssid); put("p", pass); put("f", freq); put("h", host); put("o", port)
        put("n", name.take(32)); put("a", avatarId); put("d", deviceId.take(12)); put("m", mode)
        if (encrypted) { put("e", 1); put("k", salt) }
    }.toString()

    companion object {
        private const val PREFIX = "FLASHSHARE1:"

        fun decode(text: String): QrPayload? = runCatching {
            if (!text.startsWith(PREFIX)) return null
            val o = JSONObject(text.removePrefix(PREFIX))
            QrPayload(
                ssid = o.getString("s"), pass = o.getString("p"), freq = o.optInt("f"),
                host = o.optString("h", "192.168.49.1"), port = o.optInt("o", Proto.PORT),
                name = o.optString("n", "Phone"), avatarId = o.optInt("a"), deviceId = o.optString("d"),
                mode = o.optString("m", "r"), encrypted = o.optInt("e") == 1, salt = o.optString("k"),
            )
        }.getOrNull()
    }
}
