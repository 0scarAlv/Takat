package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseEntity

/** A [FixedExpenseEntity] resolved against the current period: either its stored exception row, or the implicit default (active, unpaid). */
data class PendingFixedExpense(
    val fixedExpense: FixedExpenseEntity,
    val periodKey: String,
    val active: Boolean,
    val paidTransactionId: Long?,
    val countsTowardTotal: Boolean
) {
    val isPending: Boolean get() = active && paidTransactionId == null
}
