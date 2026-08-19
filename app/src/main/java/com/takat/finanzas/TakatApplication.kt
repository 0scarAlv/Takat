package com.takat.finanzas

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.takat.finanzas.backup.DailyBackupWorker
import com.takat.finanzas.data.AppDatabase
import com.takat.finanzas.data.attachment.AttachmentStorage
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.notifications.FixedExpenseReminderWorker
import com.takat.finanzas.notifications.NotificationHelper
import com.takat.finanzas.util.DebugLog
import com.takat.finanzas.widget.WidgetMidnightRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class TakatApplication : Application() {
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        DebugLog.init(this)
        DebugLog.log("TakatApplication.onCreate")
        repository = FinanceRepository(AppDatabase.getInstance(this), AttachmentStorage(this), this)
        CoroutineScope(Dispatchers.IO).launch { repository.ensureDailyBudgetFrozen() }
        NotificationHelper.createChannel(this)
        val request = PeriodicWorkRequestBuilder<FixedExpenseReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "fixed_expense_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )

        // Aligns the periodic run to just after local midnight instead of "24h after whenever the
        // app first launched" — see WidgetMidnightRefreshWorker for why this exists.
        val initialDelay = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay())
        DebugLog.log("TakatApplication: scheduling widget midnight refresh, initialDelay=${initialDelay.toMinutes()}min")
        val widgetRefreshRequest = PeriodicWorkRequestBuilder<WidgetMidnightRefreshWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget_midnight_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            widgetRefreshRequest
        )
    }
}
