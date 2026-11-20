package com.tabitos.app.core.backup

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** Content of a backup inside the archive: capture instant, database rows and preferences. */
internal class BackupDocument(val capturedAt: Long, val rows: ByteArray, val preferences: ByteArray) {
    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(FORMAT)
            output.writeLong(capturedAt)
            output.block(rows)
            output.block(preferences)
        }
        return bytes.toByteArray()
    }

    companion object {
        /** Bumped only when the layout below changes; newer files are rejected before any replacement. */
        private const val FORMAT = 1

        fun decode(payload: ByteArray): BackupDocument = try {
            DataInputStream(payload.inputStream()).use { input ->
                if (input.readInt() != FORMAT) throw IOException("Unsupported backup format")
                val capturedAt = input.readLong()
                val document = BackupDocument(capturedAt, input.block(), input.block())
                if (input.read() != -1) throw IOException("Unexpected backup content")
                document
            }
        } catch (failure: IOException) {
            throw failure
        } catch (failure: RuntimeException) {
            throw IOException("Invalid backup", failure)
        }

        private fun DataOutputStream.block(bytes: ByteArray) {
            writeInt(bytes.size)
            write(bytes)
        }

        private fun DataInputStream.block(): ByteArray {
            val size = readInt()
            if (size !in 0..available()) throw IOException("Incomplete backup")
            return ByteArray(size).also(::readFully)
        }
    }
}
