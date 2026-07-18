package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialBalanceCents: Long,
    val colorArgb: Int,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDebt: Boolean = false,
    val includeInTotal: Boolean = true
)
