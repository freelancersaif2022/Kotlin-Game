package com.tvremote.universal.tv

import android.util.Base64
import com.tvremote.universal.core.ClientIdentity
import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher

/**
 * Minimal ADB-over-network client (port 5555) used to control Fire TV devices.
 * Requires "ADB debugging" to be enabled on the Fire TV once.
 */
class AdbClient(private val host: String, private val identity: ClientIdentity) {

    private class Packet(val command: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    private var socket: Socket? = null
    private var out: OutputStream? = null
    private var input: DataInputStream? = null
    private val writeLock = Any()
    private val localIds = AtomicInteger(1)

    val isOpen get() = socket?.isClosed == false

    /**
     * Connects and authenticates. [onApprovalNeeded] is invoked when the TV shows
     * the "Allow USB debugging?" dialog.
     */
    fun connect(onApprovalNeeded: () -> Unit, approvalTimeoutMs: Int) {
        val s = Socket()
        s.connect(InetSocketAddress(host, 5555), 4000)
        s.soTimeout = 8000
        s.tcpNoDelay = true
        socket = s
        out = s.getOutputStream()
        input = DataInputStream(s.getInputStream().buffered())

        write(CNXN, VERSION, MAX_DATA, "host::\u0000".toByteArray())
        var p = read()
        var signed = false
        while (p.command == AUTH) {
            if (p.arg0 != AUTH_TOKEN) throw IOException("Unexpected ADB auth")
            if (!signed) {
                write(AUTH, AUTH_SIGNATURE, 0, sign(p.data))
                signed = true
            } else {
                val key = Base64.encodeToString(androidPublicKey(identity.publicKey), Base64.NO_WRAP) + " tvremote@android\u0000"
                write(AUTH, AUTH_RSAPUBLICKEY, 0, key.toByteArray())
                onApprovalNeeded()
                s.soTimeout = approvalTimeoutMs
            }
            p = read()
        }
        if (p.command != CNXN) throw IOException("ADB connection refused")
        s.soTimeout = 0
    }

    /** Runs a shell command on its own stream without waiting for output. */
    fun shell(command: String) {
        val id = localIds.getAndIncrement()
        write(OPEN, id, 0, "shell:$command\u0000".toByteArray())
    }

    /** Handles incoming packets until the connection drops. */
    fun pump() {
        while (true) {
            val p = read()
            when (p.command) {
                WRTE -> write(OKAY, p.arg1, p.arg0, ByteArray(0))
                CLSE -> if (p.arg0 != 0) write(CLSE, p.arg1, p.arg0, ByteArray(0))
            }
        }
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun write(command: Int, arg0: Int, arg1: Int, data: ByteArray) {
        val o = out ?: throw IOException("Not connected")
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(command).putInt(arg0).putInt(arg1).putInt(data.size)
        header.putInt(data.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) })
        header.putInt(command xor -1)
        synchronized(writeLock) {
            o.write(header.array())
            if (data.isNotEmpty()) o.write(data)
            o.flush()
        }
    }

    private fun read(): Packet {
        val i = input ?: throw IOException("Not connected")
        val header = ByteArray(24)
        i.readFully(header)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = bb.int
        val arg0 = bb.int
        val arg1 = bb.int
        val len = bb.int
        if (len < 0 || len > MAX_DATA) throw IOException("Bad ADB packet")
        val data = ByteArray(len)
        i.readFully(data)
        return Packet(command, arg0, arg1, data)
    }

    private fun sign(token: ByteArray): ByteArray {
        val payload = SHA1_DIGEST_INFO + token
        return runCatching {
            Signature.getInstance("NONEwithRSA").run {
                initSign(identity.privateKey)
                update(payload)
                sign()
            }
        }.getOrElse {
            Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
                init(Cipher.ENCRYPT_MODE, identity.privateKey)
                doFinal(payload)
            }
        }
    }

    /** Android's custom RSA public key format used by adbd. */
    private fun androidPublicKey(key: RSAPublicKey): ByteArray {
        val n = key.modulus
        val words = 64
        val r32 = BigInteger.ONE.shiftLeft(32)
        val n0inv = r32.subtract(n.mod(r32).modInverse(r32))
        val rr = BigInteger.ONE.shiftLeft(2048).modPow(BigInteger.valueOf(2), n)
        val bb = ByteBuffer.allocate(4 + 4 + 256 + 256 + 4).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(words)
        bb.putInt(n0inv.toInt())
        bb.put(littleEndian(n, 256))
        bb.put(littleEndian(rr, 256))
        bb.putInt(key.publicExponent.toInt())
        return bb.array()
    }

    private fun littleEndian(v: BigInteger, size: Int): ByteArray {
        val be = v.toByteArray()
        val out = ByteArray(size)
        for (i in 0 until size) {
            val src = be.size - 1 - i
            out[i] = if (src >= 0) be[src] else 0
        }
        return out
    }

    companion object {
        private fun cmd(s: String) = s.toByteArray().foldIndexed(0) { idx, acc, b -> acc or ((b.toInt() and 0xFF) shl (idx * 8)) }
        val CNXN = cmd("CNXN")
        val AUTH = cmd("AUTH")
        val OPEN = cmd("OPEN")
        val OKAY = cmd("OKAY")
        val CLSE = cmd("CLSE")
        val WRTE = cmd("WRTE")
        const val VERSION = 0x01000000
        const val MAX_DATA = 256 * 1024
        const val AUTH_TOKEN = 1
        const val AUTH_SIGNATURE = 2
        const val AUTH_RSAPUBLICKEY = 3
        private val SHA1_DIGEST_INFO = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14,
        )
    }
}

