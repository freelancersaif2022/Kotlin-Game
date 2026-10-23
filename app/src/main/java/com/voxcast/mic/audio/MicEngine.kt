package com.voxcast.mic.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Network
import android.os.Process
import com.voxcast.mic.data.AppSettings
import com.voxcast.mic.data.NoiseMode
import com.voxcast.mic.net.ConnectionInfo
import com.voxcast.mic.net.HostMode
import com.voxcast.mic.net.NetworkUtils
import com.voxcast.mic.net.Protocol
import com.voxcast.mic.net.WifiDirectJoiner
import com.voxcast.mic.service.LinkStats
import com.voxcast.mic.service.Role
import com.voxcast.mic.service.StreamController
import com.voxcast.mic.service.StreamEvent
import com.voxcast.mic.service.StreamState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/** The phone you talk into: captures, cleans and streams voice to the speaker. */
class MicEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: StateFlow<AppSettings>,
) {
    @Volatile private var running = false
    private var socket: Socket? = null
    private var udp: DatagramSocket? = null
    private var captureThread: Thread? = null
    private val joiner = WifiDirectJoiner(context)
    private var network: Network? = null
    private var everConnected = false

    suspend fun start(payload: String) {
        running = true
        val info = ConnectionInfo.parse(payload) ?: run {
            fail("Invalid QR code", "This isn't a VoxCast speaker code. Open VoxCast on the other phone and tap Speaker.")
            return
        }
        val name = info.speakerName
        StreamController.update(StreamState.MicConnecting(name, "Checking network", 1))

        var hosts = info.hosts
        if (info.mode == HostMode.WIFI_DIRECT && info.ssid != null && info.passphrase != null) {
            // If both phones already share a Wi-Fi/hotspot, use it and skip joining the group.
            val lanHosts = info.hosts.filterNot { it.startsWith("192.168.49.") }
            val lanSocket = if (lanHosts.isNotEmpty() && NetworkUtils.localLinks().isNotEmpty()) {
                connectFirst(lanHosts, info.port, null, 1_500)
            } else null
            if (lanSocket != null) {
                runCatching { lanSocket.close() }
                hosts = lanHosts
            } else if (joiner.canJoinAutomatically) {
                StreamController.update(StreamState.MicConnecting(name, "Joining ${info.ssid}", 2))
                network = joiner.join(info.ssid, info.passphrase).getOrElse {
                    fail("Couldn't join Wi-Fi Direct", it.message ?: "Join was cancelled")
                    return
                }
            } else {
                StreamController.update(
                    StreamState.MicConnecting(name, "Join this Wi-Fi in Settings", 2, info.ssid, info.passphrase)
                )
                val joined = withTimeoutOrNull(180_000) {
                    while (running && NetworkUtils.localLinks().none { it.ip.startsWith("192.168.49.") }) delay(1_000)
                    true
                }
                if (joined == null) {
                    fail("Not joined", "Join ${info.ssid} from Wi-Fi settings, then scan again.")
                    return
                }
            }
        } else if (NetworkUtils.localLinks().isEmpty()) {
            fail("No local network", "Connect to the same Wi-Fi as the speaker, or join its hotspot.")
            return
        }
        if (!running) return

        var attempt = 0
        while (running) {
            val established = runSession(info, hosts)
            if (!running) break
            if (!everConnected) {
                fail(
                    "Speaker not reachable",
                    "Make sure both phones are on the same Wi-Fi or hotspot, and the speaker screen is open.",
                )
                break
            }
            attempt = if (established) 1 else attempt + 1
            if (attempt > MAX_RECONNECTS) {
                fail("Connection lost", "The speaker went out of range. Move closer and reconnect.")
                break
            }
            StreamController.update(StreamState.MicReconnecting(name, attempt))
            delay(600L * attempt)
        }
    }

    fun stop() {
        running = false
        socket?.let { s ->
            runCatching { s.getOutputStream().write((Protocol.BYE + "\n").toByteArray()); s.getOutputStream().flush() }
            runCatching { s.close() }
        }
        runCatching { udp?.close() }
        captureThread?.interrupt()
        runCatching { captureThread?.join(500) }
        captureThread = null
        joiner.release()
        socket = null; udp = null
        StreamController.level(0f)
    }

    private fun fail(title: String, message: String) {
        running = false
        StreamController.update(StreamState.Failed(Role.MIC, title, message))
    }

    /** Returns true if the session was established before it ended. */
    private suspend fun runSession(info: ConnectionInfo, hosts: List<String>): Boolean {
        if (!everConnected) StreamController.update(StreamState.MicConnecting(info.speakerName, "Reaching speaker", 3))
        val s = connectFirst(hosts, info.port, network, 3_000) ?: return false
        socket = s
        var heartbeat: Job? = null
        var talkWatcher: Job? = null
        try {
            s.tcpNoDelay = true
            s.soTimeout = 5_000
            val reader = s.getInputStream().bufferedReader(Charsets.UTF_8)
            val writer = s.getOutputStream().bufferedWriter(Charsets.UTF_8)
            val current = settings.value
            val sampleRate = current.quality.sampleRate
            send(writer, Protocol.line(Protocol.HELLO, ConnectionInfo.PROTOCOL_VERSION, info.token, current.deviceName, sampleRate))

            val reply = Protocol.split(reader.readLine() ?: return false)
            if (reply[0] == Protocol.REJECT) {
                fail("Connection refused", reply.getOrElse(1) { "The speaker refused the connection" })
                return false
            }
            if (reply[0] != Protocol.WELCOME || reply.size < 4) return false
            val udpPort = reply[1].toIntOrNull() ?: return false
            val sessionId = reply[2].toIntOrNull() ?: return false
            val speakerName = reply[3].ifBlank { info.speakerName }

            if (!everConnected) StreamController.update(StreamState.MicConnecting(speakerName, "Securing audio link", 4))
            val dgram = DatagramSocket().apply { sendBufferSize = 128 * 1024 }
            network?.bindSocket(dgram)
            dgram.connect(InetSocketAddress(s.inetAddress, udpPort))
            udp = dgram

            s.soTimeout = Protocol.PEER_TIMEOUT_MS
            StreamController.stats(LinkStats())
            if (!everConnected) delay(450) // let the connecting animation finish its last step
            everConnected = true
            StreamController.update(StreamState.MicLive(speakerName, System.currentTimeMillis(), sampleRate))
            StreamController.emit(StreamEvent.Success("Connected to $speakerName"))

            captureThread = Thread({ captureLoop(dgram, sessionId, sampleRate) }, "vox-capture").apply { start() }

            heartbeat = scope.launch(Dispatchers.IO) {
                while (isActive && running) {
                    runCatching { send(writer, Protocol.line(Protocol.PING, System.nanoTime())) }
                        .onFailure { runCatching { s.close() } }
                    delay(Protocol.HEARTBEAT_MS)
                }
            }
            talkWatcher = scope.launch(Dispatchers.IO) {
                StreamController.onAir.collect { on ->
                    runCatching { send(writer, Protocol.line(Protocol.TALK, if (on) 1 else 0)) }
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
                    Protocol.BYE -> {
                        StreamController.emit(StreamEvent.Info("$speakerName ended the session"))
                        running = false
                        StreamController.update(StreamState.Idle)
                        StreamController.emit(StreamEvent.SessionEnded(Role.MIC))
                    }
                }
            }
            return true
        } catch (_: Exception) {
            return everConnected
        } finally {
            heartbeat?.cancel()
            talkWatcher?.cancel()
            captureThread?.interrupt()
            runCatching { captureThread?.join(300) }
            captureThread = null
            runCatching { udp?.close() }
            runCatching { s.close() }
            udp = null
            socket = null
        }
    }

    @SuppressLint("MissingPermission") // RECORD_AUDIO is requested before the session starts.
    private fun captureLoop(dgram: DatagramSocket, sessionId: Int, sampleRate: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val cfg = settings.value
        val frameSamples = sampleRate * Protocol.FRAME_MS / 1000
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val source = if (cfg.noiseMode == NoiseMode.OFF && !cfg.echoCancel) {
            MediaRecorder.AudioSource.MIC
        } else {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        }
        val record = try {
            AudioRecord(source, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, frameSamples * 2 * 4))
        } catch (e: Exception) {
            StreamController.emit(StreamEvent.Error("Microphone unavailable"))
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            StreamController.emit(StreamEvent.Error("Microphone is in use by another app"))
            return
        }

        val session = record.audioSessionId
        val ns = if (cfg.noiseMode != NoiseMode.OFF && NoiseSuppressor.isAvailable()) {
            runCatching { NoiseSuppressor.create(session)?.apply { enabled = true } }.getOrNull()
        } else null
        val aec = if (cfg.echoCancel && AcousticEchoCanceler.isAvailable()) {
            runCatching { AcousticEchoCanceler.create(session)?.apply { enabled = true } }.getOrNull()
        } else null
        val agc = if (AutomaticGainControl.isAvailable()) {
            runCatching { AutomaticGainControl.create(session)?.apply { enabled = cfg.autoGain } }.getOrNull()
        } else null

        val processor = NoiseProcessor(sampleRate)
        val frame = ShortArray(frameSamples)
        val packetBytes = ByteArray(Protocol.HEADER_SIZE + frameSamples * 2)
        val bb = ByteBuffer.wrap(packetBytes).order(ByteOrder.LITTLE_ENDIAN)
        bb.put(0, Protocol.MAGIC)
        bb.put(1, 0)
        bb.putInt(2, sessionId)
        val packet = DatagramPacket(packetBytes, packetBytes.size)
        var seq = 0
        var tick = 0
        var smoothLevel = 0f
        val warmupFrames = AtomicInteger(15) // skip the first 150 ms of mic start-up click

        try {
            record.startRecording()
            while (running && !Thread.currentThread().isInterrupted) {
                var filled = 0
                while (filled < frameSamples) {
                    val n = record.read(frame, filled, frameSamples - filled)
                    if (n <= 0) break
                    filled += n
                }
                if (filled < frameSamples) {
                    if (!running) break
                    continue
                }

                val live = settings.value
                processor.mode = live.noiseMode
                processor.gainDb = live.micGainDb
                processor.voiceEnhance = live.voiceEnhance

                val level = processor.process(frame, frameSamples)
                if (warmupFrames.get() > 0) {
                    warmupFrames.decrementAndGet()
                    frame.fill(0)
                }
                smoothLevel = max(level, smoothLevel * 0.8f)
                if (++tick % 2 == 0) StreamController.level(smoothLevel)

                if (StreamController.onAir.value) {
                    bb.putInt(6, seq++)
                    for (i in 0 until frameSamples) bb.putShort(Protocol.HEADER_SIZE + i * 2, frame[i])
                    try { dgram.send(packet) } catch (_: Exception) { if (dgram.isClosed) break }
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { record.stop() }
            record.release()
            ns?.release(); aec?.release(); agc?.release()
            StreamController.level(0f)
        }
    }

    private fun send(writer: BufferedWriter, line: String) {
        synchronized(writer) {
            writer.write(line)
            writer.write("\n")
            writer.flush()
        }
    }

    /** Tries every address in the QR code at once and keeps whichever answers first. */
    private suspend fun connectFirst(hosts: List<String>, port: Int, net: Network?, timeoutMs: Int): Socket? {
        if (hosts.isEmpty()) return null
        val result = CompletableDeferred<Socket?>()
        val remaining = AtomicInteger(hosts.size)
        for (host in hosts) {
            Thread {
                val s = Socket()
                try {
                    net?.bindSocket(s)
                    s.connect(InetSocketAddress(host, port), timeoutMs)
                    if (!result.complete(s)) runCatching { s.close() }
                } catch (_: Exception) {
                    runCatching { s.close() }
                    if (remaining.decrementAndGet() == 0) result.complete(null)
                }
            }.start()
        }
        return withTimeoutOrNull(timeoutMs + 500L) { result.await() }
    }

    private companion object {
        const val MAX_RECONNECTS = 6
    }
}
