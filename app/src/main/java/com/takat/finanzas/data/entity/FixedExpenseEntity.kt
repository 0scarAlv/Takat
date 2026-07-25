package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FixedExpenseFrequency { MENSUAL, QUINCENAL }

@Entity(
    tableName = "fixed_expenses",
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
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val frequency: FixedExpenseFrequency,
    /** Day of month (1-31, clamped to the last day of shorter months) used only when frequency is MENSUAL. */
    val dayOfMonth: Int,
    val notifyEnabled: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
