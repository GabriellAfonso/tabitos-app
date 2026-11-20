package com.tabitos.app.core.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Typed copy of the app's DataStore entries; the file never carries executable or untyped values. */
internal object BackupPreferences {
    private const val MAX_ENTRIES = 256
    private const val MAX_SET_SIZE = 256

    /** Wire codes are part of format version one and never change. */
    private enum class Type(val code: Int) {
        BOOLEAN(1), INT(2), LONG(3), FLOAT(4), DOUBLE(5), STRING(6), STRING_SET(7);

        companion object {
            fun of(code: Int) = entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException()
        }
    }

    fun encode(preferences: Preferences): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            val entries = preferences.asMap().entries.sortedBy { it.key.name }
            output.writeInt(entries.size)
            entries.forEach { (key, value) ->
                output.writeUTF(key.name)
                writeValue(output, value)
            }
        }
        return bytes.toByteArray()
    }

    /** Parses the whole block first, so a damaged file fails before any preference changes. */
    fun decode(data: ByteArray): (MutablePreferences) -> Unit {
        val writes = mutableListOf<(MutablePreferences) -> Unit>()
        DataInputStream(data.inputStream()).use { input ->
            val count = input.readInt()
            require(count in 0..MAX_ENTRIES)
            repeat(count) { writes += readEntry(input) }
            require(input.read() == -1)
        }
        return { target ->
            target.clear()
            writes.forEach { it(target) }
        }
    }

    private fun writeValue(output: DataOutputStream, value: Any) {
        when (value) {
            is Boolean -> output.tagged(Type.BOOLEAN) { writeBoolean(value) }
            is Int -> output.tagged(Type.INT) { writeInt(value) }
            is Long -> output.tagged(Type.LONG) { writeLong(value) }
            is Float -> output.tagged(Type.FLOAT) { writeFloat(value) }
            is Double -> output.tagged(Type.DOUBLE) { writeDouble(value) }
            is String -> output.tagged(Type.STRING) { writeUTF(value) }
            is Set<*> -> output.tagged(Type.STRING_SET) {
                val items = value.map { it as String }.sorted()
                writeInt(items.size)
                items.forEach(::writeUTF)
            }
            else -> error("Unsupported preference type")
        }
    }

    private fun readEntry(input: DataInputStream): (MutablePreferences) -> Unit {
        val name = input.readUTF()
        val type = Type.of(input.readUnsignedByte())
        return when (type) {
            Type.BOOLEAN -> input.readBoolean().let { value -> { it[booleanPreferencesKey(name)] = value } }
            Type.INT -> input.readInt().let { value -> { it[intPreferencesKey(name)] = value } }
            Type.LONG -> input.readLong().let { value -> { it[longPreferencesKey(name)] = value } }
            Type.FLOAT -> input.readFloat().let { value -> { it[floatPreferencesKey(name)] = value } }
            Type.DOUBLE -> input.readDouble().let { value -> { it[doublePreferencesKey(name)] = value } }
            Type.STRING -> input.readUTF().let { value -> { it[stringPreferencesKey(name)] = value } }
            Type.STRING_SET -> {
                val size = input.readInt()
                require(size in 0..MAX_SET_SIZE)
                val items = List(size) { input.readUTF() }.toSet()
                val write: (MutablePreferences) -> Unit = { it[stringSetPreferencesKey(name)] = items }
                write
            }
        }
    }

    private fun DataOutputStream.tagged(type: Type, body: DataOutputStream.() -> Unit) {
        writeByte(type.code)
        body()
    }
}
