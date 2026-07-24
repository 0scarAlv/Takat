package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BudgetPeriodType { QUINCENA, MES }
enum class BudgetBasis { DISPONIBLE, CAPITAL }

@Entity(tableName = "budget_settings")
data class BudgetSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val enabled: Boolean = false,
    val periodType: BudgetPeriodType = BudgetPeriodType.QUINCENA,
    val dayOfMonth: Int = 1,
    val basis: BudgetBasis = BudgetBasis.DISPONIBLE
)