class FireTvController(
    device: TvDevice,
    private val identity: () -> ClientIdentity,
    onDeviceChanged: (TvDevice) -> Unit,
    private val approvalTimeoutMs: Int = 15_000,
) : BaseController(device, onDeviceChanged) {

    override val capabilities = Capabilities(
        keys = RemoteKey.entries.toSet() - RemoteKey.INPUT,
        text = true,
        backspace = true,
        apps = StreamingApp.entries.toSet(),
    )

    @Volatile private var adb: AdbClient? = null
    private var pumpJob: Job? = null

    override suspend fun connect() {
        disconnect()
        _state.value = ConnectionState.Connecting()
        val client = AdbClient(device.host, withContext(Dispatchers.IO) { identity() })
        try {
            withContext(Dispatchers.IO) {
                client.connect(
                    onApprovalNeeded = {
                        _state.value = ConnectionState.Connecting(hint = "On your TV, check \"Always allow\" and select OK")
                    },
                    approvalTimeoutMs = approvalTimeoutMs,
                )
            }
        } catch (e: Exception) {
            client.close()
            val waitingForUser = (_state.value as? ConnectionState.Connecting)?.hint != null
            if (waitingForUser) {
                fail(FailReason.NEEDS_PAIRING, "The Fire TV did not allow the connection. Select OK on the \"Allow USB debugging?\" popup, then try again.")
            } else {
                fail(
                    FailReason.UNREACHABLE,
                    "Can't reach your Fire TV. Make sure it's on, on the same Wi-Fi, and that ADB debugging is turned on (Settings › My Fire TV › Developer options).",
                )
            }
            return
        }
        adb = client
        pumpJob = scope.launch {
            try {
                client.pump()
            } catch (e: Exception) {
                if (adb === client) {
                    adb = null
                    fail(FailReason.UNREACHABLE, Messages.LOST)
                }
            }
        }
        _state.value = ConnectionState.Connected(tvOn = true)
        if (device.model.isBlank()) updateDevice { it.copy(model = "Fire TV") }
    }

    private suspend fun shell(command: String) = withContext(Dispatchers.IO) {
        val client = adb ?: return@withContext
        try {
            client.shell(command)
        } catch (e: IOException) {
            if (adb === client) {
                adb = null
                fail(FailReason.UNREACHABLE, Messages.LOST)
            }
        }
    }

    override suspend fun sendKey(key: RemoteKey) {
        shell("input keyevent ${AndroidTvProtocol.keyCode(key)}")
    }

    override suspend fun sendText(text: String) {
        if (text.isEmpty()) return
        val escaped = text.replace(" ", "%s").replace("'", "'\\''")
        shell("input text '$escaped'")
    }

    override suspend fun sendBackspace() = shell("input keyevent 67")

    override suspend fun sendEnter() = shell("input keyevent 66")

    override suspend fun launchApp(app: StreamingApp) {
        val pkg = when (app) {
            StreamingApp.YOUTUBE -> "com.amazon.firetv.youtube"
            StreamingApp.NETFLIX -> "com.netflix.ninja"
            StreamingApp.PRIME_VIDEO -> "com.amazon.firebat"
            StreamingApp.DISNEY_PLUS -> "com.disney.disneyplus"
        }
        shell("monkey -p $pkg 1")
    }

    override fun disconnect() {
        val client = adb
        adb = null
        pumpJob?.cancel()
        pumpJob = null
        client?.close()
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            _state.value = ConnectionState.Idle
        }
    }
}
