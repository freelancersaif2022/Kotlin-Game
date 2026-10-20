package com.tvremote.universal.core

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAKeyGenParameterSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

/**
 * The phone's long-lived RSA identity. Android TV pairs with this client certificate,
 * and Fire TV (ADB) trusts its public key.
 */
class ClientIdentity(val privateKey: PrivateKey, val certificate: X509Certificate) {

    val publicKey: RSAPublicKey get() = certificate.publicKey as RSAPublicKey

    fun keyManager(): X509ExtendedKeyManager = object : X509ExtendedKeyManager() {
        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(ALIAS)
        override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = ALIAS
        override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?) = ALIAS
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null
        override fun getCertificateChain(alias: String?): Array<X509Certificate> = arrayOf(certificate)
        override fun getPrivateKey(alias: String?): PrivateKey = privateKey
    }

    companion object {
        private const val ALIAS = "tvremote"
        private const val PREFS = "identity"

        @Volatile private var cached: ClientIdentity? = null

        /** Loads (or creates on first use) the identity. Call from a background thread. */
        fun get(context: Context): ClientIdentity = cached ?: synchronized(this) {
            cached ?: loadOrCreate(context.applicationContext).also { cached = it }
        }

        private fun loadOrCreate(context: Context): ClientIdentity {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val keyB64 = prefs.getString("key", null)
            val certB64 = prefs.getString("cert", null)
            if (keyB64 != null && certB64 != null) {
                runCatching {
                    val key = KeyFactory.getInstance("RSA")
                        .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(keyB64, Base64.NO_WRAP)))
                    val cert = CertificateFactory.getInstance("X.509")
                        .generateCertificate(ByteArrayInputStream(Base64.decode(certB64, Base64.NO_WRAP))) as X509Certificate
                    return ClientIdentity(key, cert)
                }
            }
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4), SecureRandom())
            val kp = kpg.generateKeyPair()
            val der = selfSignedCertificate(kp.public.encoded, kp.private)
            prefs.edit()
                .putString("key", Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
                .putString("cert", Base64.encodeToString(der, Base64.NO_WRAP))
                .apply()
            val cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(der)) as X509Certificate
            return ClientIdentity(kp.private, cert)
        }

        private fun selfSignedCertificate(subjectPublicKeyInfo: ByteArray, key: PrivateKey): ByteArray {
            val name = Der.seq(Der.set(Der.seq(Der.oid(OID_COMMON_NAME), Der.utf8("atvremote"))))
            val now = System.currentTimeMillis()
            val day = 86_400_000L
            val validity = Der.seq(Der.utcTime(now - day), Der.utcTime(now + 3650 * day))
            val algorithm = Der.seq(Der.oid(OID_SHA256_RSA), Der.NULL)
            val extensions = Der.explicit(
                3,
                Der.seq(
                    Der.seq(Der.oid(OID_BASIC_CONSTRAINTS), Der.octet(Der.seq(Der.bool(true), Der.int(BigInteger.ZERO)))),
                    Der.seq(Der.oid(OID_SUBJECT_ALT_NAME), Der.octet(Der.seq(Der.tlv(0x82, "atvremote".toByteArray())))),
                ),
            )
            val tbs = Der.seq(
                Der.explicit(0, Der.int(BigInteger.valueOf(2))),
                Der.int(BigInteger(62, SecureRandom()).add(BigInteger.ONE)),
                algorithm,
                name,
                validity,
                name,
                subjectPublicKeyInfo,
                extensions,
            )
            val signature = Signature.getInstance("SHA256withRSA").run {
                initSign(key)
                update(tbs)
                sign()
            }
            return Der.seq(tbs, algorithm, Der.bitString(signature))
        }

        private val OID_COMMON_NAME = byteArrayOf(0x55, 0x04, 0x03)
        private val OID_SHA256_RSA = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B)
        private val OID_BASIC_CONSTRAINTS = byteArrayOf(0x55, 0x1D, 0x13)
        private val OID_SUBJECT_ALT_NAME = byteArrayOf(0x55, 0x1D, 0x11)
    }
}

/** Tiny DER encoder used to build the self-signed client certificate. */
private object Der {
    val NULL = byteArrayOf(0x05, 0x00)

    fun tlv(tag: Int, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tag)
        val len = content.size
        when {
            len < 0x80 -> out.write(len)
            len < 0x100 -> { out.write(0x81); out.write(len) }
            len < 0x10000 -> { out.write(0x82); out.write(len shr 8); out.write(len and 0xFF) }
            else -> { out.write(0x83); out.write(len shr 16); out.write((len shr 8) and 0xFF); out.write(len and 0xFF) }
        }
        out.write(content)
        return out.toByteArray()
    }

    fun seq(vararg parts: ByteArray) = tlv(0x30, concat(parts))
    fun set(vararg parts: ByteArray) = tlv(0x31, concat(parts))
    fun int(v: BigInteger) = tlv(0x02, v.toByteArray())
    fun oid(encoded: ByteArray) = tlv(0x06, encoded)
    fun utf8(s: String) = tlv(0x0C, s.toByteArray(Charsets.UTF_8))
    fun octet(b: ByteArray) = tlv(0x04, b)
    fun bool(v: Boolean) = tlv(0x01, byteArrayOf(if (v) 0xFF.toByte() else 0))
    fun bitString(b: ByteArray) = tlv(0x03, byteArrayOf(0) + b)
    fun explicit(n: Int, inner: ByteArray) = tlv(0xA0 or n, inner)

    fun utcTime(ms: Long): ByteArray {
        val fmt = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return tlv(0x17, fmt.format(Date(ms)).toByteArray(Charsets.US_ASCII))
    }

    private fun concat(parts: Array<out ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        parts.forEach { out.write(it) }
        return out.toByteArray()
    }
}
