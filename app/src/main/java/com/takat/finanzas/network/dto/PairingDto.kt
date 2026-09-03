package com.takat.finanzas.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairInitRequest(val publicKey: String)

@Serializable
data class PairInitResponse(val pairingId: String, val publicKey: String)

/** Generic AES-GCM envelope: [iv] and [ciphertext] are both Base64. Used for /pair/status and every /api body. */
@Serializable
data class EncryptedEnvelope(val iv: String, val ciphertext: String)

@Serializable
data class PairStatusPayload(val status: String, val deviceToken: String? = null)
