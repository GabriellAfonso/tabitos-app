package com.tabitos.app.core.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupDocumentTest {
    @Test fun documentRoundTripsCaptureInstantRowsAndPreferences() {
        val document = BackupDocument(1_763_570_000_000L, byteArrayOf(1, 2, 3), byteArrayOf(9))
        val decoded = BackupDocument.decode(document.encode())
        assertEquals(document.capturedAt, decoded.capturedAt)
        assertArrayEquals(document.rows, decoded.rows)
        assertArrayEquals(document.preferences, decoded.preferences)
    }

    @Test fun truncatedOrNewerDocumentFailsBeforeAnyReplacement() {
        val encoded = BackupDocument(1L, ByteArray(40), ByteArray(4)).encode()
        assertThrows(IOException::class.java) { BackupDocument.decode(encoded.copyOf(encoded.size - 1)) }
        val newer = encoded.copyOf().also { it[3] = 2 }
        assertThrows(IOException::class.java) { BackupDocument.decode(newer) }
    }

    @Test fun preferencesKeepTypesAndReplaceTheDestinationEntries() {
        val theme = stringPreferencesKey("appearance_mode")
        val gameMode = booleanPreferencesKey("game_mode")
        val dayTime = intPreferencesKey("task_reminder_default_day_time")
        val offsets = stringSetPreferencesKey("task_reminder_default_offsets")
        val source = mutablePreferencesOf(theme to "DARK", gameMode to false, dayTime to 540,
            offsets to setOf("30", "1440"))
        val destination = mutablePreferencesOf(booleanPreferencesKey("timer_alarm") to true)

        BackupPreferences.decode(BackupPreferences.encode(source))(destination)

        assertEquals(source, destination)
    }

    @Test fun damagedPreferencesAreRejectedWhole() {
        val encoded = BackupPreferences.encode(mutablePreferencesOf(stringPreferencesKey("appearance_mode") to "DARK"))
        assertThrows(Exception::class.java) { BackupPreferences.decode(encoded.copyOf(encoded.size - 1)) }
        val unknownType = encoded.copyOf().also { it[it.size - 7] = 99 }
        assertThrows(Exception::class.java) { BackupPreferences.decode(unknownType) }
    }
}
