package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.takat.finanzas.data.entity.BudgetSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetSettingsDao {
    @Query("SELECT * FROM budget_settings WHERE id = 0")
    fun get(): Flow<BudgetSettingsEntity?>

    @Upsert
    suspend fun upsert(entity: BudgetSettingsEntity)
}
