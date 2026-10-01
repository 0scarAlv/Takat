package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.AccountEntity

data class AccountWithBalance(
    val account: AccountEntity,
    /** Movements-only balance: excludes [financedCents], so a card shows what's actually been charged to it. */
    val balanceCents: Long,
    /** Principal still owed on this account from "compra a crédito" plans, kept apart from [balanceCents]. */
    val financedCents: Long = 0
)
