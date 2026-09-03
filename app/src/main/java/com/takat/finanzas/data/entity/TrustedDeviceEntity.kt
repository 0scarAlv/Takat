package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A browser that completed QR pairing for the PC-access panel. [secretBase64] is the ECDH-derived
 * session secret (see network/crypto/SessionCrypto.kt) used to key every request/response — it
 * never travels over the network itself, only the [deviceToken] does (as a lookup id).
 */
@Entity(tableName = "trusted_devices")
data class TrustedDeviceEntity(
    @PrimaryKey val deviceToken: String,
    val name: String,
    val secretBase64: String,
    val createdAt: Long,
    val lastUsedAt: Long
)
