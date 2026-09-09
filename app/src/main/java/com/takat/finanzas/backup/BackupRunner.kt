package com.takat.finanzas.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val FILE_PREFIX = "takat_backup_"

/** How many daily backups to keep in the folder before pruning the oldest ones. */
private const val RETENTION_COUNT = 30

sealed class BackupOutcome {
    data object NoFolderConfigured : BackupOutcome()
    data object Success : BackupOutcome()
    data class FolderUnwritable(val message: String) : BackupOutcome()
    data class WriteFailed(val message: String) : BackupOutcome()
}

/**
 * Writes today's backup .zip into the folder saved in Settings (same format as the manual
 * "Exportar datos" button), overwriting today's file if it already ran once today, and prunes
 * anything past [RETENTION_COUNT]. Shared by [DailyBackupWorker]'s scheduled run and any on-demand
 * trigger (e.g. right before installing an app update).
 */
suspend fun runBackupNow(context: Context, repository: FinanceRepository): BackupOutcome {
    val settings = repository.appSettings().first() ?: return BackupOutcome.NoFolderConfigured
    val folderUriString = settings.backupFolderUri ?: return BackupOutcome.NoFolderConfigured

    val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUriString))
    if (folder == null || !folder.canWrite()) {
        val message = "No se pudo escribir en la carpeta elegida. Volvé a elegirla en Ajustes."
        repository.updateAppSettings(settings.copy(lastBackupError = message))
        return BackupOutcome.FolderUnwritable(message)
    }

    return try {
        val fileName = "$FILE_PREFIX${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.zip"
        folder.findFile(fileName)?.delete()
        val file = folder.createFile("application/zip", fileName)
            ?: error("No se pudo crear el archivo de respaldo")
        context.contentResolver.openOutputStream(file.uri)?.use { output ->
            repository.exportBackup(output)
        } ?: error("No se pudo abrir el archivo de respaldo")

        pruneOldBackups(folder)

        repository.updateAppSettings(
            settings.copy(lastBackupEpochMillis = System.currentTimeMillis(), lastBackupError = null)
        )
        BackupOutcome.Success
    } catch (e: Exception) {
        val message = e.message ?: "Error desconocido"
        repository.updateAppSettings(settings.copy(lastBackupError = message))
        BackupOutcome.WriteFailed(message)
    }
}

private fun pruneOldBackups(folder: DocumentFile) {
    folder.listFiles()
        .filter { it.name?.startsWith(FILE_PREFIX) == true }
        .sortedByDescending { it.name }
        .drop(RETENTION_COUNT)
        .forEach { it.delete() }
}
