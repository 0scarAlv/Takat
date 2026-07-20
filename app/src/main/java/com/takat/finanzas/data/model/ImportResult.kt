package com.takat.finanzas.data.model

data class ImportResult(
    val accountsAdded: Int,
    val categoriesAdded: Int,
    val transactionsAdded: Int,
    val transfersAdded: Int,
    val skipped: Int,
    val attachmentsAdded: Int = 0
)
