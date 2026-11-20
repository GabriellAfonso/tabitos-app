package com.tabitos.app.core.backup

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class BackupPasswordRequired : IOException()
internal class BackupAuthenticationFailed(cause: Throwable) : IOException(cause)

/** Version one of the on-disk envelope. Its key and identifiers are independent of the Android installation. */
internal object BackupArchive {
    const val MAX_PAYLOAD_BYTES = 32 * 1024 * 1024
    // Allows an incompressible payload, DEFLATE overhead, ZIP directories and the encryption envelope.
    const val MAX_ARCHIVE_BYTES = MAX_PAYLOAD_BYTES + 128 * 1024

    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private val magic = "TABITOS!".toByteArray(Charsets.US_ASCII)
    private val zipSignature = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
    private val random = SecureRandom()

    fun isProtected(input: InputStream): Boolean {
        val prefix = ByteArray(magic.size)
        DataInputStream(input).readFully(prefix)
        return when {
            prefix.contentEquals(magic) -> true
            zipSignature.indices.all { prefix[it] == zipSignature[it] } -> false
            else -> throw IOException("Unknown backup format")
        }
    }

    fun write(output: OutputStream, payload: ByteArray, password: CharArray?) {
        if (payload.size > MAX_PAYLOAD_BYTES) throw IOException("Backup exceeds supported size")
        require(password == null || password.isNotEmpty())
        val zip = BackupZip.pack(payload)
        if (password == null) {
            output.write(zip)
            return
        }
        val salt = randomBytes(SALT_BYTES)
        val nonce = randomBytes(NONCE_BYTES)
        val header = magic + VERSION + salt + nonce
        val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, nonce, header)
        output.write(header)
        output.write(cipher.doFinal(zip))
    }

    fun read(input: InputStream, password: CharArray?): ByteArray {
        val source = input.buffered()
        source.mark(magic.size)
        val protected = isProtected(source)
        source.reset()
        if (!protected) return BackupZip.unpack(source.readLimited(MAX_ARCHIVE_BYTES))
        val header = ByteArray(magic.size + 1 + SALT_BYTES + NONCE_BYTES)
        DataInputStream(source).readFully(header)
        if (header[magic.size] != VERSION) throw IOException("Unsupported protected backup version")
        if (password == null) throw BackupPasswordRequired()
        val saltOffset = magic.size + 1
        val nonceOffset = saltOffset + SALT_BYTES
        val salt = header.copyOfRange(saltOffset, nonceOffset)
        val nonce = header.copyOfRange(nonceOffset, header.size)
        val encrypted = source.readLimited(MAX_ARCHIVE_BYTES - header.size)
        val zip = try {
            cipher(Cipher.DECRYPT_MODE, password, salt, nonce, header).doFinal(encrypted)
        } catch (failure: GeneralSecurityException) {
            throw BackupAuthenticationFailed(failure)
        }
        return BackupZip.unpack(zip)
    }

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, nonce: ByteArray, header: ByteArray): Cipher {
        val key = BackupPasswordKey.derive(password, salt)
        try {
            return Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
                updateAAD(header)
            }
        } finally {
            key.fill(0)
        }
    }

    private fun randomBytes(count: Int) = ByteArray(count).also(random::nextBytes)
}

internal fun InputStream.readLimited(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        if (count > limit - output.size()) throw IOException("Backup exceeds supported size")
        output.write(buffer, 0, count)
    }
}
