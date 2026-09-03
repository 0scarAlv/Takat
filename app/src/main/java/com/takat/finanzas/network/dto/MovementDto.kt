package com.takat.finanzas.network.dto

import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.model.key
import kotlinx.serialization.Serializable

@Serializable
data class NewTransactionRequest(
    val accountId: Long,
    val categoryId: Long?,
    val amountCents: Long,
    val note: String?,
    val date: Long
)

@Serializable
data class NewTransferRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountCents: Long,
    val note: String?,
    val date: Long
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val emoji: String,
    val kind: CategoryKind
)

fun CategoryEntity.toDto() = CategoryDto(id = id, name = name, emoji = emoji, kind = kind)

/**
 * Flattened view of [Movement]: a transaction or a transfer, discriminated by [type]. Kept as one
 * flat shape (instead of mirroring the sealed class) since the web dashboard only needs to list
 * and delete movements, not the richer editing the Android app supports.
 */
@Serializable
data class MovementDto(
    val key: String,
    val type: String, // "transaction" | "transfer"
    /** The underlying row's own id (transaction.id or transfer.id) — what DELETE /api/transactions/{id} expects. */
    val id: Long,
    val date: Long,
    val amountCents: Long,
    val note: String?,
    val accountId: Long? = null,
    val accountName: String? = null,
    val fromAccountId: Long? = null,
    val fromAccountName: String? = null,
    val toAccountId: Long? = null,
    val toAccountName: String? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val categoryEmoji: String? = null
)

fun Movement.toDto(): MovementDto = when (this) {
    is Movement.TransactionMovement -> MovementDto(
        key = key,
        type = "transaction",
        id = transaction.id,
        date = transaction.date,
        amountCents = transaction.amountCents,
        note = transaction.note,
        accountId = transaction.accountId,
        accountName = account?.name,
        categoryId = transaction.categoryId,
        categoryName = category?.name,
        categoryEmoji = category?.emoji
    )
    is Movement.TransferMovement -> MovementDto(
        key = key,
        type = "transfer",
        id = transfer.id,
        date = transfer.date,
        amountCents = transfer.amountCents,
        note = transfer.note,
        fromAccountId = transfer.fromAccountId,
        fromAccountName = fromAccount?.name,
        toAccountId = transfer.toAccountId,
        toAccountName = toAccount?.name,
        categoryId = transfer.categoryId,
        categoryName = category?.name,
        categoryEmoji = category?.emoji
    )
}
