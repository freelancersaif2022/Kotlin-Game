package com.flashshare.app.net

import android.util.Log
import com.flashshare.app.data.Category
import com.flashshare.app.data.ShareItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import java.util.Base64

object Proto {
    const val PORT = 39601
    const val T_CONTROL: Byte = 'C'.code.toByte()
    const val T_DATA: Byte = 'D'.code.toByte()
    const val VERSION = 1
    /** Big files are split into chunks that are spread across the parallel streams. */
    const val CHUNK = 8L * 1024 * 1024
    /** Unit of progress reporting / rate limiting / encryption frames. */
    const val SLICE = 1024 * 1024
    const val STREAMS = 4
    const val SOCKET_BUFFER = 4 * 1024 * 1024
    /** Free plan speed cap (bytes/s). Premium is uncapped – the Wi-Fi link is the only limit. */
    const val FREE_CAP = 25L * 1024 * 1024
}

class AuthException(message: String, val needPin: Boolean) : IOException(message)

data class PeerInfo(
    val deviceId: String,
    val name: String,
    val avatarId: Int,
    val avatar: ByteArray?,
    val model: String,
    val premium: Boolean,
)

data class FileMeta(
    val index: Int,
    val name: String,
    val size: Long,
    val mime: String,
    val category: Category,
    val relPath: String,
) {
    fun toJson() = JSONObject().apply {
        put("i", index); put("n", name); put("s", size); put("m", mime); put("c", category.name)
        if (relPath.isNotEmpty()) put("r", relPath)
    }

    companion object {
        fun fromJson(o: JSONObject) = FileMeta(
            index = o.getInt("i"), name = o.getString("n"), size = o.getLong("s"), mime = o.optString("m", "application/octet-stream"),
            category = runCatching { Category.valueOf(o.optString("c")) }.getOrDefault(Category.FILE), relPath = o.optString("r"),
        )
    }
}

enum class BatchStatus { WAITING, RUNNING, COMPLETED, FAILED, CANCELLED }

/** One "send" action: a list of files moving in one direction between two phones. */
class Batch(
    val id: Int,
    val session: PeerSession,
    val outgoing: Boolean,
    val files: List<FileMeta>,
    val items: List<ShareItem>?,
    val streamCount: Int,
) {
    val total: Long = files.sumOf { it.size }
    val fileProgress = AtomicLongArray(files.size)
    val transferred = AtomicLong()
    val filesDone = AtomicInteger()
    val savedUris = arrayOfNulls<String>(files.size)
    val sinks = arrayOfNulls<Sink>(files.size)
    val channels = CopyOnWriteArrayList<SocketChannel>()
    val queue = ConcurrentLinkedQueue<LongArray>() // [fileIndex, offset, length]
    val streamsFinished = AtomicInteger()
    @Volatile var status = BatchStatus.WAITING
    @Volatile var error: String? = null
    @Volatile var startedAt = System.currentTimeMillis()
    @Volatile var endedAt = 0L
    val key: String get() = session.key + "#" + id
    val isActive: Boolean get() = status == BatchStatus.WAITING || status == BatchStatus.RUNNING
}

interface EngineCallbacks {
    fun localHello(): JSONObject
    fun acceptNewPeer(): Boolean
    fun isPremium(): Boolean
    fun saver(): SinkFactory
    fun openSource(item: ShareItem): Source
    fun onSessionReady(session: PeerSession)
    fun onSessionClosed(session: PeerSession, error: String?)
    fun onBatchAdded(batch: Batch)
    fun onBatchFinished(batch: Batch)
}

