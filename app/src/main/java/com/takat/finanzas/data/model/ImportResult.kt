package com.takat.finanzas.data.model

data class ImportResult(
    val accountsAdded: Int,
    val categoriesAdded: Int,
    val transactionsAdded: Int,
    val transfersAdded: Int,
    val skipped: Int,
    val attachmentsAdded: Int = 0,
    val fixedExpensesAdded: Int = 0,
    /** Movements left out because an identical one was already in the app (ImportMode.ONLY_MISSING only). */
    val duplicatesSkipped: Int = 0
)
