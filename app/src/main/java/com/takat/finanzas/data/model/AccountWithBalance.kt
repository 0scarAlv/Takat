package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.AccountEntity

data class AccountWithBalance(
    val account: AccountEntity,
    /** Movements-only balance: excludes [financedCents], so a card shows what's actually been charged to it. */
    val balanceCents: Long,
    /** Principal still owed on this account from "compra a crédito" plans, kept apart from [balanceCents]. */
    val financedCents: Long = 0
) {
    /** What's been charged against the card: purchases (its negative balance) plus "compra a crédito" principal. */
    val creditUsedCents: Long get() = (-balanceCents).coerceAtLeast(0) + financedCents

    /** Room left under [AccountEntity.creditLimitCents], negative once exceeded; null when no limit is set. */
    val creditAvailableCents: Long? get() = account.creditLimitCents?.let { it - creditUsedCents }
}
