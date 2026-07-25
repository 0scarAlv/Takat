package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.takat.finanzas.data.entity.FixedExpensePeriodStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedExpensePeriodStateDao {
    @Query("SELECT * FROM fixed_expense_period_state")
    fun getAll(): Flow<List<FixedExpensePeriodStateEntity>>

    @Query("SELECT * FROM fixed_expense_period_state WHERE fixedExpenseId = :fixedExpenseId AND periodKey = :periodKey LIMIT 1")
    suspend fun find(fixedExpenseId: Long, periodKey: String): FixedExpensePeriodStateEntity?

    @Insert
    suspend fun insert(entity: FixedExpensePeriodStateEntity): Long

    @Update
    suspend fun update(entity: FixedExpensePeriodStateEntity)
}
