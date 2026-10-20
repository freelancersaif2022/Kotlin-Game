package com.tvremote.universal.tv

import com.tvremote.universal.core.CertPins
import com.tvremote.universal.core.ClientIdentity
import com.tvremote.universal.core.ProtoMessage
import com.tvremote.universal.core.ProtoWriter
import com.tvremote.universal.core.Tls
import com.tvremote.universal.core.isCertificateChange
import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket

/**
 * Android TV / Google TV "Remote Control v2" protocol (the same protocol used by the
 * Google TV app): pairing on TLS port 6467, remote control on TLS port 6466, framed protobuf.
 */
object AndroidTvProtocol {
    const val PAIRING_PORT = 6467
    const val REMOTE_PORT = 6466

    // OuterMessage (polo.proto)
    const val STATUS_OK = 200
    const val STATUS_BAD_SECRET = 402

    // RemoteMessage feature flags
    const val FEATURE_PING = 1
    const val FEATURE_KEY = 2
    const val FEATURE_IME = 4
    const val FEATURE_POWER = 32
    const val FEATURE_VOLUME = 64
    const val FEATURE_APP_LINK = 512
    const val ACTIVE_FEATURES = FEATURE_PING or FEATURE_KEY or FEATURE_IME or FEATURE_POWER or FEATURE_VOLUME or FEATURE_APP_LINK

    const val DIRECTION_SHORT = 3

    fun keyCode(key: RemoteKey): Int = when (key) {
        RemoteKey.POWER -> 26
        RemoteKey.UP -> 19
        RemoteKey.DOWN -> 20
        RemoteKey.LEFT -> 21
        RemoteKey.RIGHT -> 22
        RemoteKey.OK -> 23
        RemoteKey.BACK -> 4
        RemoteKey.HOME -> 3
        RemoteKey.MENU -> 82
        RemoteKey.VOLUME_UP -> 24
        RemoteKey.VOLUME_DOWN -> 25
        RemoteKey.MUTE -> 164
        RemoteKey.CHANNEL_UP -> 166
        RemoteKey.CHANNEL_DOWN -> 167
        RemoteKey.INPUT -> 178
        RemoteKey.PLAY_PAUSE -> 85
        RemoteKey.REWIND -> 89
        RemoteKey.FAST_FORWARD -> 90
        RemoteKey.INFO -> 165
        else -> 7 + RemoteKey.NUMBERS.indexOf(key) // KEYCODE_0..KEYCODE_9
    }

    /** Android key codes for typing when the TV has no active input field session. */
    fun charKeyCode(c: Char): Int? = when (c) {
        in 'a'..'z' -> 29 + (c - 'a')
        in 'A'..'Z' -> 29 + (c - 'A')
        in '0'..'9' -> 7 + (c - '0')
        ' ' -> 62
        '.' -> 56
        ',' -> 55
        '@' -> 77
        '-' -> 69
        '/' -> 76
        '\'' -> 75
        '=' -> 70
        '+' -> 81
        ';' -> 74
        else -> null
    }

    fun appLink(app: StreamingApp): String = when (app) {
        StreamingApp.YOUTUBE -> "https://www.youtube.com"
        StreamingApp.NETFLIX -> "https://www.netflix.com/title"
        StreamingApp.PRIME_VIDEO -> "https://app.primevideo.com"
        StreamingApp.DISNEY_PLUS -> "https://www.disneyplus.com"
    }

    fun outer(block: ProtoWriter.() -> Unit): ByteArray = ProtoWriter.build {
        int(1, 2) // protocol_version
        int(2, STATUS_OK) // status
        block()
    }
}

class PairingException(message: String, val wrongCode: Boolean = false) : IOException(message)

