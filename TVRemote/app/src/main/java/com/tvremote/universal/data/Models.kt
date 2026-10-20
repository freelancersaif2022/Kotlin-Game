package com.tvremote.universal.data

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.util.UUID

enum class TvPlatform(
    val title: String,
    val short: String,
    val brands: String,
    val color: Color,
) {
    ANDROID_TV("Android TV / Google TV", "Google TV", "Sony, TCL, Hisense, Philips, Xiaomi, Sharp, Chromecast", Color(0xFF34A853)),
    SAMSUNG("Samsung Smart TV", "Samsung", "Samsung Tizen TVs (2016 and newer)", Color(0xFF1428A0)),
    LG("LG webOS TV", "LG webOS", "LG Smart TVs with webOS", Color(0xFFA50034)),
    ROKU("Roku", "Roku", "Roku players & Roku TVs (TCL, Hisense, Onn, Sharp…)", Color(0xFF6F1AB1)),
    VIZIO("Vizio SmartCast", "Vizio", "Vizio SmartCast TVs", Color(0xFF0F6FDE)),
    FIRE_TV("Amazon Fire TV", "Fire TV", "Fire TV Stick, Fire TV Cube & Fire TV Edition TVs", Color(0xFFFF9900)),
}

data class TvDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val platform: TvPlatform,
    val host: String,
    val model: String = "",
    val mac: String = "",
    val credentials: Map<String, String> = emptyMap(),
    val addedAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("platform", platform.name)
        put("host", host)
        put("model", model)
        put("mac", mac)
        put("credentials", JSONObject(credentials))
        put("addedAt", addedAt)
        put("lastUsed", lastUsed)
    }

    companion object {
        fun fromJson(o: JSONObject): TvDevice? = runCatching {
            val creds = o.optJSONObject("credentials")
            val map = buildMap {
                creds?.keys()?.forEach { k -> put(k, creds.optString(k)) }
            }
            TvDevice(
                id = o.getString("id"),
                name = o.getString("name"),
                platform = TvPlatform.valueOf(o.getString("platform")),
                host = o.getString("host"),
                model = o.optString("model"),
                mac = o.optString("mac"),
                credentials = map,
                addedAt = o.optLong("addedAt"),
                lastUsed = o.optLong("lastUsed"),
            )
        }.getOrNull()
    }
}

data class DiscoveredTv(
    val platform: TvPlatform,
    val host: String,
    val name: String,
    val model: String = "",
    val mac: String = "",
)

enum class RemoteKey {
    POWER, UP, DOWN, LEFT, RIGHT, OK, BACK, HOME, MENU,
    VOLUME_UP, VOLUME_DOWN, MUTE, CHANNEL_UP, CHANNEL_DOWN, INPUT,
    PLAY_PAUSE, REWIND, FAST_FORWARD, INFO,
    NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9;

    companion object {
        val NUMBERS = listOf(NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9)
        fun number(n: Int) = NUMBERS[n]
    }
}

enum class StreamingApp(val title: String, val color: Color, val textColor: Color) {
    YOUTUBE("YouTube", Color(0xFFFF0033), Color.White),
    NETFLIX("Netflix", Color(0xFF141414), Color(0xFFE50914)),
    PRIME_VIDEO("Prime Video", Color(0xFF00A8E1), Color.White),
    DISNEY_PLUS("Disney+", Color(0xFF0B1C5A), Color.White),
}
