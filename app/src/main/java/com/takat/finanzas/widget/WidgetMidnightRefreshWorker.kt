package com.takat.finanzas.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.util.DebugLog

/**
 * Runs about once a day, scheduled (see TakatApplication) with an initial delay that lands it
 * shortly after local midnight. Exists because the Glance widget's own updatePeriodMillis
 * broadcast is not reliable enough by itself to roll the daily-budget freeze over at midnight —
 * Android (and some OEM battery managers more aggressively than others) can defer that broadcast
 * for hours once the device is idle overnight, so the freeze was only actually happening once the
 * app was next opened by hand. WorkManager/JobScheduler survives Doze noticeably better.
 */
class WidgetMidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DebugLog.log("WidgetMidnightRefreshWorker: doWork start")
        return try {
            val repository = (applicationContext as TakatApplication).repository
            repository.ensureDailyBudgetFrozen()
            WidgetUpdater.refresh(applicationContext)
            DebugLog.log("WidgetMidnightRefreshWorker: doWork success")
            Result.success()
        } catch (e: Exception) {
            DebugLog.log("WidgetMidnightRefreshWorker: doWork failed: ${e.message}")
            Result.retry()
        }
    }
}
