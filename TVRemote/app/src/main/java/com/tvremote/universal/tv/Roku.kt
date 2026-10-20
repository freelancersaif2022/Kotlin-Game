package com.tvremote.universal.tv

import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/** Roku External Control Protocol (HTTP :8060). No pairing required. */
object RokuApi {
    data class Info(
        val name: String,
        val model: String,
        val mac: String,
        val isTv: Boolean,
        val powerOn: Boolean,
    )

    class Forbidden : Exception()

    suspend fun info(http: OkHttpClient, host: String): Info? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("http://$host:8060/query/device-info").build()
        val result = runCatching {
            http.newCall(req).execute().use { resp ->
                if (resp.code == 403) throw Forbidden()
                if (!resp.isSuccessful) return@use null
                val xml = resp.body?.string().orEmpty()
                Info(
                    name = Xml.tag(xml, "user-device-name").ifBlank { Xml.tag(xml, "friendly-device-name") }
                        .ifBlank { Xml.tag(xml, "friendly-model-name") }.ifBlank { "Roku" },
                    model = Xml.tag(xml, "friendly-model-name").ifBlank { Xml.tag(xml, "model-name") },
                    mac = Xml.tag(xml, "ethernet-mac").ifBlank { Xml.tag(xml, "wifi-mac") },
                    isTv = Xml.tag(xml, "is-tv").equals("true", true),
                    powerOn = Xml.tag(xml, "power-mode").let { it.isBlank() || it.equals("PowerOn", true) },
                )
            }
        }
        result.exceptionOrNull()?.let { if (it is Forbidden) throw it }
        result.getOrNull()
    }

    fun key(key: RemoteKey): String? = when (key) {
        RemoteKey.UP -> "Up"
        RemoteKey.DOWN -> "Down"
        RemoteKey.LEFT -> "Left"
        RemoteKey.RIGHT -> "Right"
        RemoteKey.OK -> "Select"
        RemoteKey.BACK -> "Back"
        RemoteKey.HOME -> "Home"
        RemoteKey.MENU -> "Info"
        RemoteKey.INFO -> "Info"
        RemoteKey.VOLUME_UP -> "VolumeUp"
        RemoteKey.VOLUME_DOWN -> "VolumeDown"
        RemoteKey.MUTE -> "VolumeMute"
        RemoteKey.CHANNEL_UP -> "ChannelUp"
        RemoteKey.CHANNEL_DOWN -> "ChannelDown"
        RemoteKey.PLAY_PAUSE -> "Play"
        RemoteKey.REWIND -> "Rev"
        RemoteKey.FAST_FORWARD -> "Fwd"
        else -> null
    }

    fun appId(app: StreamingApp) = when (app) {
        StreamingApp.YOUTUBE -> "837"
        StreamingApp.NETFLIX -> "12"
        StreamingApp.PRIME_VIDEO -> "13"
        StreamingApp.DISNEY_PLUS -> "291097"
    }
}

class RokuController(
    device: TvDevice,
    private val http: OkHttpClient,
    onDeviceChanged: (TvDevice) -> Unit,
) : BaseController(device, onDeviceChanged) {

    override val capabilities = Capabilities(
        keys = RemoteKey.entries.toSet() - RemoteKey.NUMBERS.toSet() - RemoteKey.INPUT,
        text = true,
        backspace = true,
        apps = StreamingApp.entries.toSet(),
    )

    @Volatile private var tvOn = true

    override suspend fun connect() {
        _state.value = ConnectionState.Connecting()
        val info = try {
            RokuApi.info(http, device.host)
        } catch (e: RokuApi.Forbidden) {
            fail(
                FailReason.NEEDS_PAIRING,
                "Your Roku blocks phone control. On the Roku open Settings › System › Advanced system settings › Control by mobile apps › Network access and choose \"Default\" or \"Permissive\".",
            )
            return
        }
        if (info == null) {
            fail(FailReason.UNREACHABLE, Messages.UNREACHABLE)
            return
        }
        tvOn = info.powerOn
        updateDevice { it.copy(model = it.model.ifBlank { info.model }, mac = it.mac.ifBlank { info.mac }) }
        _state.value = ConnectionState.Connected(tvOn = info.powerOn)
    }

    private suspend fun post(path: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("http://${device.host}:8060/$path").post(ByteArray(0).toRequestBody()).build()
            http.newCall(req).execute().use { resp ->
                if (resp.code == 403) {
                    fail(FailReason.NEEDS_PAIRING, "Your Roku blocks phone control. Set Settings › System › Advanced system settings › Control by mobile apps › Network access to \"Default\".")
                }
                resp.isSuccessful
            }
        }.getOrElse {
            if (isConnected) fail(FailReason.UNREACHABLE, Messages.LOST)
            false
        }
    }

    override suspend fun sendKey(key: RemoteKey) {
        if (key == RemoteKey.POWER) {
            if (post(if (tvOn) "keypress/PowerOff" else "keypress/PowerOn")) tvOn = !tvOn
            else post("keypress/Power")
            _state.value = ConnectionState.Connected(tvOn = tvOn)
            return
        }
        RokuApi.key(key)?.let { post("keypress/$it") }
    }

    override suspend fun sendText(text: String) {
        text.forEach { c -> post("keypress/Lit_" + URLEncoder.encode(c.toString(), "UTF-8")) }
    }

    override suspend fun sendBackspace() { post("keypress/Backspace") }

    override suspend fun sendEnter() { post("keypress/Enter") }

    override suspend fun launchApp(app: StreamingApp) { post("launch/${RokuApi.appId(app)}") }

    override suspend fun wake() {
        post("keypress/PowerOn")
    }

    override fun disconnect() {
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            _state.value = ConnectionState.Idle
        }
    }
}

object Xml {
    fun tag(xml: String, name: String): String {
        val m = Regex("<$name[^>]*>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL).find(xml) ?: return ""
        return m.groupValues[1].trim()
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'")
    }
}
