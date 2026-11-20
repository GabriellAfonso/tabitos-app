package com.tabitos.app.core.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupArchiveTest {
    private val routine = "Tarefa, café e compras da semana".toByteArray()
    private val password = "rotina 🗓 ç".toCharArray()

    @Test
    fun plainFileOpensWithAStandardZipReaderAndRestoresWithoutAName() {
        val archive = export()
        assertFalse(BackupArchive.isProtected(archive.inputStream()))
        ZipInputStream(archive.inputStream()).use { zip ->
            assertEquals("routine-v1.bin", zip.nextEntry.name)
            assertArrayEquals(routine, zip.readBytes())
        }
        assertArrayEquals(routine, BackupArchive.read(archive.inputStream(), null))
    }

    @Test
    fun encryptedExportsArePortableAndUseFreshSaltAndNonce() {
        val first = export(password)
        val second = export(password)
        assertTrue(BackupArchive.isProtected(first.inputStream()))
        assertFalse(first.contentEquals(second))
        assertFalse(first.toString(StandardCharsets.ISO_8859_1).contains("routine-v1.bin"))
        assertArrayEquals(routine, BackupArchive.read(second.inputStream(), password))
        assertThrows(BackupPasswordRequired::class.java) { BackupArchive.read(first.inputStream(), null) }
        assertThrows(IOException::class.java) { BackupArchive.read(first.inputStream(), "wrong".toCharArray()) }
        first[first.lastIndex] = (first.last().toInt() xor 1).toByte()
        assertThrows(IOException::class.java) { BackupArchive.read(first.inputStream(), password) }
    }

    @Test
    fun damagedDataMissingZipFooterAndUnknownFormatsAreRejected() {
        val whole = export()
        val damaged = whole.copyOf().apply { this[40] = (this[40].toInt() xor 1).toByte() }
        val invalidFiles = listOf(damaged, whole.copyOf(whole.size - 1), whole + 0, byteArrayOf(1, 2, 3))
        for (bytes in invalidFiles) {
            assertThrows(IOException::class.java) { BackupArchive.read(bytes.inputStream(), null) }
        }
    }

    @Test
    fun android24KeyDerivationMatchesStandardPbkdf2Sha256Vector() {
        val bytes = BackupPasswordKey.deriveWithMac("password".toCharArray(), "salt".toByteArray(), 2)
        val expectedHex = "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43"
        assertArrayEquals(expectedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), bytes)
    }

    private fun export(secret: CharArray? = null): ByteArray = ByteArrayOutputStream().use { output ->
        BackupArchive.write(output, routine, secret)
        output.toByteArray()
    }
}
