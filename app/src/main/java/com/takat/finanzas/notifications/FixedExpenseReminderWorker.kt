package com.takat.finanzas.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.data.model.FixedExpensePeriod
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Runs about once a day; notifies for each enabled fixed expense whose notify-day is today and hasn't been flagged yet this period. */
class FixedExpenseReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as TakatApplication).repository
        val today = LocalDate.now()

        repository.pendingFixedExpenses().first()
            .filter { it.fixedExpense.enabled && it.fixedExpense.notifyEnabled && it.isPending }
            .filter { FixedExpensePeriod.isNotifyDay(it.fixedExpense.frequency, it.fixedExpense.dayOfMonth, today) }
            .filter { !repository.wasFixedExpenseNotified(it.fixedExpense.id, it.periodKey) }
            .forEach { pending ->
                NotificationHelper.notifyDue(applicationContext, pending.fixedExpense.id, pending.fixedExpense.name, pending.fixedExpense.amountCents)
                repository.markFixedExpenseNotified(pending.fixedExpense.id, pending.periodKey, System.currentTimeMillis())
            }

        return Result.success()
    }
}
