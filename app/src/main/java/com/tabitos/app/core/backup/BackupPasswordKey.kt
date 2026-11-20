package com.tabitos.app.core.backup

import java.nio.CharBuffer
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupPasswordKey {
    private const val ITERATIONS = 600_000
    private const val KEY_BITS = 256
    private const val MAC_ALGORITHM = "HmacSHA256"

    fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: NoSuchAlgorithmException) {
            return deriveWithMac(password, salt, ITERATIONS)
        }
        val specification = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            factory.generateSecret(specification).encoded
        } finally {
            specification.clearPassword()
        }
    }

    // Android 24/25 provides HmacSHA256 but lacks the PBKDF2 SHA-256 factory. RFC 8018 section 5.2
    // needs exactly one 32-byte block for an AES-256 key, using the UTF-8 password on both providers.
    internal fun deriveWithMac(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val encoded = Charsets.UTF_8.encode(CharBuffer.wrap(password))
        val mac = Mac.getInstance(MAC_ALGORITHM)
        try {
            mac.init(SecretKeySpec(encoded.array(), encoded.arrayOffset(), encoded.remaining(), MAC_ALGORITHM))
        } finally {
            encoded.array().fill(0)
        }
        mac.update(salt)
        val round = mac.doFinal(byteArrayOf(0, 0, 0, 1))
        val key = round.copyOf()
        try {
            repeat(iterations - 1) {
                mac.update(round)
                mac.doFinal(round, 0)
                for (i in key.indices) key[i] = (key[i].toInt() xor round[i].toInt()).toByte()
            }
            return key
        } finally {
            round.fill(0)
        }
    }
}
