package com.takat.finanzas.data.attachment

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.FileProvider
import com.takat.finanzas.data.dao.AttachmentDao
import com.takat.finanzas.data.entity.AttachmentEntity
import com.takat.finanzas.data.entity.AttachmentType
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_ALIAS = "takat_attachments_key"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH_BITS = 128
private const val MAX_IMAGE_WIDTH = 1200
private const val THUMBNAIL_WIDTH = 120
private const val JPEG_QUALITY = 75

/**
 * Owns everything file-related for transaction attachments: hashing for dedup,
 * image resize/compression, AES-GCM encryption at rest, and the on-disk layout.
 * The database only ever stores the paths this class hands back.
 */
class AttachmentStorage(private val context: Context) {

    private val attachmentsRoot: File
        get() = File(context.filesDir, "adjuntos").apply { mkdirs() }

    // region Public API

    /** Resizes/compresses [sourceBytes] as a JPEG, encrypts it, and stores an unencrypted thumbnail alongside it. */
    suspend fun saveImage(dao: AttachmentDao, transactionId: Long, sourceBytes: ByteArray): AttachmentEntity {
        val original = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size)
            ?: error("No se pudo leer la imagen")
        val resized = resize(original, MAX_IMAGE_WIDTH)
        val processedBytes = toJpegBytes(resized, JPEG_QUALITY)
        val hash = sha256(processedBytes)

        dao.findByHash(hash)?.let { existing ->
            if (resized !== original) original.recycle()
            resized.recycle()
            return persist(dao, transactionId, AttachmentType.IMAGE, existing.filePath, existing.thumbnailPath, hash)
        }

        val folder = folderFor(transactionId)
        val fileName = "${UUID.randomUUID()}.enc"
        val file = File(folder, fileName)
        file.writeBytes(encrypt(processedBytes))

        val thumbnail = resize(resized, THUMBNAIL_WIDTH)
        val thumbFile = File(folder, "thumb_$fileName.jpg")
        thumbFile.writeBytes(toJpegBytes(thumbnail, JPEG_QUALITY))

        if (resized !== original) original.recycle()
        if (thumbnail !== resized) thumbnail.recycle()
        resized.recycle()

        return persist(dao, transactionId, AttachmentType.IMAGE, file.absolutePath, thumbFile.absolutePath, hash)
    }

    /** Stores a JSON or PDF file as-is (no compression), encrypted at rest. */
    suspend fun saveDocument(dao: AttachmentDao, transactionId: Long, type: AttachmentType, sourceBytes: ByteArray): AttachmentEntity {
        val hash = sha256(sourceBytes)

        dao.findByHash(hash)?.let { existing ->
            return persist(dao, transactionId, type, existing.filePath, existing.thumbnailPath, hash)
        }

        val folder = folderFor(transactionId)
        val extension = if (type == AttachmentType.PDF) "pdf" else "json"
        val file = File(folder, "${UUID.randomUUID()}.$extension.enc")
        file.writeBytes(encrypt(sourceBytes))

        return persist(dao, transactionId, type, file.absolutePath, null, hash)
    }

    suspend fun readDecrypted(attachment: AttachmentEntity): ByteArray =
        decrypt(File(attachment.filePath).readBytes())

    /** Deletes the files backing [attachment], unless another attachment row still points at them (dedup). */
    suspend fun deleteFiles(dao: AttachmentDao, attachment: AttachmentEntity) {
        val stillReferenced = dao.countByFilePath(attachment.filePath, attachment.id) > 0
        if (stillReferenced) return
        File(attachment.filePath).delete()
        attachment.thumbnailPath?.let { File(it).delete() }
    }

    fun readFromUri(resolver: ContentResolver, uri: Uri): ByteArray =
        resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo leer el archivo")

    /** Creates a temp file in cache for the camera to write a full-res JPEG into, and returns its content:// Uri. */
    fun createCaptureFile(): Pair<File, Uri> {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        return file to FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Writes decrypted bytes to a scratch cache file for handing to an external viewer (e.g. a PDF app). */
    fun writeTempForView(bytes: ByteArray, suffix: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.$suffix")
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // endregion

    private suspend fun persist(
        dao: AttachmentDao,
        transactionId: Long,
        type: AttachmentType,
        filePath: String,
        thumbnailPath: String?,
        hash: String
    ): AttachmentEntity {
        val entity = AttachmentEntity(
            transactionId = transactionId,
            type = type,
            filePath = filePath,
            thumbnailPath = thumbnailPath,
            contentHash = hash,
            createdAt = System.currentTimeMillis()
        )
        return entity.copy(id = dao.insert(entity))
    }

    private fun folderFor(transactionId: Long): File =
        File(attachmentsRoot, transactionId.toString()).apply { mkdirs() }

    private fun resize(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    private fun toJpegBytes(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // region Crypto

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return cipher.iv + cipher.doFinal(plain)
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    // endregion
}
