package com.netscope.wifianalyzer.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.random.Random

enum class SpeedPhase { IDLE, CONNECTING, PING, DOWNLOAD, UPLOAD, DONE, ERROR }

data class ServerInfo(val ip: String, val isp: String, val city: String, val country: String, val colo: String) {
    val serverLabel: String get() = listOf("Cloudflare", city.ifBlank { colo }).filter { it.isNotBlank() }.joinToString(" • ")
}

data class SpeedProgress(
    val phase: SpeedPhase = SpeedPhase.IDLE,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val liveMbps: Double = 0.0,
    val progress: Float = 0f,
    val samples: List<Float> = emptyList(),
    val server: ServerInfo? = null,
    val error: String? = null,
)

/**
 * Multi-stream speed test against Cloudflare's public speed test endpoints
 * (the same ones used by speed.cloudflare.com).
 */
class SpeedTestEngine {

    fun run(): Flow<SpeedProgress> = callbackFlow {
        val openConnections = Collections.synchronizedList(mutableListOf<HttpURLConnection>())
        val job = launch(Dispatchers.IO) {
            var state = SpeedProgress(phase = SpeedPhase.CONNECTING, progress = 0.02f)
            trySend(state)
            try {
                val server = fetchMeta()
                state = state.copy(server = server, phase = SpeedPhase.PING, progress = 0.04f)
                trySend(state)

                val (ping, jitter) = measurePing { p, samples ->
                    state = state.copy(progress = 0.04f + 0.08f * p, samples = samples)
                    trySend(state)
                }
                state = state.copy(pingMs = ping, jitterMs = jitter, phase = SpeedPhase.DOWNLOAD, samples = emptyList(), progress = 0.12f)
                trySend(state)

                val dl = measureThroughput(upload = false, streams = 4, durationMs = 10_000, openConnections) { p, live, samples ->
                    state = state.copy(liveMbps = live, progress = 0.12f + 0.44f * p, samples = samples)
                    trySend(state)
                }
                state = state.copy(downloadMbps = dl, liveMbps = 0.0, phase = SpeedPhase.UPLOAD, samples = emptyList(), progress = 0.56f)
                trySend(state)

                val ul = measureThroughput(upload = true, streams = 3, durationMs = 9_000, openConnections) { p, live, samples ->
                    state = state.copy(liveMbps = live, progress = 0.56f + 0.44f * p, samples = samples)
                    trySend(state)
                }
                state = state.copy(uploadMbps = ul, liveMbps = 0.0, phase = SpeedPhase.DONE, progress = 1f)
                trySend(state)
            } catch (e: Exception) {
                if (isActive) {
                    trySend(state.copy(phase = SpeedPhase.ERROR, error = friendlyError(e)))
                }
            }
            channel.close()
        }
        awaitClose {
            job.cancel()
            synchronized(openConnections) { openConnections.forEach { runCatching { it.disconnect() } } }
        }
    }.flowOn(Dispatchers.IO)

    private fun friendlyError(e: Exception): String = when (e) {
        is java.net.UnknownHostException -> "No internet connection. Check your Wi-Fi and try again."
        is java.net.SocketTimeoutException -> "The connection timed out. Your network may be too slow or blocked."
        is javax.net.ssl.SSLException -> "A secure connection could not be made. Check if the Wi-Fi needs a login page."
        else -> "The test could not finish. Please check your connection and try again."
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            useCaches = false
            setRequestProperty("User-Agent", "WiFiAnalyzer-Android")
            setRequestProperty("Accept-Encoding", "identity")
        }

    fun fetchMeta(): ServerInfo? = runCatching {
        val c = open("$BASE/meta")
        try {
            val o = JSONObject(c.inputStream.bufferedReader().readText())
            ServerInfo(
                ip = o.optString("clientIp"),
                isp = o.optString("asOrganization"),
                city = o.optString("city"),
                country = o.optString("country"),
                colo = o.optString("colo"),
            )
        } finally {
            c.disconnect()
        }
    }.getOrNull()

    private suspend fun measurePing(onProgress: (Float, List<Float>) -> Unit): Pair<Double, Double> {
        val samples = mutableListOf<Double>()
        val total = 14
        var failures = 0
        for (i in 0 until total) {
            if (!currentCoroutineContext().isActive) break
            try {
                val t0 = System.nanoTime()
                val c = open("$BASE/__down?bytes=0")
                val code = c.responseCode
                c.inputStream.use { it.readBytes() }
                val elapsed = (System.nanoTime() - t0) / 1e6
                val serverMs = c.getHeaderField("Server-Timing")
                    ?.let { Regex("dur=([0-9.]+)").find(it)?.groupValues?.get(1)?.toDoubleOrNull() } ?: 0.0
                // First request includes DNS + TLS handshake, so it is not a fair ping sample.
                if (i > 0 && code in 200..299) samples += (elapsed - serverMs).coerceAtLeast(1.0)
            } catch (e: IOException) {
                if (++failures >= 4) throw e
            }
            onProgress((i + 1f) / total, samples.map { it.toFloat() })
            delay(60)
        }
        if (samples.isEmpty()) throw IOException("ping failed")
        val sorted = samples.sorted()
        val ping = sorted[sorted.size / 2]
        val jitter = if (samples.size > 1) samples.zipWithNext { a, b -> abs(a - b) }.average() else 0.0
        return ping to jitter
    }

