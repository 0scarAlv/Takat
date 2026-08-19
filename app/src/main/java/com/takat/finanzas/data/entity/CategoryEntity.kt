package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryKind { INCOME, EXPENSE, BOTH }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val kind: CategoryKind,
    val isDefault: Boolean = false,
    /** Income tagged with this category marks the start of a new quincena as soon as it's logged (see FixedExpensePeriod). */
    val isSalary: Boolean = false
)
