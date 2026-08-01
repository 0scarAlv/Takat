package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.takat.finanzas.data.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun get(): Flow<AppSettingsEntity?>

    @Upsert
    suspend fun upsert(entity: AppSettingsEntity)
}
