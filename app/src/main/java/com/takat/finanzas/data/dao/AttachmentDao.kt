package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.takat.finanzas.data.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments ORDER BY createdAt ASC")
    fun getAll(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId ORDER BY createdAt ASC")
    fun getForTransaction(transactionId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId")
    suspend fun getForTransactionOnce(transactionId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE contentHash = :contentHash LIMIT 1")
    suspend fun findByHash(contentHash: String): AttachmentEntity?

    @Query("SELECT COUNT(*) FROM attachments WHERE filePath = :filePath AND id != :excludeId")
    suspend fun countByFilePath(filePath: String, excludeId: Long): Int

    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: AttachmentEntity)
}
