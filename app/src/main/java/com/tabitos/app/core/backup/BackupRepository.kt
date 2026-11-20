package com.tabitos.app.core.backup

interface BackupRepository {
    suspend fun export(document: String, password: CharArray?)
    suspend fun inspect(document: String, password: CharArray?): BackupInspection
    suspend fun restore(preview: BackupPreview, confirmedReplacement: Boolean)
}

sealed interface BackupInspection {
    data object PasswordRequired : BackupInspection
    data class Ready(val preview: BackupPreview) : BackupInspection
}

class BackupPreview internal constructor(
    val capturedAt: Long,
    val lists: Int,
    val tasks: Int,
    val habits: Int,
    val hasPersonalData: Boolean,
    internal val rows: ByteArray,
    internal val preferences: ByteArray,
)

class ReplacementRequired : Exception()
