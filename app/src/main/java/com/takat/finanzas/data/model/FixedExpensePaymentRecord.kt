package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseEntity

/** A period of a fixed expense with at least one payment toward it, for the "Historial" report. */
data class FixedExpensePaymentRecord(
    val fixedExpense: FixedExpenseEntity,
    val periodKey: String,
    val paidCents: Long,
    val lastPaymentDate: Long
) {
    val isComplete: Boolean get() = paidCents >= fixedExpense.amountCents
}