class TransferEngine(private val cb: EngineCallbacks) {
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "fs-io").apply { priority = Thread.MAX_PRIORITY - 1 } }
    private var server: ServerSocketChannel? = null
    @Volatile private var closed = false
    val sessions = CopyOnWriteArrayList<PeerSession>()
    @Volatile var hostCrypto: CryptoBox? = null
    internal val random = SecureRandom()

    // region server
    fun listen(preferredPort: Int = Proto.PORT): Int {
        val ss = ServerSocketChannel.open()
        ss.socket().reuseAddress = true
        ss.socket().receiveBufferSize = Proto.SOCKET_BUFFER
        try {
            ss.bind(InetSocketAddress(preferredPort), 16)
        } catch (e: IOException) {
            ss.bind(InetSocketAddress(0), 16)
        }
        server = ss
        pool.execute { acceptLoop(ss) }
        return ss.socket().localPort
    }

    private fun acceptLoop(ss: ServerSocketChannel) {
        while (!closed) {
            val ch = try { ss.accept() } catch (e: IOException) { break } ?: continue
            pool.execute { handleIncoming(ch) }
        }
    }

    private fun handleIncoming(ch: SocketChannel) {
        try {
            tune(ch)
            val t = ByteBuffer.allocate(1).also { ch.readFully(it) }.get(0)
            when (t) {
                Proto.T_CONTROL -> serverHandshake(ch)
                Proto.T_DATA -> {
                    val h = ByteBuffer.allocate(16 + 8).also { ch.readFully(it); it.flip() }
                    val sid = ByteArray(16).also { h.get(it) }
                    val batchId = h.int
                    val session = sessions.firstOrNull { it.sessionId.contentEquals(sid) }
                    if (session == null) { ch.close(); return }
                    session.runDataStream(batchId, ch)
                }
                else -> ch.close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "incoming failed", e)
            runCatching { ch.close() }
        }
    }

    private fun serverHandshake(ch: SocketChannel) {
        val control = ControlChannel(ch)
        val hello = control.receive()
        if (hello.optString("t") != "hello") { ch.close(); return }
        if (!cb.acceptNewPeer()) {
            control.send(JSONObject().put("t", "busy"))
            ch.close(); return
        }
        val sid = ByteArray(16).also { random.nextBytes(it) }
        val nonce = ByteArray(16).also { random.nextBytes(it) }
        val crypto = hostCrypto
        control.send(cb.localHello().apply {
            put("t", "hello"); put("sid", b64(sid)); put("nonce", b64(nonce)); put("enc", crypto != null)
        })
        if (crypto != null) {
            val auth = control.receive()
            val proof = runCatching { unb64(auth.optString("proof")) }.getOrNull()
            if (auth.optString("t") != "auth" || proof == null || !MessageDigest.isEqual(proof, crypto.proof(nonce))) {
                control.send(JSONObject().put("t", "auth_fail"))
                ch.close(); return
            }
            control.send(JSONObject().put("t", "auth_ok"))
            control.crypto = crypto
        }
        val session = PeerSession(this, cb, control, isClient = false, remote = null, port = 0,
            sessionId = sid, peer = parsePeer(hello), crypto = crypto)
        sessions += session
        cb.onSessionReady(session)
        session.readLoop()
    }
    // endregion

    // region client
    /** Connects to a hosting phone. Blocking – call from a background thread. */
    fun connect(host: String, port: Int, crypto: CryptoBox?, encryptedHost: Boolean): PeerSession {
        var ch: SocketChannel? = null
        var lastError: Exception? = null
        // The group owner may need a moment after the Wi-Fi link is up before its socket is reachable.
        for (attempt in 0 until 20) {
            val c = SocketChannel.open()
            try {
                tune(c)
                c.socket().connect(InetSocketAddress(host, port), 4000)
                ch = c
                break
            } catch (e: Exception) {
                runCatching { c.close() }
                lastError = e
                Thread.sleep(500)
            }
        }
        val socket = ch ?: throw IOException("Couldn't reach the other phone (${lastError?.message})")
        socket.writeFully(ByteBuffer.wrap(byteArrayOf(Proto.T_CONTROL)))
        val control = ControlChannel(socket)
        control.send(cb.localHello().put("t", "hello"))
        val reply = control.receive()
        when (reply.optString("t")) {
            "busy" -> { socket.close(); throw IOException("The other phone is already connected to someone else") }
            "hello" -> Unit
            else -> { socket.close(); throw IOException("Unexpected reply") }
        }
        val sid = unb64(reply.getString("sid"))
        val enc = reply.optBoolean("enc")
        if (enc) {
            if (crypto == null) { socket.close(); throw AuthException("Enter the 6-digit PIN shown on the other phone", needPin = true) }
            control.send(JSONObject().put("t", "auth").put("proof", b64(crypto.proof(unb64(reply.getString("nonce"))))))
            val r = control.receive()
            if (r.optString("t") != "auth_ok") { socket.close(); throw AuthException("Wrong PIN – check the code on the other phone", needPin = true) }
            control.crypto = crypto
        }
        val session = PeerSession(this, cb, control, isClient = true, remote = host, port = port,
            sessionId = sid, peer = parsePeer(reply), crypto = if (enc) crypto else null)
        sessions += session
        cb.onSessionReady(session)
        pool.execute { session.readLoop() }
        return session
    }
    // endregion

    internal fun execute(r: Runnable) = pool.execute(r)

    internal fun removeSession(s: PeerSession) { sessions.remove(s) }

    fun close() {
        closed = true
        sessions.forEach { it.close("Disconnected", notifyPeer = true) }
        sessions.clear()
        runCatching { server?.close() }
        server = null
        hostCrypto = null
    }

    fun shutdown() {
        close()
        pool.shutdownNow()
    }

    private fun parsePeer(o: JSONObject) = PeerInfo(
        deviceId = o.optString("id"),
        name = o.optString("name", "Phone"),
        avatarId = o.optInt("av"),
        avatar = o.optString("img").takeIf { it.isNotEmpty() }?.let { runCatching { unb64(it) }.getOrNull() },
        model = o.optString("model"),
        premium = o.optBoolean("premium"),
    )

    companion object {
        private const val TAG = "Engine"

        fun tune(ch: SocketChannel) {
            runCatching {
                ch.setOption(StandardSocketOptions.TCP_NODELAY, true)
                ch.setOption(StandardSocketOptions.SO_SNDBUF, Proto.SOCKET_BUFFER)
                ch.setOption(StandardSocketOptions.SO_RCVBUF, Proto.SOCKET_BUFFER)
                ch.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
            }
        }

        fun b64(b: ByteArray): String = Base64.getEncoder().encodeToString(b)
        fun unb64(s: String): ByteArray = Base64.getDecoder().decode(s)
    }
}