/** Pairing session. TV shows a 6 character hex code; the secret is derived from both certificates. */
class AndroidTvPairing(
    private val host: String,
    private val identity: ClientIdentity,
    private val pins: CertPins,
) {
    private var socket: SSLSocket? = null

    suspend fun start(clientName: String) = withContext(Dispatchers.IO) {
        pins.clearHost(host)
        val s = Tls.connect(host, AndroidTvProtocol.PAIRING_PORT, pins, identity.keyManager(), timeoutMs = 6000)
        s.soTimeout = 15000
        socket = s
        send(AndroidTvProtocol.outer {
            message(10) { // pairing_request
                string(1, "atvremote")
                string(2, clientName)
            }
        })
        expect(11) // pairing_request_ack
        send(AndroidTvProtocol.outer {
            message(20) { // options
                message(1) { int(1, 3); int(2, 6) } // input encoding: hexadecimal, 6 symbols
                int(3, 1) // preferred role: input
            }
        })
        expect(20) // options
        send(AndroidTvProtocol.outer {
            message(30) { // configuration
                message(1) { int(1, 3); int(2, 6) }
                int(2, 1) // client role: input
            }
        })
        expect(31) // configuration_ack -> TV now shows the code
    }

    /** Returns false when the code does not match locally (TV keeps waiting, user can retry). */
    suspend fun finish(code: String): Boolean = withContext(Dispatchers.IO) {
        val s = socket ?: throw PairingException("Pairing session closed")
        val clean = code.trim().uppercase()
        require(clean.length == 6)
        val clientKey = identity.publicKey
        val serverKey = s.session.peerCertificates.first().publicKey as RSAPublicKey
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(clientKey.modulus.magnitude())
        digest.update(clientKey.publicExponent.magnitude())
        digest.update(serverKey.modulus.magnitude())
        digest.update(serverKey.publicExponent.magnitude())
        digest.update(hexToBytes(clean.substring(2)))
        val hash = digest.digest()
        if ((hash[0].toInt() and 0xFF) != clean.substring(0, 2).toInt(16)) return@withContext false
        send(AndroidTvProtocol.outer { message(40) { bytes(1, hash) } })
        expect(41)
        true
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun send(bytes: ByteArray) {
        val s = socket ?: throw PairingException("Pairing session closed")
        ProtoWriter.writeDelimited(s.outputStream, bytes)
    }

    private fun expect(field: Int) {
        val s = socket ?: throw PairingException("Pairing session closed")
        val msg = ProtoMessage.parse(ProtoMessage.readDelimited(s.inputStream))
        val status = msg.int(2)
        if (status == AndroidTvProtocol.STATUS_BAD_SECRET) throw PairingException("Incorrect code", wrongCode = true)
        if (status != AndroidTvProtocol.STATUS_OK) throw PairingException("The TV rejected pairing (status $status)")
        if (!msg.has(field)) throw PairingException("Unexpected response from TV")
    }

    private fun BigInteger.magnitude(): ByteArray {
        val b = toByteArray()
        var i = 0
        while (i < b.size - 1 && b[i] == 0.toByte()) i++
        return b.copyOfRange(i, b.size)
    }

    private fun hexToBytes(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

class AndroidTvController(
    device: TvDevice,
    private val identity: () -> ClientIdentity,
    private val pins: CertPins,
    onDeviceChanged: (TvDevice) -> Unit,
) : BaseController(device, onDeviceChanged) {

    override val capabilities = Capabilities(
        keys = RemoteKey.entries.toSet(),
        text = true,
        backspace = true,
        apps = StreamingApp.entries.toSet(),
    )

    @Volatile private var socket: SSLSocket? = null
    @Volatile private var out: OutputStream? = null
    private var readJob: Job? = null
    private val writeLock = Any()
    @Volatile private var imeCounter = 0
    @Volatile private var imeFieldCounter = 0
    @Volatile private var imeKnown = false

    override suspend fun connect() {
        disconnect()
        _state.value = ConnectionState.Connecting()
        try {
            val s = withContext(Dispatchers.IO) {
                Tls.connect(device.host, AndroidTvProtocol.REMOTE_PORT, pins, identity().keyManager(), timeoutMs = 5000)
            }
            s.soTimeout = 25_000 // the TV pings every ~5 s
            socket = s
            out = s.outputStream
            readJob = scope.launch { readLoop(s) }
            // The TV sends remote_start right after configuration; assume on if it's quiet.
            val settled = awaitSettled(6000)
            if (settled is ConnectionState.Connecting && socket === s) {
                _state.value = ConnectionState.Connected(tvOn = true)
            }
        } catch (e: Exception) {
            disconnect()
            fail(classify(e), message(e))
        }
    }

    private fun classify(e: Exception): FailReason = when {
        e.isCertificateChange() -> FailReason.CERT_CHANGED
        e is SSLException -> FailReason.NEEDS_PAIRING
        else -> FailReason.UNREACHABLE
    }

    private fun message(e: Exception): String = when (classify(e)) {
        FailReason.CERT_CHANGED -> Messages.CERT_CHANGED
        FailReason.NEEDS_PAIRING -> "This TV needs to be paired again."
        else -> if (e is ConnectException || e is SocketTimeoutException || e is NoRouteToHostException) Messages.UNREACHABLE else Messages.UNREACHABLE
    }

    private fun readLoop(s: SSLSocket) {
        try {
            val input = s.inputStream
            while (true) {
                val msg = ProtoMessage.parse(ProtoMessage.readDelimited(input))
                handle(msg)
            }
        } catch (e: Exception) {
            if (socket === s) {
                disconnect()
                // A handshake-time rejection means the TV forgot us.
                fail(if (e is SSLException) FailReason.NEEDS_PAIRING else FailReason.UNREACHABLE, if (e is SSLException) "This TV needs to be paired again." else Messages.LOST)
            }
        }
    }

    private fun handle(msg: ProtoMessage) {
        when {
            msg.has(1) -> { // remote_configure
                val supported = msg.message(1)?.int(1) ?: AndroidTvProtocol.ACTIVE_FEATURES
                val active = AndroidTvProtocol.ACTIVE_FEATURES and supported
                activeFeatures = active
                send(ProtoWriter.build {
                    message(1) {
                        int(1, active)
                        message(2) {
                            int(3, 1)
                            string(4, "1")
                            string(5, "atvremote")
                            string(6, "1.0.0")
                        }
                    }
                })
            }
            msg.has(2) -> send(ProtoWriter.build { message(2) { int(1, activeFeatures) } }) // remote_set_active
            msg.has(8) -> { // remote_ping_request
                val v = msg.message(8)?.int(1) ?: 0
                send(ProtoWriter.build { message(9) { int(1, v) } })
            }
            msg.has(21) -> { // remote_ime_batch_edit
                msg.message(21)?.let {
                    imeCounter = it.int(1)
                    imeFieldCounter = it.int(2)
                    imeKnown = true
                }
            }
            msg.has(40) -> { // remote_start
                val started = msg.message(40)?.bool(1) ?: true
                _state.value = ConnectionState.Connected(tvOn = started)
            }
        }
    }

    @Volatile private var activeFeatures = AndroidTvProtocol.ACTIVE_FEATURES

    private fun send(bytes: ByteArray) {
        val o = out ?: return
        try {
            synchronized(writeLock) { ProtoWriter.writeDelimited(o, bytes) }
        } catch (e: IOException) {
            if (out === o) {
                disconnect()
                fail(FailReason.UNREACHABLE, Messages.LOST)
            }
        }
    }

    private fun sendKeyCode(code: Int) {
        send(ProtoWriter.build {
            message(10) {
                int(1, code)
                int(2, AndroidTvProtocol.DIRECTION_SHORT)
            }
        })
    }

    override suspend fun sendKey(key: RemoteKey) = withContext(Dispatchers.IO) {
        sendKeyCode(AndroidTvProtocol.keyCode(key))
    }

    override suspend fun sendText(text: String) = withContext(Dispatchers.IO) {
        if (text.isEmpty()) return@withContext
        if (imeKnown) {
            send(ProtoWriter.build {
                message(21) {
                    int(1, imeCounter)
                    int(2, imeFieldCounter)
                    message(3) {
                        int(1, 1)
                        message(2) {
                            int(1, text.length - 1)
                            int(2, text.length - 1)
                            string(3, text)
                        }
                    }
                }
            })
        } else {
            text.forEach { c -> AndroidTvProtocol.charKeyCode(c)?.let { sendKeyCode(it) } }
        }
    }

    override suspend fun sendBackspace() = withContext(Dispatchers.IO) { sendKeyCode(67) }

    override suspend fun sendEnter() = withContext(Dispatchers.IO) { sendKeyCode(66) }

    override suspend fun launchApp(app: StreamingApp) = withContext(Dispatchers.IO) {
        send(ProtoWriter.build { message(90) { string(1, AndroidTvProtocol.appLink(app)) } })
    }

    override fun disconnect() {
        val s = socket
        socket = null
        out = null
        readJob?.cancel()
        readJob = null
        runCatching { s?.close() }
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            _state.value = ConnectionState.Idle
        }
    }
}
