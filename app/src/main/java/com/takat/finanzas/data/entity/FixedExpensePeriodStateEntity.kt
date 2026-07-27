package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-period exception to a [FixedExpenseEntity]'s default state. A period with no row here is
 * implicitly active and unpaid — rows only exist once the user deviates from that default
 * (deactivates the period, or gets notified about it). How much has been paid is derived from
 * [TransactionEntity] rows tagged with this period, not stored here.
 *
 * The three `*NotifiedAt` columns track independently whether each MENSUAL reminder stage (day
 * before, due day, 2-days-after follow-up) has already fired for this period; QUINCENAL only ever
 * uses [dueNotifiedAt].
 */
@Entity(
    tableName = "fixed_expense_period_state",
    foreignKeys = [
        ForeignKey(
            entity = FixedExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["fixedExpenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("fixedExpenseId"),
        Index(value = ["fixedExpenseId", "periodKey"], unique = true)
    ]
)
data class FixedExpensePeriodStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fixedExpenseId: Long,
    val periodKey: String,
    val active: Boolean = true,
    val preNotifiedAt: Long? = null,
    val dueNotifiedAt: Long? = null,
    val followUpNotifiedAt: Long? = null
)
