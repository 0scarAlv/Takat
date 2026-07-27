package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("accountId"), Index("categoryId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long?,
    /** Signed amount in cents: positive for income, negative for expense. */
    val amountCents: Long,
    val note: String?,
    val date: Long,
    /**
     * Set when this transaction is a payment toward a fixed expense's period. Deliberately not a
     * DB foreign key: "paid so far" for a period is derived by summing matching transactions, so
     * deleting one here automatically un-counts it — no separate "paid" flag to fall out of sync.
     */
    val fixedExpenseId: Long? = null,
    val fixedExpensePeriodKey: String? = null
)
