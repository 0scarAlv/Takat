package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseEntity
import com.takat.finanzas.data.entity.TransactionEntity

/** A past period of a fixed expense that was actually paid, for the "Historial" report. */
data class FixedExpensePaymentRecord(
    val fixedExpense: FixedExpenseEntity,
    val periodKey: String,
    val transaction: TransactionEntity
)
