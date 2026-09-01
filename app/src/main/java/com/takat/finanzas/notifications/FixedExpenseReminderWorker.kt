package com.takat.finanzas.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.data.model.FixedExpensePeriod
import com.takat.finanzas.data.model.ReminderStage
import com.takat.finanzas.util.DebugLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Runs about once a day. QUINCENAL rules get a single due-day notification, same as before.
 * MENSUAL rules get up to three independent notifications per period: a day-before heads-up, the
 * due-day notification, and — only if still unpaid — a one-shot follow-up two days after due.
 */
class FixedExpenseReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DebugLog.log("FixedExpenseReminderWorker: doWork start")
        val repository = (applicationContext as TakatApplication).repository
        val today = LocalDate.now()

        val rules = repository.fixedExpenses().first().filter { it.enabled && it.notifyEnabled }
        val pendingByRuleId = repository.pendingFixedExpenses().first().associateBy { it.fixedExpense.id }

        rules.forEach { rule ->
            when (rule.frequency) {
                FixedExpenseFrequency.QUINCENAL -> {
                    if (!FixedExpensePeriod.isQuincenalNotifyDay(today)) return@forEach
                    val pending = pendingByRuleId[rule.id] ?: return@forEach
                    if (!pending.isPending) return@forEach
                    if (repository.wasFixedExpenseNotified(rule.id, pending.periodKey, ReminderStage.DUE)) return@forEach
                    NotificationHelper.notify(applicationContext, FixedExpenseNotificationKind.DUE, rule.id, rule.name, pending.remainingCents)
                    repository.markFixedExpenseNotified(rule.id, pending.periodKey, ReminderStage.DUE, System.currentTimeMillis())
                }

                FixedExpenseFrequency.MENSUAL -> {
                    val (periodKey, stage) = FixedExpensePeriod.mensualReminderStage(rule.dayOfMonth, today) ?: return@forEach
                    if (repository.wasFixedExpenseNotified(rule.id, periodKey, stage)) return@forEach

                    when (stage) {
                        ReminderStage.PRE_DUE -> {
                            NotificationHelper.notify(applicationContext, FixedExpenseNotificationKind.UPCOMING, rule.id, rule.name, rule.amountCents)
                            repository.markFixedExpenseNotified(rule.id, periodKey, stage, System.currentTimeMillis())
                        }
                        ReminderStage.DUE -> {
                            val pending = pendingByRuleId[rule.id]
                            if (pending?.isPending != true) return@forEach
                            NotificationHelper.notify(applicationContext, FixedExpenseNotificationKind.DUE, rule.id, rule.name, pending.remainingCents)
                            repository.markFixedExpenseNotified(rule.id, periodKey, stage, System.currentTimeMillis())
                        }
                        ReminderStage.FOLLOW_UP -> {
                            val remainingCents = repository.remainingCentsForPeriod(rule, periodKey)
                            if (remainingCents <= 0) return@forEach
                            NotificationHelper.notify(applicationContext, FixedExpenseNotificationKind.OVERDUE, rule.id, rule.name, remainingCents)
                            repository.markFixedExpenseNotified(rule.id, periodKey, stage, System.currentTimeMillis())
                        }
                    }
                }
            }
        }

        DebugLog.log("FixedExpenseReminderWorker: doWork success")
        return Result.success()
    }
}