    private suspend fun measureThroughput(
        upload: Boolean,
        streams: Int,
        durationMs: Long,
        openConnections: MutableList<HttpURLConnection>,
        onProgress: (Float, Double, List<Float>) -> Unit,
    ): Double = coroutineScope {
        val bytes = AtomicLong(0)
        val start = System.currentTimeMillis()
        val deadline = start + durationMs
        val payload = ByteArray(64 * 1024).also { Random.nextBytes(it) }
        val failures = AtomicLong(0)

        val workers = (0 until streams).map {
            launch(Dispatchers.IO) {
                while (isActive && System.currentTimeMillis() < deadline) {
                    var c: HttpURLConnection? = null
                    try {
                        if (upload) {
                            val size = 8 * 1024 * 1024
                            c = open("$BASE/__up").apply {
                                requestMethod = "POST"
                                doOutput = true
                                setFixedLengthStreamingMode(size)
                                setRequestProperty("Content-Type", "application/octet-stream")
                            }
                            openConnections += c
                            c.outputStream.use { out ->
                                var sent = 0
                                while (sent < size && isActive && System.currentTimeMillis() < deadline) {
                                    val n = minOf(payload.size, size - sent)
                                    out.write(payload, 0, n)
                                    sent += n
                                    bytes.addAndGet(n.toLong())
                                }
                                if (sent < size) {
                                    c.disconnect()
                                    return@use
                                }
                            }
                            if (System.currentTimeMillis() < deadline) c.inputStream.use { it.readBytes() }
                        } else {
                            c = open("$BASE/__down?bytes=${50 * 1024 * 1024}")
                            openConnections += c
                            c.inputStream.use { input ->
                                val buf = ByteArray(64 * 1024)
                                while (isActive && System.currentTimeMillis() < deadline) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    bytes.addAndGet(n.toLong())
                                }
                            }
                        }
                    } catch (e: IOException) {
                        if (System.currentTimeMillis() >= deadline) break
                        if (failures.incrementAndGet() > streams * 3) break
                        delay(200)
                    } finally {
                        c?.let {
                            openConnections -= it
                            if (System.currentTimeMillis() >= deadline) runCatching { it.disconnect() }
                        }
                    }
                }
            }
        }

        // Sampler: live speed (smoothed) and the final average after a warm-up window.
        val warmupMs = 2_000L
        var lastBytes = 0L
        var lastT = start
        var ema = 0.0
        var warmBytes = -1L
        var warmT = 0L
        val samples = mutableListOf<Float>()
        while (System.currentTimeMillis() < deadline) {
            delay(200)
            val now = System.currentTimeMillis()
            val b = bytes.get()
            val dt = (now - lastT).coerceAtLeast(1)
            val inst = (b - lastBytes) * 8.0 / dt / 1000.0
            ema = if (ema == 0.0) inst else ema * 0.7 + inst * 0.3
            lastBytes = b
            lastT = now
            if (warmBytes < 0 && now - start >= warmupMs) {
                warmBytes = b
                warmT = now
            }
            samples += ema.toFloat()
            onProgress(((now - start).toFloat() / durationMs).coerceIn(0f, 1f), ema, samples.toList())
        }
        synchronized(openConnections) { openConnections.forEach { runCatching { it.disconnect() } } }
        workers.forEach { it.cancel() }

        val endBytes = bytes.get()
        val endT = System.currentTimeMillis()
        val result = if (warmBytes >= 0 && endT > warmT) {
            (endBytes - warmBytes) * 8.0 / (endT - warmT) / 1000.0
        } else {
            endBytes * 8.0 / (endT - start).coerceAtLeast(1) / 1000.0
        }
        if (endBytes == 0L) throw IOException("no data transferred")
        result
    }

    companion object {
        private const val BASE = "https://speed.cloudflare.com"

        /** Public IP and provider; used by the Wi-Fi details screen. */
        suspend fun publicInfo(): ServerInfo? = withContext(Dispatchers.IO) { SpeedTestEngine().fetchMeta() }
    }
}

