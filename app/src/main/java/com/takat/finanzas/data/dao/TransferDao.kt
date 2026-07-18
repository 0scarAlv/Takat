package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.takat.finanzas.data.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun getAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE fromAccountId = :accountId OR toAccountId = :accountId ORDER BY date DESC")
    fun getForAccount(accountId: Long): Flow<List<TransferEntity>>

    @Insert
    suspend fun insert(transfer: TransferEntity): Long

    @Delete
    suspend fun delete(transfer: TransferEntity)
}
