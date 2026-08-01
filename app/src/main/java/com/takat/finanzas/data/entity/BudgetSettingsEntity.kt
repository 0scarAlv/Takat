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
    val basis: BudgetBasis = BudgetBasis.DISPONIBLE,
    /** Most recently observed live daily-budget value, used to seed [frozenBudgetCents] on the next day rollover. */
    val lastLiveValueCents: Long = 0,
    /** LocalDate.toEpochDay() [lastLiveValueCents] was observed on. 0 = sentinel "never observed". */
    val lastLiveValueEpochDay: Long = 0,
    /** Static "presupuesto diario" for [frozenBudgetEpochDay], frozen from the prior day's last live value. */
    val frozenBudgetCents: Long = 0,
    /** LocalDate.toEpochDay() [frozenBudgetCents] applies to. 0 = sentinel "never frozen". */
    val frozenBudgetEpochDay: Long = 0
)
