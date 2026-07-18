package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.CategoryEntity

data class CategoryExpense(
    val category: CategoryEntity?,
    val totalCents: Long
)
