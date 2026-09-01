package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseEntity

/** A [FixedExpenseEntity] resolved against the current period: either its stored exception row, or the implicit default (active, unpaid). */
data class PendingFixedExpense(
    val fixedExpense: FixedExpenseEntity,
    val periodKey: String,
    val active: Boolean,
    /** Whether this period has begun mattering yet — see [FixedExpensePeriod.hasPeriodStarted]. */
    val started: Boolean,
    val paidCents: Long,
    /** Sum of every payment ever made toward this rule, across all periods — only meaningful when it's a debt. */
    val totalPaidCents: Long,
    val lastPaymentTransactionId: Long?,
    val countsTowardTotal: Boolean
) {
    /** Null when this rule isn't a debt payment plan. */
    val debtRemainingCents: Long? get() = fixedExpense.totalDebtCents?.let { (it - totalPaidCents).coerceAtLeast(0) }

    /**
     * What's left to pay this period. For a debt, capped by [debtRemainingCents] so the last installment
     * (or an early payoff from prior overpayments) never asks for more than what's actually still owed —
     * cuotas themselves are never reduced, only this final-period display shrinks.
     */
    val remainingCents: Long get() {
        val periodRemaining = (fixedExpense.amountCents - paidCents).coerceAtLeast(0)
        val debtRemaining = debtRemainingCents ?: return periodRemaining
        return minOf(periodRemaining, debtRemaining)
    }

    val isPending: Boolean get() = active && started && remainingCents > 0
}
