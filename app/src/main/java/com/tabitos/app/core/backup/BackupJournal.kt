package com.tabitos.app.core.backup

import android.content.Context
import androidx.core.util.AtomicFile
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Preferences of a restore in progress, with the fingerprint of the rows that restore commits. */
class PendingRestore(val fingerprint: ByteArray, val preferences: ByteArray)

/**
 * Private recovery file outside Android's cloud backup, so it never travels with the installation. AtomicFile
 * guarantees a reader sees either the previous state or the complete new journal.
 */
@Singleton
class BackupJournal @Inject constructor(@param:ApplicationContext context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))

    fun exists(): Boolean = file.baseFile.exists()

    fun write(fingerprint: ByteArray, preferences: ByteArray) {
        val output = file.startWrite()
        try {
            DataOutputStream(output).apply {
                writeInt(fingerprint.size)
                write(fingerprint)
                writeInt(preferences.size)
                write(preferences)
                flush()
            }
            file.finishWrite(output)
        } catch (failure: IOException) {
            file.failWrite(output)
            throw failure
        }
    }

    fun read(): PendingRestore? {
        if (!exists()) return null
        return try {
            DataInputStream(file.openRead()).use { input ->
                PendingRestore(input.block(), input.block())
            }
        } catch (_: IOException) {
            null
        }
    }

    fun clear() = file.delete()

    private fun DataInputStream.block(): ByteArray {
        val size = readInt()
        if (size !in 0..available()) throw IOException("Damaged journal")
        return ByteArray(size).also(::readFully)
    }

    private companion object {
        const val FILE_NAME = "backup-restore.journal"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(implementation: RoomBackupRepository): BackupRepository
}
