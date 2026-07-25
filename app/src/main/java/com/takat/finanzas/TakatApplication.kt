package com.takat.finanzas

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.takat.finanzas.data.AppDatabase
import com.takat.finanzas.data.attachment.AttachmentStorage
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.notifications.FixedExpenseReminderWorker
import com.takat.finanzas.notifications.NotificationHelper
import java.util.concurrent.TimeUnit

class TakatApplication : Application() {
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FinanceRepository(AppDatabase.getInstance(this), AttachmentStorage(this), this)
        NotificationHelper.createChannel(this)
        val request = PeriodicWorkRequestBuilder<FixedExpenseReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "fixed_expense_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
