package com.takat.finanzas.network.crypto

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * P-256 ECDH + HKDF-SHA256 + AES-256-GCM building blocks for the phone<->browser session crypto
 * behind the PC-access panel. There is no TLS/certificate involved on purpose (self-signed certs
 * mean scary browser warnings on a LAN-only server) — this is what "encrypted end to end" means
 * for that feature instead: see the Takat 2.0 plan for the full protocol
 * (POST /pair/init -> QR carries pairingId -> phone approves -> per-request AEAD envelope).
 *
 * Uses the same raw uncompressed-point format (0x04 || X || Y, 65 bytes) that the browser's
 * WebCrypto SubtleCrypto ECDH import/export("raw", ...) produces, so both sides speak the same
 * wire format with no extra encoding step.
 */
object SessionCrypto {
    private const val CURVE = "secp256r1"
    private const val COORD_SIZE = 32

    private val ecParams: ECParameterSpec by lazy {
        val params = AlgorithmParameters.getInstance("EC")
        params.init(ECGenParameterSpec(CURVE))
        params.getParameterSpec(ECParameterSpec::class.java)
    }

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(CURVE))
        return kpg.generateKeyPair()
    }

    fun encodeRawPublicKey(publicKey: PublicKey): ByteArray {
        val ecKey = publicKey as ECPublicKey
        return byteArrayOf(0x04) +
            ecKey.w.affineX.toFixedBytes(COORD_SIZE) +
            ecKey.w.affineY.toFixedBytes(COORD_SIZE)
    }

    fun decodeRawPublicKey(bytes: ByteArray): PublicKey {
        require(bytes.size == 1 + 2 * COORD_SIZE && bytes[0] == 0x04.toByte()) {
            "Expected an uncompressed P-256 point (65 bytes starting with 0x04)"
        }
        val x = BigInteger(1, bytes.copyOfRange(1, 1 + COORD_SIZE))
        val y = BigInteger(1, bytes.copyOfRange(1 + COORD_SIZE, bytes.size))
        val spec = ECPublicKeySpec(ECPoint(x, y), ecParams)
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }

    /**
     * Fixed 32-byte X-coordinate, regardless of whether the JCE provider strips leading zero
     * bytes from the raw secret (some historically have). The browser side (noble-curves) always
     * emits a fixed-length point encoding, so both sides must agree on a fixed-length secret too.
     */
    fun sharedSecret(privateKey: PrivateKey, otherPublicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(otherPublicKey, true)
        return BigInteger(1, agreement.generateSecret()).toFixedBytes(COORD_SIZE)
    }

    /** HKDF-SHA256 (RFC 5869). */
    fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int = 32): ByteArray {
        val extractMac = Mac.getInstance("HmacSHA256")
        extractMac.init(SecretKeySpec(if (salt.isEmpty()) ByteArray(32) else salt, "HmacSHA256"))
        val prk = extractMac.doFinal(ikm)

        val expandMac = Mac.getInstance("HmacSHA256")
        expandMac.init(SecretKeySpec(prk, "HmacSHA256"))
        val output = ByteArray(length)
        var previousBlock = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            expandMac.reset()
            expandMac.update(previousBlock)
            expandMac.update(info)
            expandMac.update(counter.toByte())
            previousBlock = expandMac.doFinal()
            val toCopy = minOf(previousBlock.size, length - offset)
            System.arraycopy(previousBlock, 0, output, offset, toCopy)
            offset += toCopy
            counter++
        }
        return output
    }

    fun encrypt(key: ByteArray, plaintext: ByteArray): EncryptedPayload {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return EncryptedPayload(iv, cipher.doFinal(plaintext))
    }

    fun decrypt(key: ByteArray, payload: EncryptedPayload): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, payload.iv))
        return cipher.doFinal(payload.ciphertext)
    }

    private fun BigInteger.toFixedBytes(size: Int): ByteArray {
        val raw = toByteArray()
        return when {
            raw.size == size -> raw
            raw.size == size + 1 && raw[0] == 0.toByte() -> raw.copyOfRange(1, raw.size)
            raw.size < size -> ByteArray(size - raw.size) + raw
            else -> error("BigInteger too large for a $size-byte fixed-size encoding")
        }
    }

    data class EncryptedPayload(val iv: ByteArray, val ciphertext: ByteArray)
}
