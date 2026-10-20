import android.net.Uri
import com.flashshare.app.data.Category
import com.flashshare.app.data.ShareItem
import com.flashshare.app.net.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class Peer(val name: String, val premium: Boolean = true, val maxPeers: Int = 1) : EngineCallbacks {
    val dir = File.createTempFile("fs-$name", "").apply { delete(); mkdirs() }
    val finished = java.util.concurrent.LinkedBlockingQueue<Batch>()
    val ready = java.util.concurrent.LinkedBlockingQueue<PeerSession>()
    val engine = TransferEngine(this)
    override fun localHello() = JSONObject().put("id", name).put("name", name).put("av", 3).put("model", "jvm").put("premium", premium)
    override fun acceptNewPeer() = engine.sessions.size < maxPeers
    override fun isPremium() = premium
    override fun saver() = object : SinkFactory {
        override fun create(meta: FileMeta): Sink {
            val f = File(dir, (if (meta.relPath.isNotEmpty()) meta.relPath + "/" else "") + meta.name).apply { parentFile.mkdirs() }
            val raf = RandomAccessFile(f, "rw")
            return object : Sink {
                override val channel: FileChannel = raf.channel
                override fun finish(): String { raf.close(); return f.absolutePath }
                override fun abort() { raf.close(); f.delete() }
            }
        }
        override fun freeSpace() = Long.MAX_VALUE
    }
    override fun openSource(item: ShareItem) = Source(FileInputStream(File(item.uri.path!!)))
    override fun onSessionReady(session: PeerSession) { ready += session }
    override fun onSessionClosed(session: PeerSession, error: String?) {}
    override fun onBatchAdded(batch: Batch) {}
    override fun onBatchFinished(batch: Batch) { finished += batch }
}

fun sha(f: File): String = MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }

fun makeFiles(dir: File, sizes: List<Int>): List<ShareItem> = sizes.mapIndexed { i, s ->
    val f = File(dir, "file_$i.bin"); f.writeBytes(Random(i).nextBytes(s))
    ShareItem(Uri.fromFile(f), f.name, s.toLong(), "application/octet-stream", Category.FILE)
}

class EngineTest {
    private val srcDir = File.createTempFile("fs-src", "").apply { delete(); mkdirs() }

    private fun runTransfer(host: Peer, client: Peer, items: List<ShareItem>, crypto: CryptoBox?, hostSends: Boolean = false): Double {
        val port = host.engine.listen(0)
        val cs = client.engine.connect("127.0.0.1", port, crypto, crypto != null)
        val hs = host.ready.poll(5, TimeUnit.SECONDS)!!
        val sender = if (hostSends) hs else cs
        val (sp, rp) = if (hostSends) host to client else client to host
        val t0 = System.nanoTime()
        val b = sender.send(items)!!
        val rb = rp.finished.poll(120, TimeUnit.SECONDS)!!
        val sb = sp.finished.poll(30, TimeUnit.SECONDS)!!
        val secs = (System.nanoTime() - t0) / 1e9
        assertEquals(rb.error, BatchStatus.COMPLETED, rb.status)
        assertEquals(sb.error, BatchStatus.COMPLETED, sb.status)
        items.forEachIndexed { i, it ->
            val out = File(rb.savedUris[i]!!)
            assertEquals("size ${it.name}", it.size, out.length())
            assertEquals("hash ${it.name}", sha(File(it.uri.path!!)), sha(out))
        }
        val mb = b.total / 1048576.0
        println("  %.1f MB in %.2fs = %.0f MB/s (loopback)".format(mb, secs, mb / secs))
        host.engine.close(); client.engine.close()
        return mb / secs
    }

    @Test fun plainManyFiles() {
        println("plain transfer, mixed sizes incl. 0 B and 90 MB:")
        val items = makeFiles(srcDir, listOf(0, 1, 1000, 8 * 1048576 + 3, 50 * 1048576 + 17, 90 * 1048576, 4096, 0))
        runTransfer(Peer("R"), Peer("S"), items, null)
    }

