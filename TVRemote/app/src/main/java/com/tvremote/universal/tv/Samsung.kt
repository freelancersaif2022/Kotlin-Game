package com.tvremote.universal.tv

import android.util.Base64
import com.tvremote.universal.core.CertPins
import com.tvremote.universal.core.Tls
import com.tvremote.universal.core.isCertificateChange
import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Samsung Tizen (2016+) WebSocket remote API on ports 8001 (ws) / 8002 (wss + token). */
object SamsungApi {
    const val APP_NAME = "TV Remote"

    data class Info(
        val name: String,
        val model: String,
        val mac: String,
        val tokenAuth: Boolean,
        val powerOn: Boolean,
    )

    suspend fun info(http: OkHttpClient, host: String): Info? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("http://$host:8001/api/v2/").build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val json = JSONObject(resp.body?.string().orEmpty())
                val d = json.optJSONObject("device") ?: JSONObject()
                val rawName = d.optString("name").ifBlank { json.optString("name") }
                Info(
                    name = rawName.removePrefix("[TV]").trim().ifBlank { "Samsung TV" },
                    model = d.optString("modelName"),
                    mac = d.optString("wifiMac"),
                    tokenAuth = d.optString("TokenAuthSupport").equals("true", true),
                    powerOn = !d.optString("PowerState").equals("standby", true),
                )
            }
        }.getOrNull()
    }

    fun key(key: RemoteKey, playing: Boolean): String = when (key) {
        RemoteKey.POWER -> "KEY_POWER"
        RemoteKey.UP -> "KEY_UP"
        RemoteKey.DOWN -> "KEY_DOWN"
        RemoteKey.LEFT -> "KEY_LEFT"
        RemoteKey.RIGHT -> "KEY_RIGHT"
        RemoteKey.OK -> "KEY_ENTER"
        RemoteKey.BACK -> "KEY_RETURN"
        RemoteKey.HOME -> "KEY_HOME"
        RemoteKey.MENU -> "KEY_MENU"
        RemoteKey.VOLUME_UP -> "KEY_VOLUP"
        RemoteKey.VOLUME_DOWN -> "KEY_VOLDOWN"
        RemoteKey.MUTE -> "KEY_MUTE"
        RemoteKey.CHANNEL_UP -> "KEY_CHUP"
        RemoteKey.CHANNEL_DOWN -> "KEY_CHDOWN"
        RemoteKey.INPUT -> "KEY_SOURCE"
        RemoteKey.PLAY_PAUSE -> if (playing) "KEY_PAUSE" else "KEY_PLAY"
        RemoteKey.REWIND -> "KEY_REWIND"
        RemoteKey.FAST_FORWARD -> "KEY_FF"
        RemoteKey.INFO -> "KEY_INFO"
        else -> "KEY_${RemoteKey.NUMBERS.indexOf(key)}"
    }

    fun appIds(app: StreamingApp): List<String> = when (app) {
        StreamingApp.YOUTUBE -> listOf("111299001912")
        StreamingApp.NETFLIX -> listOf("3201907018807", "11101200001")
        StreamingApp.PRIME_VIDEO -> listOf("3201910019365", "3201512006785")
        StreamingApp.DISNEY_PLUS -> listOf("3201901017640")
    }
}

