package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttachmentType { JSON, IMAGE, PDF }

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId"), Index("contentHash")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val type: AttachmentType,
    /** Path to the stored file (encrypted at rest). Never the raw bytes. */
    val filePath: String,
    /** Path to a small unencrypted preview, only set for IMAGE attachments. */
    val thumbnailPath: String?,
    /** SHA-256 of the original file content, used to dedupe repeated imports. */
    val contentHash: String,
    val createdAt: Long
)
