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
fun computeLiveBudget(settings: BudgetSettingsEntity?, totals: AccountTotals, today: LocalDate): LiveBudget {
    if (settings == null || !settings.enabled) return LiveBudget(null, 0, 0)
    val nextDate = nextPaymentDate(today, settings.periodType, settings.dayOfMonth)
    val remaining = daysRemaining(today, nextDate)
    val balance = if (settings.basis == BudgetBasis.DISPONIBLE) totals.availableCents else totals.capitalCents
    return LiveBudget(nextDate, remaining, dailyBudgetCents(balance, remaining))
}
