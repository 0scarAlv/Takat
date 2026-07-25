package com.takat.finanzas.data.model

data class AccountTotals(
    val availableCents: Long,
    val capitalCents: Long,
    val debtCents: Long,
    val pendingFixedExpensesCents: Long = 0
)
