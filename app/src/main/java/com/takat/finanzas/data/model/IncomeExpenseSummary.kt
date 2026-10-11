package com.takat.finanzas.data.model

import java.time.LocalDate

data class IncomeExpenseSummary(
    val incomeCents: Long,
    val expenseCents: Long
)

/** One calendar day of the Ingresos vs Gastos report (salary reattribution already applied). */
data class DailyFlow(val date: LocalDate, val incomeCents: Long, val expenseCents: Long)
