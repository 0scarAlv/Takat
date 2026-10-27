package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.takat.finanzas.data.entity.TutorialProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TutorialProgressDao {
    @Query("SELECT * FROM tutorial_progress")
    fun getAll(): Flow<List<TutorialProgressEntity>>

    @Upsert
    suspend fun upsert(entity: TutorialProgressEntity)

    /** Used for skips, so skipping a guide again later never erases an earlier completion. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(entity: TutorialProgressEntity)
}
