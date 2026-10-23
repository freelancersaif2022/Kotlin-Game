package com.voxcast.mic.audio

import android.content.Context
import com.voxcast.mic.net.ConnectionInfo
import com.voxcast.mic.net.HostMode
import com.voxcast.mic.net.NetworkUtils
import com.voxcast.mic.net.Protocol
import com.voxcast.mic.net.WifiDirectHost
import com.voxcast.mic.service.LinkStats
import com.voxcast.mic.service.Role
import com.voxcast.mic.service.StreamController
import com.voxcast.mic.service.StreamEvent
import com.voxcast.mic.service.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference

/** The "box" phone: hosts the session, shows the QR code and plays incoming voice. */
class SpeakerEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val speakerName: String,
) {
    private var server: ServerSocket? = null
    private var udp: DatagramSocket? = null
    private var p2p: WifiDirectHost? = null
    private val activeClient = AtomicReference<Socket?>(null)
    private var acceptJob: Job? = null
    @Volatile private var running = false
    private lateinit var info: ConnectionInfo

    suspend fun start(mode: HostMode) {
        running = true
        var ssid: String? = null
        var pass: String? = null
        val hosts = mutableListOf<String>()

        if (mode == HostMode.WIFI_DIRECT) {
            val host = WifiDirectHost(context)
            p2p = host
            val group = host.start().getOrElse {
                fail("Wi-Fi Direct failed", it.message ?: "Could not create the group")
                return
            }
            ssid = group.ssid
            pass = group.passphrase
            hosts += group.ownerIp
        }
        if (!running) return

        NetworkUtils.localLinks().forEach { if (it.ip !in hosts) hosts += it.ip }
        if (hosts.isEmpty()) {
            fail("No local network", "Connect this phone to Wi-Fi, turn on its hotspot, or choose Wi-Fi Direct.")
            return
        }

        try {
            server = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(0))
            }
            udp = DatagramSocket(null).apply {
                reuseAddress = true
                receiveBufferSize = 256 * 1024
                soTimeout = 400
                bind(InetSocketAddress(0))
            }
        } catch (e: Exception) {
            fail("Could not open speaker", e.message ?: "Network error")
            return
        }

        info = ConnectionInfo(
            speakerName = speakerName,
            hosts = hosts,
            port = server!!.localPort,
            token = ConnectionInfo.randomToken(),
            mode = mode,
            ssid = ssid,
            passphrase = pass,
        )
        StreamController.update(StreamState.SpeakerWaiting(info))
        acceptJob = scope.launch(Dispatchers.IO) { acceptLoop(server!!) }
    }

    fun refreshToken() {
        if (!running || !::info.isInitialized || activeClient.get() != null) return
        info = info.copy(token = ConnectionInfo.randomToken())
        StreamController.update(StreamState.SpeakerWaiting(info))
    }

    fun stop() {
        running = false
        activeClient.getAndSet(null)?.let { s ->
            runCatching { s.getOutputStream().write((Protocol.BYE + "\n").toByteArray()); s.getOutputStream().flush() }
            runCatching { s.close() }
        }
        runCatching { server?.close() }
        runCatching { udp?.close() }
        acceptJob?.cancel()
        p2p?.stop()
        server = null; udp = null; p2p = null
    }

    private fun fail(title: String, message: String) {
        running = false
        StreamController.update(StreamState.Failed(Role.SPEAKER, title, message))
    }

    private fun acceptLoop(srv: ServerSocket) {
        while (running) {
            val socket = try { srv.accept() } catch (_: Exception) { break }
            scope.launch(Dispatchers.IO) { handleConnection(socket) }
        }
    }

    private suspend fun handleConnection(socket: Socket) {
        var claimed = false
        var micName = "Microphone"
        var heartbeat: Job? = null
        var receiver: AudioReceiver? = null
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = 4_000
            val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            val writer = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)

            // Parallel connection attempts from the mic that lost the race close without a HELLO.
            val hello = Protocol.split(reader.readLine() ?: return)
            if (hello.size < 5 || hello[0] != Protocol.HELLO) return
            if (hello[2] != info.token) {
                send(writer, Protocol.line(Protocol.REJECT, "This QR code has expired. Scan the new code"))
                return
            }
            if (!activeClient.compareAndSet(null, socket)) {
                send(writer, Protocol.line(Protocol.REJECT, "Speaker is already connected to another microphone"))
                return
            }
            claimed = true
            micName = hello[3].ifBlank { "Microphone" }
            val sampleRate = hello[4].toIntOrNull()?.takeIf { it in 8_000..48_000 } ?: 24_000
            val sessionId = SecureRandom().nextInt()
            val dgram = udp ?: return

            receiver = AudioReceiver(sampleRate, sessionId, dgram).also { it.start() }
            send(writer, Protocol.line(Protocol.WELCOME, dgram.localPort, sessionId, speakerName))
            socket.soTimeout = Protocol.PEER_TIMEOUT_MS
            StreamController.stats(LinkStats())
            StreamController.update(StreamState.SpeakerLive(info, micName, System.currentTimeMillis(), talking = true))
            StreamController.emit(StreamEvent.Success("$micName connected"))

            heartbeat = scope.launch(Dispatchers.IO) {
                while (isActive && running) {
                    runCatching { send(writer, Protocol.line(Protocol.PING, System.nanoTime())) }
                        .onFailure { runCatching { socket.close() } }
                    delay(Protocol.HEARTBEAT_MS)
                }
            }

            while (running) {
                val parts = Protocol.split(reader.readLine() ?: break)
                when (parts[0]) {
                    Protocol.PING -> send(writer, Protocol.line(Protocol.PONG, parts.getOrElse(1) { "0" }))
                    Protocol.PONG -> parts.getOrNull(1)?.toLongOrNull()?.let { t ->
                        val rtt = ((System.nanoTime() - t) / 1_000_000).toInt()
                        StreamController.stats(StreamController.stats.value.copy(rttMs = rtt))
                    }
                    Protocol.TALK -> {
                        val current = StreamController.state.value
                        if (current is StreamState.SpeakerLive) {
                            StreamController.update(current.copy(talking = parts.getOrNull(1) == "1"))
                        }
                    }
                    Protocol.BYE -> break
                }
            }
        } catch (_: Exception) {
            // Timeout or reset: treated as a disconnect below.
        } finally {
            heartbeat?.cancel()
            receiver?.stop()
            runCatching { socket.close() }
            if (claimed) {
                activeClient.compareAndSet(socket, null)
                if (running) {
                    StreamController.update(StreamState.SpeakerWaiting(info))
                    StreamController.emit(StreamEvent.Warning("$micName disconnected"))
                }
            }
        }
    }

    private fun send(writer: BufferedWriter, line: String) {
        synchronized(writer) {
            writer.write(line)
            writer.write("\n")
            writer.flush()
        }
    }
}
