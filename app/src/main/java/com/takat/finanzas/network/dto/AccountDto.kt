package com.takat.finanzas.network.dto

import com.takat.finanzas.data.model.AccountWithBalance
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: Long,
    val name: String,
    val balanceCents: Long,
    val colorArgb: Int,
    val isDebt: Boolean,
    val includeInTotal: Boolean
)

fun AccountWithBalance.toDto() = AccountDto(
    id = account.id,
    name = account.name,
    balanceCents = balanceCents,
    colorArgb = account.colorArgb,
    isDebt = account.isDebt,
    includeInTotal = account.includeInTotal
)
