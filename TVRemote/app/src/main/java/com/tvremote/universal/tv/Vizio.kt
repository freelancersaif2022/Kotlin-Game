package com.tvremote.universal.tv

import com.tvremote.universal.core.CertPins
import com.tvremote.universal.core.Tls
import com.tvremote.universal.core.isCertificateChange
import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Vizio SmartCast HTTPS API (port 7345, older firmware 9000). Pairing uses a 4 digit PIN. */
object VizioApi {
    private val JSON = "application/json".toMediaType()

    class Response(val code: Int, val body: JSONObject)

    fun client(http: OkHttpClient, host: String, port: Int, pins: CertPins) = Tls.okHttp(http, host, port, pins)

    suspend fun findPort(http: OkHttpClient, host: String, pins: CertPins): Int? = withContext(Dispatchers.IO) {
        for (port in intArrayOf(7345, 9000)) {
            val ok = runCatching {
                client(http, host, port, pins).newCall(
                    Request.Builder().url("https://$host:$port/state/device/deviceinfo").build(),
                ).execute().use { true }
            }
            if (ok.getOrDefault(false)) return@withContext port
            if (ok.exceptionOrNull()?.isCertificateChange() == true) throw ok.exceptionOrNull()!!
        }
        null
    }

    suspend fun call(
        client: OkHttpClient,
        url: String,
        body: JSONObject?,
        auth: String? = null,
    ): Response = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url)
        auth?.let { builder.header("AUTH", it) }
        if (body != null) builder.put(body.toString().toRequestBody(JSON)) else builder.get()
        client.newCall(builder.build()).execute().use { resp ->
            Response(resp.code, runCatching { JSONObject(resp.body?.string().orEmpty()) }.getOrDefault(JSONObject()))
        }
    }

    /** Vizio responses use inconsistent key case. */
    fun JSONObject.ci(key: String): Any? {
        keys().forEach { if (it.equals(key, true)) return opt(it) }
        return null
    }

    fun result(body: JSONObject): String =
        ((body.ci("STATUS") as? JSONObject)?.ci("RESULT") as? String).orEmpty()

    fun newDeviceId() = UUID.randomUUID().toString()

    fun keyCode(key: RemoteKey): Pair<Int, Int>? = when (key) {
        RemoteKey.POWER -> 11 to 2
        RemoteKey.UP -> 3 to 8
        RemoteKey.DOWN -> 3 to 0
        RemoteKey.LEFT -> 3 to 1
        RemoteKey.RIGHT -> 3 to 7
        RemoteKey.OK -> 3 to 2
        RemoteKey.BACK -> 4 to 0
        RemoteKey.HOME -> 4 to 15
        RemoteKey.MENU -> 4 to 8
        RemoteKey.INFO -> 4 to 6
        RemoteKey.VOLUME_UP -> 5 to 1
        RemoteKey.VOLUME_DOWN -> 5 to 0
        RemoteKey.MUTE -> 5 to 4
        RemoteKey.CHANNEL_UP -> 8 to 1
        RemoteKey.CHANNEL_DOWN -> 8 to 0
        RemoteKey.INPUT -> 7 to 1
        RemoteKey.REWIND -> 2 to 1
        RemoteKey.FAST_FORWARD -> 2 to 0
        else -> null
    }

    fun app(app: StreamingApp): JSONObject = when (app) {
        StreamingApp.YOUTUBE -> JSONObject().put("APP_ID", "1").put("NAME_SPACE", 5)
        StreamingApp.NETFLIX -> JSONObject().put("APP_ID", "1").put("NAME_SPACE", 3)
        StreamingApp.PRIME_VIDEO -> JSONObject().put("APP_ID", "4").put("NAME_SPACE", 2)
        StreamingApp.DISNEY_PLUS -> JSONObject().put("APP_ID", "75").put("NAME_SPACE", 2)
    }.put("MESSAGE", JSONObject.NULL)
}

/** Vizio pairing session. */
class VizioPairing(private val http: OkHttpClient, private val host: String, private val pins: CertPins) {
    val deviceId = VizioApi.newDeviceId()
    var port = 7345
        private set
    private var token = 0L
    private var challenge = 1

