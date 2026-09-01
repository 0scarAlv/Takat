package com.takat.finanzas.data.csv

import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.FixedExpenseFrequency

data class AccountRow(
    val name: String,
    val initialBalanceCents: Long,
    val isDebt: Boolean,
    val includeInTotal: Boolean,
    val colorArgb: Int
)

data class CategoryRow(
    val name: String,
    val emoji: String,
    val kind: CategoryKind
)

data class TransactionRow(
    val date: Long,
    val accountName: String,
    val categoryName: String?,
    val amountCents: Long,
    val note: String?,
    /** Filenames (inside the backup zip's adjuntos/ folder) of receipts attached to this transaction. */
    val attachmentEntries: List<String> = emptyList()
)

data class TransferRow(
    val date: Long,
    val fromAccountName: String,
    val toAccountName: String,
    val categoryName: String?,
    val amountCents: Long,
    val note: String?
)

data class FixedExpenseRow(
    val name: String,
    val amountCents: Long,
    val accountName: String,
    val categoryName: String?,
    val frequency: FixedExpenseFrequency,
    val dayOfMonth: Int,
    val quincenaOnly: Boolean,
    val notifyEnabled: Boolean,
    val enabled: Boolean,
    val totalDebtCents: Long?,
    val installmentsCount: Int?
)

/** References its [FixedExpenseRow] by name, the same way [TransactionRow] references accounts/categories. */
data class FixedExpensePeriodStateRow(
    val fixedExpenseName: String,
    val periodKey: String,
    val active: Boolean
)

data class ParsedBackup(
    val accounts: List<AccountRow>,
    val categories: List<CategoryRow>,
    val transactions: List<TransactionRow>,
    val transfers: List<TransferRow>,
    val fixedExpenses: List<FixedExpenseRow> = emptyList(),
    val fixedExpensePeriodStates: List<FixedExpensePeriodStateRow> = emptyList()
)
