package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** MODERN is the 3.0 redesign; CLASSIC keeps the pre-3.0 look for anyone who prefers it. */
enum class UiStyle { MODERN, CLASSIC }

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Persisted SAF tree URI (content://...tree/...) picked via ACTION_OPEN_DOCUMENT_TREE, or null if auto-backup is off. */
    val backupFolderUri: String? = null,
    val lastBackupEpochMillis: Long? = null,
    val lastBackupError: String? = null,
    /** Toggles the joke asides like "(Eres irresponsable financieramente)" and "(te deseo suerte)". */
    val sarcasticMessagesEnabled: Boolean = true,
    /** Highest [com.takat.finanzas.util.ChangelogEntry.versionCode] already shown in the "qué hay de nuevo" dialog. */
    val lastSeenVersionCode: Int = 0,
    /** Nickname used to build the mDNS service/hostname for the PC-access panel (e.g. "oscar" -> takat-oscar.local). */
    val pcAccessNickname: String? = null,
    /** Hides Disponible/Capital total/Gasto fijo/Deuda total behind "••••" on the home screen. */
    val amountsHidden: Boolean = false,
    /**
     * Whether principal still owed on "compra a crédito" plans is subtracted from "Disponible". Off by default:
     * that debt is shown apart ("Deuda de crédito") and only each period's cuota affects what's available.
     */
    val includeFinancedInAvailable: Boolean = false,
    val uiStyle: UiStyle = UiStyle.MODERN
)
