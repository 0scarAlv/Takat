package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.entity.TransferEntity

/** A single row in a history list: either a plain transaction or a transfer between two accounts. */
sealed class Movement {
    abstract val date: Long

    data class TransactionMovement(
        val transaction: TransactionEntity,
        val category: CategoryEntity?,
        val account: AccountEntity?
    ) : Movement() {
        override val date get() = transaction.date
    }

    data class TransferMovement(
        val transfer: TransferEntity,
        val fromAccount: AccountEntity?,
        val toAccount: AccountEntity?,
        val category: CategoryEntity?
    ) : Movement() {
        override val date get() = transfer.date
    }
}

val Movement.key: String
    get() = when (this) {
        is Movement.TransactionMovement -> "t${transaction.id}"
        is Movement.TransferMovement -> "x${transfer.id}"
    }
