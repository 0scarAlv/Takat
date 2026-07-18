package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.AccountEntity

data class AccountWithBalance(
    val account: AccountEntity,
    val balanceCents: Long
)
