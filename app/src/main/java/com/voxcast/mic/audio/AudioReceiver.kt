package com.voxcast.mic.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import com.voxcast.mic.net.Protocol
import com.voxcast.mic.service.StreamController
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Receives UDP audio frames and plays them with a tiny adaptive jitter buffer
 * (20 ms pre-roll, hard cap of 80 ms) so latency stays low and never drifts.
 */
class AudioReceiver(
    private val sampleRate: Int,
    private val sessionId: Int,
    private val socket: DatagramSocket,
) {
    private class Frame(var data: ShortArray, var length: Int = 0)

    private val frameSamples = sampleRate * Protocol.FRAME_MS / 1000
    private val queue = ArrayBlockingQueue<Frame>(QUEUE_CAPACITY)
    private val pool = ArrayBlockingQueue<Frame>(QUEUE_CAPACITY + 8)

    @Volatile private var running = false
    private var receiveThread: Thread? = null
    private var playThread: Thread? = null

    @Volatile private var received = 0L
    @Volatile private var lost = 0L
    @Volatile private var bytes = 0L

    fun start() {
        running = true
        receiveThread = Thread(::receiveLoop, "vox-rx").apply { start() }
        playThread = Thread(::playLoop, "vox-play").apply { start() }
    }

    fun stop() {
        running = false
        receiveThread?.interrupt()
        playThread?.interrupt()
        runCatching { receiveThread?.join(500) }
        runCatching { playThread?.join(500) }
        receiveThread = null
        playThread = null
        queue.clear()
        StreamController.level(0f)
    }

    private fun obtain(): Frame = pool.poll() ?: Frame(ShortArray(frameSamples))
    private fun recycle(f: Frame) { pool.offer(f) }

    private fun receiveLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val buffer = ByteArray(4096)
        val packet = DatagramPacket(buffer, buffer.size)
        val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        var lastSeq = -1
        var windowStart = System.currentTimeMillis()
        var windowReceived = 0L
        var windowLost = 0L
        var windowBytes = 0L

        while (running) {
            try {
                packet.length = buffer.size
                socket.receive(packet)
            } catch (_: SocketTimeoutException) {
                continue
            } catch (_: Exception) {
                if (!running) break else continue
            }
            val len = packet.length
            if (len <= Protocol.HEADER_SIZE || buffer[0] != Protocol.MAGIC) continue
            if (bb.getInt(2) != sessionId) continue
            val seq = bb.getInt(6)
            if (lastSeq >= 0) {
                val delta = seq - lastSeq
                if (delta <= 0 && delta > -10_000) continue // duplicate or late frame
                if (delta in 2..500) windowLost += (delta - 1)
            }
            lastSeq = seq

            val samples = (len - Protocol.HEADER_SIZE) / 2
            val frame = obtain()
            if (frame.data.size < samples) frame.data = ShortArray(samples)
            for (i in 0 until samples) frame.data[i] = bb.getShort(Protocol.HEADER_SIZE + i * 2)
            frame.length = samples
            if (!queue.offer(frame)) {
                queue.poll()?.let(::recycle)
                queue.offer(frame)
            }
            windowReceived++
            windowBytes += len

            val now = System.currentTimeMillis()
            if (now - windowStart >= 1_000) {
                val total = windowReceived + windowLost
                received += windowReceived; lost += windowLost; bytes += windowBytes
                StreamController.stats(
                    StreamController.stats.value.copy(
                        lossPercent = if (total > 0) windowLost * 100f / total else 0f,
                        kbps = (windowBytes * 8 / max(1L, now - windowStart)).toInt(),
                        bufferMs = queue.size * Protocol.FRAME_MS,
                    )
                )
                windowStart = now; windowReceived = 0; windowLost = 0; windowBytes = 0
            }
        }
    }

    private fun playLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuf, frameSamples * 2 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
            .build()

        try {
            track.play()
            var prebuffering = true
            var levelTick = 0
            var smoothLevel = 0f
            while (running) {
                if (prebuffering) {
                    if (queue.size < PREROLL_FRAMES) {
                        Thread.sleep(2)
                        continue
                    }
                    prebuffering = false
                }
                val frame = queue.poll(UNDERRUN_MS, TimeUnit.MILLISECONDS)
                if (frame == null) {
                    prebuffering = true
                    smoothLevel *= 0.5f
                    StreamController.level(smoothLevel)
                    continue
                }
                // Keep latency bounded: drop the oldest audio if the network delivered a burst.
                while (queue.size > MAX_BUFFER_FRAMES) queue.poll()?.let(::recycle)

                val gain = StreamController.speakerVolume
                var sumSq = 0.0
                val data = frame.data
                for (i in 0 until frame.length) {
                    var s = data[i] * gain
                    if (abs(s) > 30_000f) s = softClip(s)
                    val v = s.toInt().coerceIn(-32768, 32767)
                    data[i] = v.toShort()
                    sumSq += v.toDouble() * v
                }
                track.write(data, 0, frame.length)
                recycle(frame)

                val rms = sqrt(sumSq / max(1, frame.length)).toFloat() / 32768f
                smoothLevel = max(min(1f, rms * 3.2f), smoothLevel * 0.82f)
                if (++levelTick % 2 == 0) StreamController.level(smoothLevel)
            }
        } catch (_: InterruptedException) {
        } catch (_: Exception) {
        } finally {
            runCatching { track.pause(); track.flush() }
            runCatching { track.release() }
        }
    }

    private fun softClip(s: Float): Float {
        val sign = if (s < 0) -1f else 1f
        val a = abs(s)
        val over = a - 30_000f
        return sign * (30_000f + 2_767f * (over / (over + 2_767f)))
    }

    private companion object {
        const val QUEUE_CAPACITY = 48
        const val PREROLL_FRAMES = 2
        const val MAX_BUFFER_FRAMES = 8
        const val UNDERRUN_MS = 60L
    }
}
