package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-period exception to a [FixedExpenseEntity]'s default state. A period with no row here is
 * implicitly active and unpaid — rows only exist once the user deviates from that default
 * (deactivates the period, pays it, or gets notified about it).
 */
@Entity(
    tableName = "fixed_expense_period_state",
    foreignKeys = [
        ForeignKey(
            entity = FixedExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["fixedExpenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("fixedExpenseId"),
        Index("paidTransactionId"),
        Index(value = ["fixedExpenseId", "periodKey"], unique = true)
    ]
)
data class FixedExpensePeriodStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fixedExpenseId: Long,
    val periodKey: String,
    val active: Boolean = true,
    val paidTransactionId: Long? = null,
    val notifiedAt: Long? = null
)
