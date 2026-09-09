package com.takat.finanzas.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.util.DebugLog

/**
 * Runs about once a day (see [runBackupNow] for the actual write/prune logic, shared with any
 * on-demand trigger). No-ops if the user hasn't picked a backup folder in Settings.
 */
class DailyBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DebugLog.log("DailyBackupWorker: doWork start")
        val repository = (applicationContext as TakatApplication).repository
        return when (val outcome = runBackupNow(applicationContext, repository)) {
            is BackupOutcome.NoFolderConfigured, is BackupOutcome.Success -> {
                DebugLog.log("DailyBackupWorker: doWork success ($outcome)")
                Result.success()
            }
            is BackupOutcome.FolderUnwritable -> {
                DebugLog.log("DailyBackupWorker: doWork failed (folder unwritable): ${outcome.message}")
                Result.failure()
            }
            is BackupOutcome.WriteFailed -> {
                DebugLog.log("DailyBackupWorker: doWork failed: ${outcome.message}")
                Result.retry()
            }
        }
    }
}
