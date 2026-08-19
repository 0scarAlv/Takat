package com.takat.finanzas.util

import com.takat.finanzas.data.entity.BudgetBasis
import com.takat.finanzas.data.entity.BudgetSettingsEntity
import com.takat.finanzas.data.model.AccountTotals
import java.time.LocalDate

data class LiveBudget(
    val nextPaymentDate: LocalDate?,
    val daysRemaining: Long,
    val liveValueCents: Long
)

/** The live "valor diario", recalculated from current settings/totals. Mirrors [dailyBudgetCents]'s formula. */
fun computeLiveBudget(settings: BudgetSettingsEntity?, totals: AccountTotals, today: LocalDate, lastSalaryDate: LocalDate? = null): LiveBudget {
    if (settings == null || !settings.enabled) return LiveBudget(null, 0, 0)
    val nextDate = nextPaymentDate(today, settings.periodType, settings.dayOfMonth, lastSalaryDate)
    val remaining = daysRemaining(today, nextDate)
    val balance = if (settings.basis == BudgetBasis.DISPONIBLE) totals.availableCents else totals.capitalCents
    // +1: spread the balance over today through the payment day inclusive. The payment may not land until
    // later that day (or on the last day of the month), so today still needs its own slice — otherwise the
    // day before payment silently eats the whole remaining balance and payment day itself gets nothing.
    return LiveBudget(nextDate, remaining, dailyBudgetCents(balance, remaining + 1))
}
