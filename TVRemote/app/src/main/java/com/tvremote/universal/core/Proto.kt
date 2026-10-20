package com.tvremote.universal.core

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/** Minimal protobuf writer: enough for the Android TV remote and pairing protocols. */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun int(field: Int, value: Int) = varint(field, value.toLong())

    fun bool(field: Int, value: Boolean) = varint(field, if (value) 1 else 0)

    fun string(field: Int, value: String) = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun bytes(field: Int, value: ByteArray) {
        tag(field, 2)
        writeVarint(out, value.size.toLong())
        out.write(value)
    }

    fun message(field: Int, block: ProtoWriter.() -> Unit) =
        bytes(field, ProtoWriter().apply(block).toByteArray())

    fun varint(field: Int, value: Long) {
        tag(field, 0)
        writeVarint(out, value)
    }

    private fun tag(field: Int, wireType: Int) = writeVarint(out, ((field shl 3) or wireType).toLong())

    fun toByteArray(): ByteArray = out.toByteArray()

    companion object {
        fun build(block: ProtoWriter.() -> Unit): ByteArray = ProtoWriter().apply(block).toByteArray()

        fun writeVarint(out: OutputStream, value: Long) {
            var v = value
            while (true) {
                if (v and 0x7FL.inv() == 0L) {
                    out.write(v.toInt())
                    return
                }
                out.write(((v and 0x7F) or 0x80).toInt())
                v = v ushr 7
            }
        }

        /** Writes a varint length-delimited frame. */
        fun writeDelimited(out: OutputStream, payload: ByteArray) {
            val buf = ByteArrayOutputStream(payload.size + 5)
            writeVarint(buf, payload.size.toLong())
            buf.write(payload)
            out.write(buf.toByteArray())
            out.flush()
        }
    }
}

/** Minimal protobuf reader producing field -> values. */
class ProtoMessage private constructor(private val fields: Map<Int, List<Any>>) {

    fun has(field: Int) = fields.containsKey(field)

    fun long(field: Int, default: Long = 0): Long = fields[field]?.lastOrNull() as? Long ?: default

    fun int(field: Int, default: Int = 0): Int = long(field, default.toLong()).toInt()

    fun bool(field: Int): Boolean = long(field) != 0L

    fun bytes(field: Int): ByteArray? = fields[field]?.lastOrNull() as? ByteArray

    fun string(field: Int): String = bytes(field)?.toString(Charsets.UTF_8).orEmpty()

    fun message(field: Int): ProtoMessage? = bytes(field)?.let { parse(it) }

    companion object {
        fun parse(data: ByteArray): ProtoMessage {
            val map = HashMap<Int, MutableList<Any>>()
            var pos = 0
            fun readVarint(): Long {
                var result = 0L
                var shift = 0
                while (true) {
                    if (pos >= data.size) throw EOFException("Truncated varint")
                    val b = data[pos++].toInt() and 0xFF
                    result = result or ((b and 0x7F).toLong() shl shift)
                    if (b and 0x80 == 0) return result
                    shift += 7
                    if (shift > 63) throw EOFException("Malformed varint")
                }
            }
            while (pos < data.size) {
                val key = readVarint()
                val field = (key ushr 3).toInt()
                when ((key and 7).toInt()) {
                    0 -> map.getOrPut(field) { mutableListOf() }.add(readVarint())
                    1 -> pos += 8
                    2 -> {
                        val len = readVarint().toInt()
                        if (len < 0 || pos + len > data.size) throw EOFException("Truncated field")
                        map.getOrPut(field) { mutableListOf() }.add(data.copyOfRange(pos, pos + len))
                        pos += len
                    }
                    5 -> pos += 4
                    else -> throw EOFException("Unsupported wire type")
                }
            }
            return ProtoMessage(map)
        }

        /** Reads one varint length-delimited frame. Throws [EOFException] when the stream ends. */
        fun readDelimited(input: InputStream): ByteArray {
            var len = 0L
            var shift = 0
            while (true) {
                val b = input.read()
                if (b < 0) throw EOFException("Connection closed")
                len = len or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) break
                shift += 7
                if (shift > 35) throw EOFException("Malformed frame length")
            }
            if (len > 1_000_000) throw EOFException("Frame too large")
            val buf = ByteArray(len.toInt())
            var read = 0
            while (read < buf.size) {
                val n = input.read(buf, read, buf.size - read)
                if (n < 0) throw EOFException("Connection closed")
                read += n
            }
            return buf
        }
    }
}
