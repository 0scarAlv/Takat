package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.takat.finanzas.data.entity.FixedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedExpenseDao {
    @Query("SELECT * FROM fixed_expenses ORDER BY createdAt ASC")
    fun getAll(): Flow<List<FixedExpenseEntity>>

    @Insert
    suspend fun insert(entity: FixedExpenseEntity): Long

    @Update
    suspend fun update(entity: FixedExpenseEntity)

    @Delete
    suspend fun delete(entity: FixedExpenseEntity)
}
