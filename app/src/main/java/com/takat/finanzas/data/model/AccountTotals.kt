package com.takat.finanzas.data.model

data class AccountTotals(
    val availableCents: Long,
    val capitalCents: Long,
    val debtCents: Long,
    val pendingFixedExpensesCents: Long = 0,
    /** Principal still owed on "compra a crédito" plans; not part of [debtCents]. */
    val financedCents: Long = 0,
    /** Whether [financedCents] was subtracted from [availableCents] (user setting). */
    val financedIncludedInAvailable: Boolean = false
)
