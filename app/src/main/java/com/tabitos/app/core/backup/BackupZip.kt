package com.tabitos.app.core.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object BackupZip {
    private const val ENTRY = "routine-v1.bin"
    private const val SHA256_HEX_LENGTH = 64
    private const val HEX = "0123456789abcdef"
    private const val END_RECORD_SIZE = 22
    private const val END_SIGNATURE = 0x06054b50
    private const val DIRECTORY_SIGNATURE = 0x02014b50
    private const val DIRECTORY_MIN_SIZE = 46
    private const val DISK_INFORMATION_OFFSET = 4
    private const val ENTRIES_ON_DISK_OFFSET = 8
    private const val TOTAL_ENTRIES_OFFSET = 10
    private const val DIRECTORY_SIZE_OFFSET = 12
    private const val DIRECTORY_POSITION_OFFSET = 16
    private const val COMMENT_SIZE_OFFSET = 20

    fun pack(payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.setComment("0".repeat(SHA256_HEX_LENGTH))
            zip.putNextEntry(ZipEntry(ENTRY))
            zip.write(payload)
            zip.closeEntry()
        }
        return output.toByteArray().also { bytes ->
            val commentStart = bytes.size - SHA256_HEX_LENGTH
            checksum(bytes, commentStart).copyInto(bytes, destinationOffset = commentStart)
        }
    }

    fun unpack(bytes: ByteArray): ByteArray {
        verifyDirectory(bytes)
        return ZipInputStream(bytes.inputStream()).use { zip ->
            if (zip.nextEntry?.name != ENTRY) throw IOException("Unsupported backup payload")
            val payload = zip.readLimited(BackupArchive.MAX_PAYLOAD_BYTES)
            if (zip.nextEntry != null) throw IOException("Unexpected backup entry")
            payload
        }
    }

    // ZipInputStream can return a complete local entry from a truncated file with no central directory.
    // The standard ZIP comment covers the whole archive (including its central directory) up to the comment.
    private fun verifyDirectory(bytes: ByteArray) {
        val commentStart = bytes.size - SHA256_HEX_LENGTH
        val end = commentStart - END_RECORD_SIZE
        if (end < DIRECTORY_MIN_SIZE) throw IOException("Incomplete backup")
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val directoryOffset = buffer.getInt(end + DIRECTORY_POSITION_OFFSET)
        val directorySize = buffer.getInt(end + DIRECTORY_SIZE_OFFSET)
        val complete = buffer.getInt(end) == END_SIGNATURE &&
            buffer.getInt(end + DISK_INFORMATION_OFFSET) == 0 &&
            buffer.getShort(end + ENTRIES_ON_DISK_OFFSET).toInt() == 1 &&
            buffer.getShort(end + TOTAL_ENTRIES_OFFSET).toInt() == 1 &&
            buffer.getShort(end + COMMENT_SIZE_OFFSET).toInt() == SHA256_HEX_LENGTH &&
            directoryOffset in 0..end - DIRECTORY_MIN_SIZE &&
            directorySize == end - directoryOffset &&
            buffer.getInt(directoryOffset) == DIRECTORY_SIGNATURE
        if (!complete) throw IOException("Invalid backup directory")
        val expected = checksum(bytes, commentStart)
        if (!expected.indices.all { expected[it] == bytes[commentStart + it] }) throw IOException("Damaged backup")
    }

    private fun checksum(bytes: ByteArray, length: Int): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256").apply { update(bytes, 0, length) }.digest()
        return buildString(SHA256_HEX_LENGTH) {
            for (value in digest) {
                val unsigned = value.toInt() and 0xff
                append(HEX[unsigned ushr 4])
                append(HEX[unsigned and 0xf])
            }
        }.toByteArray(Charsets.US_ASCII)
    }
}
