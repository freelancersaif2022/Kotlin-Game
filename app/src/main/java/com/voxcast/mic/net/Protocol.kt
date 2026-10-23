package com.voxcast.mic.net

/**
 * Wire protocol (all local, no server):
 *
 * TCP control channel, one UTF-8 line per message, fields separated by '|':
 *   mic  -> speaker  HELLO|version|token|micName|sampleRate
 *   speaker -> mic   WELCOME|udpPort|sessionId|speakerName   or   REJECT|reason
 *   both             PING|nanoTime  /  PONG|nanoTime   (heartbeat + round-trip time)
 *   mic  -> speaker  TALK|0 or TALK|1                   (on-air state)
 *   both             BYE
 *
 * UDP audio channel, one 10 ms frame per datagram:
 *   [0] magic 'V'  [1] flags  [2..5] sessionId  [6..9] sequence  [10..] PCM 16-bit LE mono
 */
object Protocol {
    const val MAGIC: Byte = 'V'.code.toByte()
    const val HEADER_SIZE = 10
    const val FRAME_MS = 10
    const val HEARTBEAT_MS = 1_000L
    const val PEER_TIMEOUT_MS = 6_000

    const val HELLO = "HELLO"
    const val WELCOME = "WELCOME"
    const val REJECT = "REJECT"
    const val PING = "PING"
    const val PONG = "PONG"
    const val TALK = "TALK"
    const val BYE = "BYE"

    fun line(vararg parts: Any): String = parts.joinToString("|") { it.toString().replace("|", "/").replace("\n", " ") }
    fun split(line: String): List<String> = line.trim().split('|')
}