    @Test fun encryptedWithPin() {
        println("PIN-encrypted transfer:")
        val salt = CryptoBox.newSalt(); val pin = CryptoBox.newPin()
        val host = Peer("R"); host.engine.hostCrypto = CryptoBox.derive(pin, salt)
        val items = makeFiles(srcDir, listOf(0, 777, 3 * 1048576 + 5, 40 * 1048576))
        runTransfer(host, Peer("S"), items, CryptoBox.derive(pin, salt))
    }

    @Test fun wrongPinRejected() {
        val salt = CryptoBox.newSalt()
        val host = Peer("R"); host.engine.hostCrypto = CryptoBox.derive("123456", salt)
        val port = host.engine.listen(0)
        val e = runCatching { Peer("S").engine.connect("127.0.0.1", port, CryptoBox.derive("654321", salt), true) }.exceptionOrNull()
        assertTrue("got $e", e is AuthException)
        val e2 = runCatching { Peer("S2").engine.connect("127.0.0.1", port, null, true) }.exceptionOrNull()
        assertTrue("got $e2", e2 is AuthException && e2.needPin)
        println("wrong/missing PIN correctly rejected")
        host.engine.close()
    }

    @Test fun hostSendsBack() {
        println("reverse direction (host -> client), folders:")
        val items = makeFiles(srcDir, listOf(5 * 1048576, 12345)).map { it.copy(relPath = "Album/Sub") }
        runTransfer(Peer("R"), Peer("S"), items, null, hostSends = true)
    }

    @Test fun freePlanIsCapped() {
        println("free plan cap (~25 MB/s):")
        val items = makeFiles(srcDir, listOf(60 * 1048576))
        val speed = runTransfer(Peer("R"), Peer("S", premium = false), items, null)
        assertTrue("speed $speed", speed in 18.0..32.0)
    }

    @Test fun groupOneToMany() {
        println("group: one sender -> two receivers")
        val host = Peer("G", maxPeers = 8)
        val port = host.engine.listen(0)
        val r1 = Peer("R1"); val r2 = Peer("R2")
        r1.engine.connect("127.0.0.1", port, null, false); r2.engine.connect("127.0.0.1", port, null, false)
        val s1 = host.ready.poll(5, TimeUnit.SECONDS)!!; val s2 = host.ready.poll(5, TimeUnit.SECONDS)!!
        val items = makeFiles(srcDir, listOf(20 * 1048576, 999))
        s1.send(items); s2.send(items)
        for (r in listOf(r1, r2)) {
            val b = r.finished.poll(60, TimeUnit.SECONDS)!!
            assertEquals(b.error, BatchStatus.COMPLETED, b.status)
            items.forEachIndexed { i, it -> assertEquals(sha(File(it.uri.path!!)), sha(File(b.savedUris[i]!!))) }
        }
        println("  both receivers got identical files")
        host.engine.close(); r1.engine.close(); r2.engine.close()
    }

    @Test fun cancelMidway() {
        val host = Peer("R"); val client = Peer("S", premium = false)
        val port = host.engine.listen(0)
        val cs = client.engine.connect("127.0.0.1", port, null, false)
        host.ready.poll(5, TimeUnit.SECONDS)
        val b = cs.send(makeFiles(srcDir, listOf(80 * 1048576)))!!
        Thread.sleep(700)
        cs.cancel(b)
        val rb = host.finished.poll(10, TimeUnit.SECONDS)!!
        assertEquals(BatchStatus.CANCELLED, rb.status)
        assertTrue("partial file removed", host.dir.walk().none { it.isFile })
        println("cancel propagates to receiver and partial file is deleted")
        host.engine.close(); client.engine.close()
    }

    @Test fun secondPeerBusy() {
        val host = Peer("R")
        val port = host.engine.listen(0)
        Peer("A").engine.connect("127.0.0.1", port, null, false)
        val e = runCatching { Peer("B").engine.connect("127.0.0.1", port, null, false) }.exceptionOrNull()
        assertNotNull(e)
        println("second sender rejected while busy: ${e!!.message}")
        host.engine.close()
    }
}
