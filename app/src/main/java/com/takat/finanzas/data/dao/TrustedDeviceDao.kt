package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.takat.finanzas.data.entity.TrustedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedDeviceDao {
    @Query("SELECT * FROM trusted_devices ORDER BY lastUsedAt DESC")
    fun getAll(): Flow<List<TrustedDeviceEntity>>

    @Query("SELECT * FROM trusted_devices WHERE deviceToken = :deviceToken")
    suspend fun getByToken(deviceToken: String): TrustedDeviceEntity?

    @Insert
    suspend fun insert(entity: TrustedDeviceEntity)

    @Update
    suspend fun update(entity: TrustedDeviceEntity)

    @Delete
    suspend fun delete(entity: TrustedDeviceEntity)
}
