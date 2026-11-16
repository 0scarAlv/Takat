package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.entity.TransferEntity

/**
 * One payment toward a fixed-expense rule, whatever paid it: an expense transaction (regular rules) or a
 * transfer (recurring-transfer rules). [amountCents] is always the positive amount credited to the rule.
 */
data class FixedExpensePayment(
    val fixedExpenseId: Long,
    val periodKey: String?,
    val amountCents: Long,
    val date: Long,
    val transactionId: Long?,
    val transferId: Long?
) {
    companion object {
        fun from(transactions: List<TransactionEntity>, transfers: List<TransferEntity>): List<FixedExpensePayment> =
            transactions.mapNotNull { tx ->
                tx.fixedExpenseId?.let { FixedExpensePayment(it, tx.fixedExpensePeriodKey, -tx.amountCents, tx.date, tx.id, null) }
            } + transfers.mapNotNull { tr ->
                tr.fixedExpenseId?.let { FixedExpensePayment(it, tr.fixedExpensePeriodKey, tr.amountCents, tr.date, null, tr.id) }
            }
    }
}
