package com.flashshare.app.net

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel

/** A file being written by one or more parallel streams using positional writes. */
interface Sink {
    val channel: FileChannel
    /** Flushes and publishes the file; returns a content Uri that other apps can open. */
    fun finish(): String?
    fun abort()
}

interface SinkFactory {
    fun create(meta: FileMeta): Sink
    fun freeSpace(): Long
}

/** Read side of a file being sent. Seekable files use zero-copy transferTo(); pipes are read sequentially. */
class Source(private val fis: FileInputStream, private val owner: AutoCloseable? = null) : AutoCloseable {
    private val fc: FileChannel? = fis.channel.takeIf { ch -> runCatching { ch.position(0); ch.size(); true }.getOrDefault(false) }
    private val stream: InputStream? = if (fc == null) fis else null
    private var streamPos = 0L

    val seekable: Boolean get() = fc != null

    /** Sends [count] bytes starting at [pos]. Returns bytes written. */
    fun sendTo(out: WritableByteChannel, pos: Long, count: Long, scratch: ByteBuffer): Long {
        val ch = fc
        if (ch != null) {
            var done = 0L
            while (done < count) {
                val n = ch.transferTo(pos + done, count - done, out)
                if (n <= 0) {
                    // transferTo can return 0 on some kernels/providers – fall back to a buffered copy.
                    scratch.clear()
                    scratch.limit(minOf(scratch.capacity().toLong(), count - done).toInt())
                    val r = ch.read(scratch, pos + done)
                    if (r <= 0) throw IOException("File changed while sending")
                    scratch.flip()
                    while (scratch.hasRemaining()) out.write(scratch)
                    done += r
                } else done += n
            }
            return done
        }
        // Sequential stream
        if (pos != streamPos) throw IOException("Non-seekable source")
        val arr = scratch.array()
        var done = 0L
        while (done < count) {
            val r = stream!!.read(arr, 0, minOf(arr.size.toLong(), count - done).toInt())
            if (r < 0) throw IOException("File ended early")
            scratch.clear(); scratch.limit(r)
            while (scratch.hasRemaining()) out.write(scratch)
            done += r
        }
        streamPos += done
        return done
    }

    /** Reads exactly [count] bytes at [pos] into [dst]. */
    fun readFully(dst: ByteArray, pos: Long, count: Int) {
        val ch = fc
        if (ch != null) {
            val bb = ByteBuffer.wrap(dst, 0, count)
            var p = pos
            while (bb.hasRemaining()) {
                val r = ch.read(bb, p)
                if (r <= 0) throw IOException("File changed while sending")
                p += r
            }
            return
        }
        if (pos != streamPos) throw IOException("Non-seekable source")
        var off = 0
        while (off < count) {
            val r = stream!!.read(dst, off, count - off)
            if (r < 0) throw IOException("File ended early")
            off += r
        }
        streamPos += count
    }

    override fun close() {
        runCatching { fis.close() }
        runCatching { owner?.close() }
    }
}