/** A connection to one peer. Both sides can send any number of batches over the same session. */
class PeerSession(
    private val engine: TransferEngine,
    private val cb: EngineCallbacks,
    private val control: ControlChannel,
    val isClient: Boolean,
    private val remote: String?,
    private val port: Int,
    val sessionId: ByteArray,
    val peer: PeerInfo,
    val crypto: CryptoBox?,
) {
    val key: String = TransferEngine.b64(sessionId)
    val batches = ConcurrentHashMap<Int, Batch>()
    private val nextId = AtomicInteger(if (isClient) 1 else 2)
    @Volatile var closed = false
        private set
    val encrypted: Boolean get() = crypto != null

    fun readLoop() {
        var err: String? = null
        try {
            while (!closed) handle(control.receive())
        } catch (e: Exception) {
            if (!closed) err = "Connection lost"
        }
        close(err, notifyPeer = false)
    }

    private fun handle(m: JSONObject) {
        when (m.optString("t")) {
            "manifest" -> onManifest(m)
            "accept" -> batches[m.getInt("b")]?.let { b ->
                b.status = BatchStatus.RUNNING
                b.startedAt = System.currentTimeMillis()
                if (isClient) openDataStreams(b)
            }
            "reject" -> batches[m.getInt("b")]?.let { finish(it, BatchStatus.FAILED, m.optString("r", "Declined"), notify = false) }
            "done" -> batches[m.getInt("b")]?.let { finish(it, BatchStatus.COMPLETED, null, notify = false) }
            "cancel" -> batches[m.getInt("b")]?.let { finish(it, BatchStatus.CANCELLED, m.optString("r").ifEmpty { "Cancelled by ${peer.name}" }, notify = false) }
            "bye" -> close(null, notifyPeer = false)
        }
    }

    // region sending
    fun send(items: List<ShareItem>): Batch? {
        if (closed || items.isEmpty()) return null
        val metas = items.mapIndexed { i, it -> FileMeta(i, it.name, it.size, it.mime, it.category, it.relPath) }
        val b = Batch(nextId.getAndAdd(2), this, outgoing = true, files = metas, items = items, streamCount = Proto.STREAMS)
        // Chunk plan: small files are one chunk; large seekable files are split so all streams stay busy.
        metas.forEach { f ->
            val seekable = f.size > Proto.CHUNK && runCatching { cb.openSource(items[f.index]).use { it.seekable } }.getOrDefault(false)
            if (!seekable || f.size <= Proto.CHUNK) {
                if (f.size > 0) b.queue += longArrayOf(f.index.toLong(), 0, f.size)
            } else {
                var off = 0L
                while (off < f.size) {
                    val len = minOf(Proto.CHUNK, f.size - off)
                    b.queue += longArrayOf(f.index.toLong(), off, len)
                    off += len
                }
            }
        }
        batches[b.id] = b
        cb.onBatchAdded(b)
        try {
            control.send(JSONObject().apply {
                put("t", "manifest"); put("b", b.id); put("n", b.streamCount)
                put("files", JSONArray().apply { metas.forEach { put(it.toJson()) } })
            })
        } catch (e: IOException) {
            finish(b, BatchStatus.FAILED, "Connection lost", notify = false)
        }
        return b
    }

    private fun runSender(b: Batch, ch: SocketChannel) {
        val limiter = if (cb.isPremium()) null else sharedLimiter
        val header = ByteBuffer.allocate(16)
        val scratch = ByteBuffer.allocate(Proto.SLICE)
        val sealed = if (crypto != null) ByteArray(Proto.SLICE + CryptoBox.IV + CryptoBox.TAG + 64) else null
        var src: Source? = null
        var srcIndex = -1
        try {
            while (b.isActive) {
                val c = b.queue.poll() ?: break
                val fi = c[0].toInt(); val start = c[1]; val len = c[2]
                if (fi != srcIndex) {
                    src?.close()
                    src = cb.openSource(b.items!![fi])
                    srcIndex = fi
                }
                val s = src!!
                var pos = start
                var remaining = len
                if (crypto == null) {
                    header.clear(); header.putInt(fi).putLong(start).putInt(len.toInt()); header.flip()
                    ch.writeFully(header)
                    while (remaining > 0) {
                        val n = minOf(remaining, Proto.SLICE.toLong())
                        limiter?.acquire(n)
                        val sent = s.sendTo(ch, pos, n, scratch)
                        pos += sent; remaining -= sent
                        b.fileProgress.addAndGet(fi, sent); b.transferred.addAndGet(sent)
                    }
                } else {
                    val plain = scratch.array()
                    while (remaining > 0) {
                        val n = minOf(remaining, Proto.SLICE.toLong()).toInt()
                        limiter?.acquire(n.toLong())
                        s.readFully(plain, pos, n)
                        val sl = crypto.sealInto(plain, n, sealed!!)
                        header.clear(); header.putInt(fi).putLong(pos).putInt(n); header.flip()
                        ch.writeFully(header)
                        ch.writeFully(ByteBuffer.wrap(sealed, 0, sl))
                        pos += n; remaining -= n
                        b.fileProgress.addAndGet(fi, n.toLong()); b.transferred.addAndGet(n.toLong())
                    }
                }
            }
            header.clear(); header.putInt(-1).putLong(0).putInt(0); header.flip()
            ch.writeFully(header)
            // All bytes are on the wire; completion is confirmed by the receiver's "done".
            b.streamsFinished.incrementAndGet()
        } catch (e: Exception) {
            failStream(b, e, "send")
        } finally {
            src?.close()
        }
    }
    // endregion

    // region receiving
    private fun onManifest(m: JSONObject) {
        val id = m.getInt("b")
        val arr = m.getJSONArray("files")
        val metas = (0 until arr.length()).map { FileMeta.fromJson(arr.getJSONObject(it)) }
        val b = Batch(id, this, outgoing = false, files = metas, items = null, streamCount = m.optInt("n", Proto.STREAMS).coerceIn(1, 8))
        batches[id] = b
        cb.onBatchAdded(b)
        val saver = cb.saver()
        if (saver.freeSpace() < b.total + 32L * 1024 * 1024) {
            b.status = BatchStatus.FAILED; b.error = "Not enough storage on this phone"; b.endedAt = System.currentTimeMillis()
            control.send(JSONObject().put("t", "reject").put("b", id).put("r", "Not enough storage on ${cb.localHello().optString("name")}"))
            cb.onBatchFinished(b)
            return
        }
        b.status = BatchStatus.RUNNING
        b.startedAt = System.currentTimeMillis()
        // Zero-byte files never get data frames – create them now.
        metas.filter { it.size == 0L }.forEach { f ->
            runCatching { b.savedUris[f.index] = saver.create(f).finish() }
            b.filesDone.incrementAndGet()
        }
        control.send(JSONObject().put("t", "accept").put("b", id))
        if (b.filesDone.get() == metas.size) {
            completeIncoming(b)
            return
        }
        if (isClient) openDataStreams(b)
    }

    private fun runReceiver(b: Batch, ch: SocketChannel) {
        val header = ByteBuffer.allocate(16)
        val buf = ByteBuffer.allocateDirect(Proto.SLICE)
        val sealed = if (crypto != null) ByteArray(Proto.SLICE + CryptoBox.IV + CryptoBox.TAG + 64) else null
        val plain = if (crypto != null) ByteArray(Proto.SLICE + 64) else null
        val saver = cb.saver()
        try {
            while (true) {
                header.clear(); ch.readFully(header); header.flip()
                val fi = header.int; val off = header.long; val len = header.int
                if (fi < 0) break
                if (fi >= b.files.size || len < 0) throw IOException("Corrupt stream")
                val sink = sinkFor(b, fi, saver)
                if (crypto == null) {
                    var pos = off
                    var remaining = len.toLong()
                    while (remaining > 0) {
                        val n = minOf(remaining, Proto.SLICE.toLong()).toInt()
                        buf.clear(); buf.limit(n)
                        ch.readFully(buf)
                        buf.flip()
                        while (buf.hasRemaining()) pos += sink.channel.write(buf, pos)
                        remaining -= n
                        progress(b, fi, n.toLong(), saver)
                    }
                } else {
                    val sl = len + CryptoBox.IV + CryptoBox.TAG
                    ch.readFully(ByteBuffer.wrap(sealed!!, 0, sl))
                    val n = crypto.openInto(sealed, sl, plain!!)
                    val bb = ByteBuffer.wrap(plain, 0, n)
                    var pos = off
                    while (bb.hasRemaining()) pos += sink.channel.write(bb, pos)
                    progress(b, fi, n.toLong(), saver)
                }
            }
        } catch (e: Exception) {
            failStream(b, e, "receive")
        }
    }

    private fun sinkFor(b: Batch, fi: Int, saver: SinkFactory): Sink {
        b.sinks[fi]?.let { return it }
        synchronized(b.sinks) {
            b.sinks[fi]?.let { return it }
            return saver.create(b.files[fi]).also { b.sinks[fi] = it }
        }
    }

    private fun progress(b: Batch, fi: Int, n: Long, saver: SinkFactory) {
        b.transferred.addAndGet(n)
        val done = b.fileProgress.addAndGet(fi, n)
        if (done == b.files[fi].size) {
            val sink = b.sinks[fi]
            b.savedUris[fi] = runCatching { sink?.finish() }.getOrNull()
            synchronized(b.sinks) { b.sinks[fi] = null }
            if (b.filesDone.incrementAndGet() == b.files.size) completeIncoming(b)
        }
    }

    private fun completeIncoming(b: Batch) {
        runCatching { control.send(JSONObject().put("t", "done").put("b", b.id)) }
        finish(b, BatchStatus.COMPLETED, null, notify = false)
    }
    // endregion

    /** A data socket broke. Give the control channel a moment – a "cancel"/"done" from the peer explains it better. */
    private fun failStream(b: Batch, e: Exception, what: String) {
        if (!b.isActive) return
        Thread.sleep(400)
        if (b.isActive && !closed) {
            Log.w("Session", "$what stream failed", e)
            finish(b, BatchStatus.FAILED, readable(e), notify = true)
        }
    }

    // region data streams
    private val sharedLimiter = RateLimiter(Proto.FREE_CAP)

    /** Client side: open the parallel data sockets for a batch. */
    private fun openDataStreams(b: Batch) {
        val host = remote ?: return
        repeat(b.streamCount) {
            engine.execute {
                try {
                    val ch = SocketChannel.open()
                    TransferEngine.tune(ch)
                    ch.socket().connect(InetSocketAddress(host, port), 8000)
                    val h = ByteBuffer.allocate(1 + 16 + 8)
                    h.put(Proto.T_DATA).put(sessionId).putInt(b.id).putInt(it); h.flip()
                    ch.writeFully(h)
                    runStream(b, ch)
                } catch (e: Exception) {
                    failStream(b, e, "open")
                }
            }
        }
    }

    /** Server side: a data socket arrived for a batch. */
    fun runDataStream(batchId: Int, ch: SocketChannel) {
        val b = batches[batchId]
        if (b == null || !b.isActive) { ch.close(); return }
        runStream(b, ch)
    }

    private fun runStream(b: Batch, ch: SocketChannel) {
        b.channels += ch
        try {
            if (b.outgoing) runSender(b, ch) else runReceiver(b, ch)
        } finally {
            // Keep the socket open until the batch settles so the last bytes are not reset.
            if (!b.isActive) runCatching { ch.close() }
        }
    }
    // endregion

    // region lifecycle
    fun cancel(b: Batch) {
        if (!b.isActive) return
        finish(b, BatchStatus.CANCELLED, "Cancelled", notify = true)
    }

    @Synchronized
    private fun finish(b: Batch, status: BatchStatus, error: String?, notify: Boolean) {
        if (!b.isActive) return
        b.status = status
        b.error = error
        b.endedAt = System.currentTimeMillis()
        b.queue.clear()
        if (notify && status != BatchStatus.COMPLETED) {
            runCatching { control.send(JSONObject().put("t", "cancel").put("b", b.id).put("r", if (status == BatchStatus.CANCELLED) "" else error.orEmpty())) }
        }
        b.channels.forEach { runCatching { it.close() } }
        if (!b.outgoing && status != BatchStatus.COMPLETED) {
            // Remove half-written files.
            synchronized(b.sinks) {
                b.sinks.forEachIndexed { i, s -> s?.abort(); b.sinks[i] = null }
            }
        }
        cb.onBatchFinished(b)
    }

    fun close(reason: String?, notifyPeer: Boolean) {
        if (closed) return
        closed = true
        if (notifyPeer) runCatching { control.send(JSONObject().put("t", "bye")) }
        batches.values.filter { it.isActive }.forEach { b ->
            b.status = BatchStatus.FAILED; b.error = reason ?: "Disconnected"; b.endedAt = System.currentTimeMillis()
            b.channels.forEach { runCatching { it.close() } }
            if (!b.outgoing) synchronized(b.sinks) { b.sinks.forEachIndexed { i, s -> s?.abort(); b.sinks[i] = null } }
            cb.onBatchFinished(b)
        }
        control.close()
        engine.removeSession(this)
        cb.onSessionClosed(this, reason)
    }
    // endregion

    private fun readable(e: Exception): String = when (e) {
        is EOFException -> "Connection closed by ${peer.name}"
        is IOException -> if (e.message?.contains("ENOSPC") == true || e.message?.contains("No space") == true) "Storage full" else "Connection lost"
        is SecurityException -> "Permission denied for a file"
        else -> "Transfer error"
    }
}

