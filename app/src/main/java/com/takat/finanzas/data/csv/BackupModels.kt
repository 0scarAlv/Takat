package com.takat.finanzas.data.csv

import com.takat.finanzas.data.entity.CategoryKind

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
    val note: String?
)

data class TransferRow(
    val date: Long,
    val fromAccountName: String,
    val toAccountName: String,
    val categoryName: String?,
    val amountCents: Long,
    val note: String?
)

data class ParsedBackup(
    val accounts: List<AccountRow>,
    val categories: List<CategoryRow>,
    val transactions: List<TransactionRow>,
    val transfers: List<TransferRow>
)
