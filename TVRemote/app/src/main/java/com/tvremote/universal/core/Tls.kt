package com.tvremote.universal.core

import android.content.Context
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/** Thrown when a TV presents a different certificate than the one seen at pairing time. */
class CertificateChangedException : CertificateException("The TV's security certificate changed")

/**
 * Trust-on-first-use store. TVs use self-signed certificates, so the first certificate
 * seen at pairing is pinned and every later connection must present the same one.
 */
class CertPins(context: Context) {
    private val prefs = context.getSharedPreferences("cert_pins", Context.MODE_PRIVATE)

    fun get(key: String): String? = prefs.getString(key, null)

    fun put(key: String, fingerprint: String) = prefs.edit().putString(key, fingerprint).apply()

    fun clearHost(host: String) {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("$host:") }.forEach { editor.remove(it) }
        editor.apply()
    }
}

class PinningTrustManager(private val pins: CertPins, private val key: String) : X509TrustManager {

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        if (chain.isNullOrEmpty()) throw CertificateException("Empty certificate chain")
        val leaf = chain[0]
        leaf.checkValidityLenient()
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(leaf.encoded).toHex()
        val pinned = pins.get(key)
        when {
            pinned == null -> pins.put(key, fingerprint)
            pinned != fingerprint -> throw CertificateChangedException()
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client certificates are not accepted")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    /** TV clocks are often wrong; only reject certificates that are not even parseable. */
    private fun X509Certificate.checkValidityLenient() {
        if (publicKey == null) throw CertificateException("Certificate without public key")
    }
}

object Tls {
    fun context(pins: CertPins, key: String, keyManager: KeyManager? = null): Pair<SSLContext, X509TrustManager> {
        val tm = PinningTrustManager(pins, key)
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(keyManager?.let { arrayOf(it) }, arrayOf(tm), SecureRandom())
        return ctx to tm
    }

    /** Opens a TLS socket to [host]:[port] with certificate pinning. */
    fun connect(host: String, port: Int, pins: CertPins, keyManager: KeyManager? = null, timeoutMs: Int = 5000): SSLSocket {
        val (ctx, _) = context(pins, "$host:$port", keyManager)
        val raw = Socket()
        try {
            raw.connect(InetSocketAddress(host, port), timeoutMs)
            raw.soTimeout = timeoutMs
            val ssl = ctx.socketFactory.createSocket(raw, host, port, true) as SSLSocket
            ssl.startHandshake()
            return ssl
        } catch (e: Exception) {
            runCatching { raw.close() }
            throw e
        }
    }

    /** OkHttp client that pins the TV certificate for [host]:[port]. */
    fun okHttp(base: OkHttpClient, host: String, port: Int, pins: CertPins): OkHttpClient {
        val (ctx, tm) = context(pins, "$host:$port")
        return base.newBuilder()
            .sslSocketFactory(ctx.socketFactory, tm)
            .hostnameVerifier { hostname, _ -> hostname == host }
            .build()
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

/** Walks the cause chain looking for a pin mismatch. */
fun Throwable.isCertificateChange(): Boolean {
    var t: Throwable? = this
    while (t != null) {
        if (t is CertificateChangedException) return true
        t = t.cause
    }
    return false
}
