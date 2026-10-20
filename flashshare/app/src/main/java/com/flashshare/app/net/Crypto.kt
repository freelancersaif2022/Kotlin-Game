package com.flashshare.app.net

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM using a key derived from the 6-digit pairing PIN + a random per-session salt from the QR code.
 * AES is hardware accelerated on every ARMv8 phone, so encrypted transfers stay close to full speed.
 */
class CryptoBox(private val keyBytes: ByteArray) {
    private val key = SecretKeySpec(keyBytes, "AES")
    private val random = SecureRandom()
    private val cipher = ThreadLocal.withInitial { Cipher.getInstance("AES/GCM/NoPadding") }

    /** Returns IV(12) + ciphertext + tag(16). */
    fun seal(plain: ByteArray, off: Int = 0, len: Int = plain.size): ByteArray {
        val iv = ByteArray(IV).also { random.nextBytes(it) }
        val c = cipher.get()!!
        c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val out = ByteArray(IV + len + TAG)
        System.arraycopy(iv, 0, out, 0, IV)
        c.doFinal(plain, off, len, out, IV)
        return out
    }

    /** Decrypts IV + ciphertext + tag into [dst]; returns the plain length. */
    fun open(data: ByteArray, len: Int = data.size, dst: ByteArray? = null): ByteArray {
        val c = cipher.get()!!
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data, 0, IV))
        if (dst != null) {
            c.doFinal(data, IV, len - IV, dst, 0)
            return dst
        }
        return c.doFinal(data, IV, len - IV)
    }

    /** Encrypts plain[0, len) into out as IV + ciphertext + tag; returns the sealed length. */
    fun sealInto(plain: ByteArray, len: Int, out: ByteArray): Int {
        val iv = ByteArray(IV).also { random.nextBytes(it) }
        val c = cipher.get()!!
        c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        System.arraycopy(iv, 0, out, 0, IV)
        return IV + c.doFinal(plain, 0, len, out, IV)
    }

    /** Decrypts a sealed frame of [len] bytes into [plainOut]; returns the plain length. */
    fun openInto(sealed: ByteArray, len: Int, plainOut: ByteArray): Int {
        val c = cipher.get()!!
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, sealed, 0, IV))
        return c.doFinal(sealed, IV, len - IV, plainOut, 0)
    }

    fun proof(nonce: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(keyBytes, "HmacSHA256"))
        update("flashshare-auth".toByteArray())
        doFinal(nonce)
    }

    companion object {
        const val IV = 12
        const val TAG = 16

        fun derive(pin: String, salt: ByteArray): CryptoBox {
            val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val k = f.generateSecret(PBEKeySpec(pin.toCharArray(), salt, 20_000, 256)).encoded
            return CryptoBox(k)
        }

        fun newPin(): String = (SecureRandom().nextInt(900_000) + 100_000).toString()

        fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }
    }
}