    /** Asks the TV to show a PIN. */
    suspend fun start(): Boolean {
        pins.clearHost(host)
        port = VizioApi.findPort(http, host, pins) ?: return false
        val client = VizioApi.client(http, host, port, pins)
        val resp = VizioApi.call(
            client, "https://$host:$port/pairing/start",
            JSONObject().put("DEVICE_ID", deviceId).put("DEVICE_NAME", "Universal Remote"),
        )
        val item = with(VizioApi) { resp.body.ci("ITEM") } as? JSONObject ?: return false
        with(VizioApi) {
            token = (item.ci("PAIRING_REQ_TOKEN") as? Number)?.toLong() ?: 0L
            challenge = (item.ci("CHALLENGE_TYPE") as? Number)?.toInt() ?: 1
        }
        return true
    }

    /** Returns the auth token, or null if the PIN was wrong. */
    suspend fun finish(pin: String): String? {
        val client = VizioApi.client(http, host, port, pins)
        val resp = VizioApi.call(
            client, "https://$host:$port/pairing/pair",
            JSONObject()
                .put("DEVICE_ID", deviceId)
                .put("CHALLENGE_TYPE", challenge)
                .put("RESPONSE_VALUE", pin)
                .put("PAIRING_REQ_TOKEN", token),
        )
        val item = with(VizioApi) { resp.body.ci("ITEM") } as? JSONObject ?: return null
        return with(VizioApi) { item.ci("AUTH_TOKEN") as? String }?.takeIf { it.isNotBlank() }
    }
}

class VizioController(
    device: TvDevice,
    private val http: OkHttpClient,
    private val pins: CertPins,
    onDeviceChanged: (TvDevice) -> Unit,
) : BaseController(device, onDeviceChanged) {

    override val capabilities = Capabilities(
        keys = RemoteKey.entries.toSet() - RemoteKey.NUMBERS.toSet(),
        text = false,
        backspace = false,
        apps = StreamingApp.entries.toSet(),
    )

    private val port get() = device.credentials["port"]?.toIntOrNull() ?: 7345
    private val auth get() = device.credentials["authToken"]
    private val client by lazy { VizioApi.client(http, device.host, port, pins) }
    @Volatile private var playing = false

    private fun url(path: String) = "https://${device.host}:$port$path"

    override suspend fun connect() {
        _state.value = ConnectionState.Connecting()
        try {
            val resp = VizioApi.call(client, url("/state/device/power_mode"), null, auth)
            if (resp.code == 401 || resp.code == 403 || VizioApi.result(resp.body).contains("REQUIRES_PAIRING", true)) {
                fail(FailReason.NEEDS_PAIRING, "This Vizio TV needs to be paired again.")
                return
            }
            val items = with(VizioApi) { resp.body.ci("ITEMS") } as? JSONArray
            val value = (items?.optJSONObject(0)?.let { with(VizioApi) { it.ci("VALUE") } } as? Number)?.toInt()
            _state.value = ConnectionState.Connected(tvOn = value != 0)
        } catch (e: Exception) {
            if (e.isCertificateChange()) fail(FailReason.CERT_CHANGED, Messages.CERT_CHANGED)
            else fail(FailReason.UNREACHABLE, Messages.UNREACHABLE)
        }
    }

    private suspend fun press(codeset: Int, code: Int) {
        runCatching {
            VizioApi.call(
                client, url("/key_command/"),
                JSONObject().put(
                    "KEYLIST",
                    JSONArray().put(JSONObject().put("CODESET", codeset).put("CODE", code).put("ACTION", "KEYPRESS")),
                ),
                auth,
            )
        }.onFailure { if (isConnected) fail(FailReason.UNREACHABLE, Messages.LOST) }
    }

    override suspend fun sendKey(key: RemoteKey) {
        if (key == RemoteKey.PLAY_PAUSE) {
            press(2, if (playing) 2 else 3)
            playing = !playing
            return
        }
        VizioApi.keyCode(key)?.let { (set, code) -> press(set, code) }
    }

    override suspend fun launchApp(app: StreamingApp) {
        runCatching {
            VizioApi.call(client, url("/app/launch"), JSONObject().put("VALUE", VizioApi.app(app)), auth)
        }
    }

    override suspend fun wake() {
        press(11, 1)
    }

    override fun disconnect() {
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            _state.value = ConnectionState.Idle
        }
    }
}