class SamsungController(
    device: TvDevice,
    private val http: OkHttpClient,
    private val pins: CertPins,
    onDeviceChanged: (TvDevice) -> Unit,
    private val approvalTimeoutMs: Long = 12_000,
) : BaseController(device, onDeviceChanged) {

    override val capabilities = Capabilities(
        keys = RemoteKey.entries.toSet(),
        text = true,
        backspace = false,
        apps = StreamingApp.entries.toSet(),
    )

    @Volatile private var ws: WebSocket? = null
    @Volatile private var playing = false

    override suspend fun connect() {
        disconnect()
        _state.value = ConnectionState.Connecting()
        val info = SamsungApi.info(http, device.host)
        if (info == null) {
            fail(FailReason.UNREACHABLE, Messages.UNREACHABLE)
            return
        }
        updateDevice {
            it.copy(
                model = it.model.ifBlank { info.model },
                mac = it.mac.ifBlank { info.mac },
                credentials = it.credentials + ("tokenAuth" to info.tokenAuth.toString()),
            )
        }
        val name = Base64.encodeToString(SamsungApi.APP_NAME.toByteArray(), Base64.NO_WRAP)
        val token = device.credentials["token"]
        val (url, client) = if (info.tokenAuth) {
            val tokenPart = token?.let { "&token=$it" }.orEmpty()
            "wss://${device.host}:8002/api/v2/channels/samsung.remote.control?name=$name$tokenPart" to
                Tls.okHttp(http, device.host, 8002, pins).newBuilder().pingInterval(20, TimeUnit.SECONDS).build()
        } else {
            "ws://${device.host}:8001/api/v2/channels/samsung.remote.control?name=$name" to
                http.newBuilder().pingInterval(20, TimeUnit.SECONDS).build()
        }
        val tvOn = info.powerOn
        val socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (ws !== webSocket) return
                val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (json.optString("event")) {
                    "ms.channel.connect" -> {
                        json.optJSONObject("data")?.optString("token")?.takeIf { it.isNotBlank() }?.let {
                            updateCredentials("token" to it)
                        }
                        _state.value = ConnectionState.Connected(tvOn = tvOn)
                    }
                    "ms.channel.unauthorized" -> {
                        ws = null
                        webSocket.close(1000, null)
                        fail(
                            FailReason.NEEDS_PAIRING,
                            "The TV blocked this phone. On your TV open Settings › General › External Device Manager › Device Connection Manager › Device List and allow \"${SamsungApi.APP_NAME}\".",
                        )
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (ws !== webSocket) return
                ws = null
                when {
                    t.isCertificateChange() -> fail(FailReason.CERT_CHANGED, Messages.CERT_CHANGED)
                    else -> fail(FailReason.UNREACHABLE, if (isConnected) Messages.LOST else Messages.UNREACHABLE)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (ws !== webSocket) return
                ws = null
                fail(FailReason.UNREACHABLE, Messages.LOST)
            }
        })
        ws = socket
        // A first-time connection shows "Allow" on the TV and waits for the user.
        val quick = awaitSettled(2500)
        if (quick is ConnectionState.Connecting) {
            _state.value = ConnectionState.Connecting(hint = "Select \"Allow\" on your TV using the TV remote")
            val settled = awaitSettled(approvalTimeoutMs)
            if (settled is ConnectionState.Connecting) {
                disconnect()
                fail(FailReason.NEEDS_PAIRING, "The TV did not respond. Select \"Allow\" on the TV when the popup appears, then try again.")
            }
        }
    }

    private fun sendJson(json: JSONObject) {
        val socket = ws ?: return
        socket.send(json.toString())
    }

    private fun remoteControl(params: JSONObject) = sendJson(
        JSONObject().put("method", "ms.remote.control").put("params", params),
    )

    override suspend fun sendKey(key: RemoteKey) {
        val code = SamsungApi.key(key, playing)
        if (key == RemoteKey.PLAY_PAUSE) playing = !playing
        remoteControl(
            JSONObject()
                .put("Cmd", "Click")
                .put("DataOfCmd", code)
                .put("Option", "false")
                .put("TypeOfRemote", "SendRemoteKey"),
        )
    }

    override suspend fun sendText(text: String) {
        if (text.isEmpty()) return
        val encoded = Base64.encodeToString(text.toByteArray(), Base64.NO_WRAP)
        remoteControl(
            JSONObject()
                .put("Cmd", encoded)
                .put("DataOfCmd", "base64")
                .put("TypeOfRemote", "SendInputString"),
        )
    }

    override suspend fun sendEnter() {
        remoteControl(JSONObject().put("TypeOfRemote", "SendInputEnd"))
    }

    override suspend fun launchApp(app: StreamingApp) {
        SamsungApi.appIds(app).forEach { id ->
            sendJson(
                JSONObject()
                    .put("method", "ms.channel.emit")
                    .put(
                        "params",
                        JSONObject()
                            .put("event", "ed.apps.launch")
                            .put("to", "host")
                            .put("data", JSONObject().put("appId", id).put("action_type", "DEEP_LINK").put("metaTag", "")),
                    ),
            )
        }
    }

    override fun disconnect() {
        val socket = ws
        ws = null
        runCatching { socket?.close(1000, null) }
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            _state.value = ConnectionState.Idle
        }
    }
}
