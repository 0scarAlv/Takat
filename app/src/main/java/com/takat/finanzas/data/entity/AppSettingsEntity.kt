package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Persisted SAF tree URI (content://...tree/...) picked via ACTION_OPEN_DOCUMENT_TREE, or null if auto-backup is off. */
    val backupFolderUri: String? = null,
    val lastBackupEpochMillis: Long? = null,
    val lastBackupError: String? = null,
    /** Toggles the joke asides like "(Eres irresponsable financieramente)" and "(te deseo suerte)". */
    val sarcasticMessagesEnabled: Boolean = true
)
