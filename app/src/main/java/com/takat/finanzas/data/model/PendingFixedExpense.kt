package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseEntity

/** A [FixedExpenseEntity] resolved against the current period: either its stored exception row, or the implicit default (active, unpaid). */
data class PendingFixedExpense(
    val fixedExpense: FixedExpenseEntity,
    val periodKey: String,
    val active: Boolean,
    val paidCents: Long,
    val lastPaymentTransactionId: Long?,
    val countsTowardTotal: Boolean
) {
    val remainingCents: Long get() = (fixedExpense.amountCents - paidCents).coerceAtLeast(0)
    val isPending: Boolean get() = active && remainingCents > 0
}
