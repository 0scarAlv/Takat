package com.takat.finanzas.network

import com.takat.finanzas.data.dao.TrustedDeviceDao
import com.takat.finanzas.data.entity.TrustedDeviceEntity
import com.takat.finanzas.network.crypto.SessionCrypto
import java.security.KeyPair
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow

private const val PAIRING_INFO = "takat-pairing-v1"
private val PAIRING_TTL_MILLIS = 2 * 60 * 1000L

/**
 * Runs the QR-pairing handshake described in the Takat 2.0 plan. Lives as long as the process
 * (instantiated once in TakatApplication, same lifetime as [com.takat.finanzas.data.repository.FinanceRepository])
 * and is shared in-process between the Ktor routes (browser side) and PairScanScreen (phone side)
 * — the phone approving a pairing is a direct Kotlin call, not a loopback HTTP request.
 */
class PairingManager(private val trustedDeviceDao: TrustedDeviceDao) {

    private data class PendingPairing(
        val serverKeyPair: KeyPair,
        val sessionKey: ByteArray,
        val createdAt: Long,
        var approvedDeviceToken: String? = null
    )

    private val pending = ConcurrentHashMap<String, PendingPairing>()

    /** Browser posts its ECDH public key; returns (pairingId, server's ECDH public key). */
    fun initPairing(browserPublicKeyRaw: ByteArray): Pair<String, ByteArray> {
        pruneExpired()
        val pairingId = UUID.randomUUID().toString()
        val serverKeyPair = SessionCrypto.generateKeyPair()
        val browserPublicKey = SessionCrypto.decodeRawPublicKey(browserPublicKeyRaw)
        val sharedSecret = SessionCrypto.sharedSecret(serverKeyPair.private, browserPublicKey)
        val sessionKey = SessionCrypto.hkdf(
            ikm = sharedSecret,
            salt = pairingId.toByteArray(Charsets.UTF_8),
            info = PAIRING_INFO.toByteArray(Charsets.UTF_8)
        )
        pending[pairingId] = PendingPairing(serverKeyPair, sessionKey, System.currentTimeMillis())
        return pairingId to SessionCrypto.encodeRawPublicKey(serverKeyPair.public)
    }

    fun qrContent(pairingId: String): String = "takat:pair:$pairingId"

    /** Encrypts [json] with the pairing's session key, for /pair/status responses. Null if unknown/expired. */
    fun encryptForPairing(pairingId: String, json: ByteArray): SessionCrypto.EncryptedPayload? {
        val session = pending[pairingId] ?: return null
        return SessionCrypto.encrypt(session.sessionKey, json)
    }

    fun isApproved(pairingId: String): String? = pending[pairingId]?.approvedDeviceToken

    /**
     * Called from the phone after scanning the QR (PairScanScreen). Persists a new trusted device
     * using the pairing's already-established session key as its long-lived secret — the browser
     * derived that same key independently via ECDH, it was never sent over the network.
     */
    suspend fun approve(pairingId: String, deviceName: String): TrustedDeviceEntity? {
        val session = pending[pairingId] ?: return null
        if (System.currentTimeMillis() - session.createdAt > PAIRING_TTL_MILLIS) {
            pending.remove(pairingId)
            return null
        }
        val deviceToken = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ByteArray(32).also { SecureRandom().nextBytes(it) }
        )
        val entity = TrustedDeviceEntity(
            deviceToken = deviceToken,
            name = deviceName,
            secretBase64 = Base64.getEncoder().encodeToString(session.sessionKey),
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis()
        )
        trustedDeviceDao.insert(entity)
        session.approvedDeviceToken = deviceToken
        return entity
    }

    /** Looks up a trusted device's secret for the per-request AEAD envelope on the /api routes. */
    suspend fun secretForDevice(deviceToken: String): ByteArray? {
        val device = trustedDeviceDao.getByToken(deviceToken) ?: return null
        trustedDeviceDao.update(device.copy(lastUsedAt = System.currentTimeMillis()))
        return Base64.getDecoder().decode(device.secretBase64)
    }

    /** For Settings' "Dispositivos vinculados" list. */
    fun devices(): Flow<List<TrustedDeviceEntity>> = trustedDeviceDao.getAll()

    suspend fun revoke(deviceToken: String) {
        trustedDeviceDao.getByToken(deviceToken)?.let { trustedDeviceDao.delete(it) }
    }

    private fun pruneExpired() {
        val cutoff = System.currentTimeMillis() - PAIRING_TTL_MILLIS
        pending.entries.removeIf { it.value.createdAt < cutoff && it.value.approvedDeviceToken == null }
    }
}
