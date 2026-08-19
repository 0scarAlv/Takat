package com.takat.finanzas.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.util.DebugLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val FILE_PREFIX = "takat_backup_"

/** How many daily backups to keep in the folder before pruning the oldest ones. */
private const val RETENTION_COUNT = 30

/**
 * Runs about once a day. No-ops if the user hasn't picked a backup folder in Settings. Writes the
 * same .zip format as the manual "Exportar datos" button, overwriting today's file if it already
 * ran once today, and prunes anything past [RETENTION_COUNT] so the folder doesn't grow forever.
 */
class DailyBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DebugLog.log("DailyBackupWorker: doWork start")
        val repository = (applicationContext as TakatApplication).repository
        val settings = repository.appSettings().first() ?: return Result.success()
        val folderUriString = settings.backupFolderUri ?: return Result.success()

        val folder = DocumentFile.fromTreeUri(applicationContext, Uri.parse(folderUriString))
        if (folder == null || !folder.canWrite()) {
            repository.updateAppSettings(
                settings.copy(lastBackupError = "No se pudo escribir en la carpeta elegida. Volvé a elegirla en Ajustes.")
            )
            return Result.failure()
        }

        return try {
            val fileName = "$FILE_PREFIX${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.zip"
            folder.findFile(fileName)?.delete()
            val file = folder.createFile("application/zip", fileName)
                ?: error("No se pudo crear el archivo de respaldo")
            applicationContext.contentResolver.openOutputStream(file.uri)?.use { output ->
                repository.exportBackup(output)
            } ?: error("No se pudo abrir el archivo de respaldo")

            pruneOldBackups(folder)

            repository.updateAppSettings(
                settings.copy(lastBackupEpochMillis = System.currentTimeMillis(), lastBackupError = null)
            )
            DebugLog.log("DailyBackupWorker: doWork success")
            Result.success()
        } catch (e: Exception) {
            repository.updateAppSettings(settings.copy(lastBackupError = e.message ?: "Error desconocido"))
            DebugLog.log("DailyBackupWorker: doWork failed: ${e.message}")
            Result.retry()
        }
    }

    private fun pruneOldBackups(folder: DocumentFile) {
        folder.listFiles()
            .filter { it.name?.startsWith(FILE_PREFIX) == true }
            .sortedByDescending { it.name }
            .drop(RETENTION_COUNT)
            .forEach { it.delete() }
    }
}
