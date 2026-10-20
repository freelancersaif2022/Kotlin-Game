package com.tvremote.universal.tv

import com.tvremote.universal.core.CertPins
import com.tvremote.universal.core.Tls
import com.tvremote.universal.core.isCertificateChange
import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** LG webOS SSAP protocol over WebSocket (ws :3000, or wss :3001 on newer firmware). */
class LgController(
    device: TvDevice,
    private val http: OkHttpClient,
    private val pins: CertPins,
    onDeviceChanged: (TvDevice) -> Unit,
    private val approvalTimeoutMs: Long = 15_000,
) : BaseController(device, onDeviceChanged) {

    override val capabilities = Capabilities(
        keys = RemoteKey.entries.toSet(),
        text = true,
        backspace = true,
        apps = StreamingApp.entries.toSet(),
    )

    @Volatile private var main: WebSocket? = null
    @Volatile private var pointer: WebSocket? = null
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()
    private val ids = AtomicInteger(1)
    @Volatile private var registered: CompletableDeferred<Boolean>? = null
    @Volatile private var playing = false

    private fun clientFor(url: String): OkHttpClient {
        val u = url.replaceFirst("wss://", "https://").replaceFirst("ws://", "http://").toHttpUrlOrNull()
        val base = http.newBuilder().pingInterval(20, TimeUnit.SECONDS).build()
        return if (url.startsWith("wss://") && u != null) Tls.okHttp(base, u.host, u.port, pins) else base
    }

    private suspend fun open(url: String, onText: (WebSocket, String) -> Unit, onDrop: (WebSocket, Throwable?) -> Unit): WebSocket? {
        val opened = CompletableDeferred<WebSocket?>()
        val error = arrayOfNulls<Throwable>(1)
        clientFor(url).newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { opened.complete(webSocket) }
            override fun onMessage(webSocket: WebSocket, text: String) = onText(webSocket, text)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                error[0] = t
                if (!opened.complete(null)) onDrop(webSocket, t)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!opened.complete(null)) onDrop(webSocket, null)
            }
        })
        val ws = withTimeoutOrNull(6000) { opened.await() }
        if (ws == null && error[0]?.isCertificateChange() == true) throw error[0]!!
        return ws
    }

    override suspend fun connect() {
        disconnect()
        _state.value = ConnectionState.Connecting()
        val socket = try {
            open("ws://${device.host}:3000", ::onMainMessage, ::onMainDrop)
                ?: open("wss://${device.host}:3001", ::onMainMessage, ::onMainDrop)
        } catch (e: Exception) {
            fail(FailReason.CERT_CHANGED, Messages.CERT_CHANGED)
            return
        }
        if (socket == null) {
            fail(FailReason.UNREACHABLE, Messages.UNREACHABLE)
            return
        }
        main = socket
        val reg = CompletableDeferred<Boolean>()
        registered = reg
        // Newer webOS versions expect hello/system info before registration.
        socket.send(JSONObject().put("id", "hello").put("type", "hello").put("payload", JSONObject()).toString())
        socket.send(registrationMessage().toString())

        var ok = withTimeoutOrNull(3000) { reg.await() }
        if (ok == null) {
            _state.value = ConnectionState.Connecting(hint = "Select \"Accept\" on your TV using the TV remote")
            ok = withTimeoutOrNull(approvalTimeoutMs) { reg.await() }
        }
        if (ok != true) {
            disconnect()
            fail(FailReason.NEEDS_PAIRING, "The TV did not accept the connection. Select \"Accept\" when the popup appears on the TV, then try again.")
            return
        }

        // Pointer socket for button presses.
        val sock = request("com.webos.service.networkinput/getPointerInputSocket")
        val path = sock?.optJSONObject("payload")?.optString("socketPath").orEmpty()
        if (path.isNotBlank()) {
            pointer = runCatching { open(path, { _, _ -> }, { ws, _ -> if (pointer === ws) pointer = null }) }.getOrNull()
        }
        _state.value = ConnectionState.Connected(tvOn = true)

        // Remember MAC / model for Wake-on-LAN.
        request("com.webos.service.connectionmanager/getinfo")?.optJSONObject("payload")?.let { p ->
            val mac = p.optJSONObject("wiredInfo")?.optString("macAddress").orEmpty().ifBlank {
                p.optJSONObject("wifiInfo")?.optString("macAddress").orEmpty()
            }
            if (mac.isNotBlank()) updateDevice { it.copy(mac = it.mac.ifBlank { mac }) }
        }
        if (device.model.isBlank()) {
            request("system/getSystemInfo")?.optJSONObject("payload")?.optString("modelName")?.takeIf { it.isNotBlank() }?.let { m ->
                updateDevice { it.copy(model = m) }
            }
        }
    }

    private fun registrationMessage(): JSONObject {
        val permissions = listOf(
            "APP_TO_APP", "CLOSE", "CONTROL_AUDIO", "CONTROL_DISPLAY", "CONTROL_INPUT_JOYSTICK",
            "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_INPUT_MEDIA_RECORDING", "CONTROL_INPUT_TEXT",
            "CONTROL_INPUT_TV", "CONTROL_MOUSE_AND_KEYBOARD", "CONTROL_POWER", "CONTROL_TV_SCREEN",
            "LAUNCH", "LAUNCH_WEBAPP", "READ_APP_STATUS", "READ_COUNTRY_INFO", "READ_CURRENT_CHANNEL",
            "READ_INPUT_DEVICE_LIST", "READ_INSTALLED_APPS", "READ_LGE_SDX", "READ_LGE_TV_INPUT_EVENTS",
            "READ_NETWORK_STATE", "READ_NOTIFICATIONS", "READ_POWER_STATE", "READ_RUNNING_APPS",
            "READ_SETTINGS", "READ_TV_CHANNEL_LIST", "READ_TV_CURRENT_TIME", "READ_UPDATE_INFO", "SEARCH",
            "TEST_OPEN", "TEST_PROTECTED", "TEST_SECURE", "UPDATE_FROM_REMOTE_APP",
            "WRITE_NOTIFICATION_ALERT", "WRITE_NOTIFICATION_TOAST", "WRITE_SETTINGS",
        )
        val payload = JSONObject()
            .put("forcePairing", false)
            .put("pairingType", "PROMPT")
            .put(
                "manifest",
                JSONObject()
                    .put("appVersion", "1.1")
                    .put("manifestVersion", 1)
                    .put("permissions", JSONArray(permissions)),
            )
        device.credentials["clientKey"]?.let { payload.put("client-key", it) }
        return JSONObject().put("type", "register").put("id", "register_0").put("payload", payload)
    }

    private fun onMainMessage(ws: WebSocket, text: String) {
        if (main !== ws) return
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        val id = json.optString("id")
        when (json.optString("type")) {
            "registered" -> {
                json.optJSONObject("payload")?.optString("client-key")?.takeIf { it.isNotBlank() }?.let {
                    updateCredentials("clientKey" to it)
                }
                registered?.complete(true)
            }
            "error" -> {
                if (id == "register_0") registered?.complete(false)
                pending.remove(id)?.complete(json)
            }
            else -> pending.remove(id)?.complete(json)
        }
    }

    private fun onMainDrop(ws: WebSocket, t: Throwable?) {
        if (main !== ws) return
        main = null
        registered?.complete(false)
        if (t?.isCertificateChange() == true) fail(FailReason.CERT_CHANGED, Messages.CERT_CHANGED)
        else fail(FailReason.UNREACHABLE, Messages.LOST)
    }

    private suspend fun request(uri: String, payload: JSONObject = JSONObject()): JSONObject? {
        val ws = main ?: return null
        val id = "req_${ids.getAndIncrement()}"
        val deferred = CompletableDeferred<JSONObject>()
        pending[id] = deferred
        ws.send(JSONObject().put("id", id).put("type", "request").put("uri", "ssap://$uri").put("payload", payload).toString())
        return withTimeoutOrNull(5000) { deferred.await() }.also { pending.remove(id) }
    }

    private fun fire(uri: String, payload: JSONObject = JSONObject()) {
        val ws = main ?: return
        val id = "cmd_${ids.getAndIncrement()}"
        ws.send(JSONObject().put("id", id).put("type", "request").put("uri", "ssap://$uri").put("payload", payload).toString())
    }

    private fun button(name: String) {
        val p = pointer
        if (p != null) p.send("type:button\nname:$name\n\n") else fallbackButton(name)
    }

    private fun fallbackButton(name: String) {
        when (name) {
            "VOLUMEUP" -> fire("audio/volumeUp")
            "VOLUMEDOWN" -> fire("audio/volumeDown")
            "CHANNELUP" -> fire("tv/channelUp")
            "CHANNELDOWN" -> fire("tv/channelDown")
            "PLAY" -> fire("media.controls/play")
            "PAUSE" -> fire("media.controls/pause")
            "REWIND" -> fire("media.controls/rewind")
            "FASTFORWARD" -> fire("media.controls/fastForward")
        }
    }

    override suspend fun sendKey(key: RemoteKey) {
        when (key) {
            RemoteKey.POWER -> fire("system/turnOff")
            RemoteKey.INPUT -> fire("system.launcher/launch", JSONObject().put("id", "com.webos.app.inputpicker"))
            RemoteKey.PLAY_PAUSE -> {
                button(if (playing) "PAUSE" else "PLAY")
                playing = !playing
            }
            else -> button(
                when (key) {
                    RemoteKey.UP -> "UP"
                    RemoteKey.DOWN -> "DOWN"
                    RemoteKey.LEFT -> "LEFT"
                    RemoteKey.RIGHT -> "RIGHT"
                    RemoteKey.OK -> "ENTER"
                    RemoteKey.BACK -> "BACK"
                    RemoteKey.HOME -> "HOME"
                    RemoteKey.MENU -> "MENU"
                    RemoteKey.VOLUME_UP -> "VOLUMEUP"
                    RemoteKey.VOLUME_DOWN -> "VOLUMEDOWN"
                    RemoteKey.MUTE -> "MUTE"
                    RemoteKey.CHANNEL_UP -> "CHANNELUP"
                    RemoteKey.CHANNEL_DOWN -> "CHANNELDOWN"
                    RemoteKey.REWIND -> "REWIND"
                    RemoteKey.FAST_FORWARD -> "FASTFORWARD"
                    RemoteKey.INFO -> "INFO"
                    else -> RemoteKey.NUMBERS.indexOf(key).toString()
                },
            )
        }
    }

    override suspend fun sendText(text: String) {
        if (text.isNotEmpty()) fire("com.webos.service.ime/insertText", JSONObject().put("text", text).put("replace", 0))
    }

    override suspend fun sendBackspace() = fire("com.webos.service.ime/deleteCharacters", JSONObject().put("count", 1))

    override suspend fun sendEnter() = fire("com.webos.service.ime/sendEnterKey")

    override suspend fun launchApp(app: StreamingApp) {
        val id = when (app) {
            StreamingApp.YOUTUBE -> "youtube.leanback.v4"
            StreamingApp.NETFLIX -> "netflix"
            StreamingApp.PRIME_VIDEO -> "amazon"
            StreamingApp.DISNEY_PLUS -> "com.disney.disneyplus-prod"
        }
        fire("system.launcher/launch", JSONObject().put("id", id))
    }

    override fun disconnect() {
        val m = main
        val p = pointer
        main = null
        pointer = null
        registered?.complete(false)
        pending.values.forEach { it.cancel() }
        pending.clear()
        runCatching { p?.close(1000, null) }
        runCatching { m?.close(1000, null) }
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            _state.value = ConnectionState.Idle
        }
    }
}