/** Length-prefixed JSON messages (AES-GCM sealed after PIN pairing). */
class ControlChannel(private val ch: SocketChannel) {
    @Volatile var crypto: CryptoBox? = null
    private val writeLock = Any()
    private val readLock = Any()

    fun send(obj: JSONObject) {
        val raw = obj.toString().toByteArray()
        val payload = crypto?.seal(raw) ?: raw
        val b = ByteBuffer.allocate(4 + payload.size)
        b.putInt(payload.size).put(payload).flip()
        synchronized(writeLock) { ch.writeFully(b) }
    }

    fun receive(): JSONObject = synchronized(readLock) {
        val h = ByteBuffer.allocate(4)
        ch.readFully(h); h.flip()
        val n = h.int
        if (n < 0 || n > 32 * 1024 * 1024) throw IOException("Bad frame")
        val b = ByteBuffer.allocate(n)
        ch.readFully(b)
        val raw = crypto?.open(b.array()) ?: b.array()
        JSONObject(String(raw))
    }

    fun close() = runCatching { ch.close() }
}

fun SocketChannel.writeFully(buf: ByteBuffer) {
    while (buf.hasRemaining()) write(buf)
}

fun SocketChannel.readFully(buf: ByteBuffer) {
    while (buf.hasRemaining()) if (read(buf) < 0) throw EOFException()
}
