package com.takat.finanzas.network

import android.content.Context
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.entity.TransferEntity
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.network.crypto.SessionCrypto
import com.takat.finanzas.network.dto.EncryptedEnvelope
import com.takat.finanzas.network.dto.IdResponse
import com.takat.finanzas.network.dto.NewTransactionRequest
import com.takat.finanzas.network.dto.NewTransferRequest
import com.takat.finanzas.network.dto.OkResponse
import com.takat.finanzas.network.dto.PairInitRequest
import com.takat.finanzas.network.dto.PairInitResponse
import com.takat.finanzas.network.dto.PairStatusPayload
import com.takat.finanzas.network.dto.toDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Embedded HTTP server exposing Takat's data to a paired PC on the same LAN. See the Takat 2.0
 * plan for the full picture; in short: no TLS, no central server — every /api body is
 * AES-GCM-encrypted with the per-device secret established during QR pairing
 * (network/PairingManager.kt + network/crypto/SessionCrypto.kt).
 */
class LocalApiServer(
    private val context: Context,
    private val repository: FinanceRepository,
    private val pairingManager: PairingManager
) {
    private var server: EmbeddedServer<*, *>? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun start(port: Int = DEFAULT_PORT) {
        if (server != null) return
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) { json(this@LocalApiServer.json) }
            routing {
                get("/") { serveAsset(call, "index.html") }
                get("/{path...}") {
                    val path = call.parameters.getAll("path")?.joinToString("/").orEmpty()
                    serveAsset(call, path.ifBlank { "index.html" })
                }

                post("/pair/init") {
                    val body = call.receive<PairInitRequest>()
                    val (pairingId, serverPublicKey) = pairingManager.initPairing(
                        Base64.getDecoder().decode(body.publicKey)
                    )
                    call.respond(
                        PairInitResponse(
                            pairingId = pairingId,
                            publicKey = Base64.getEncoder().encodeToString(serverPublicKey)
                        )
                    )
                }

                get("/pair/qr.png") {
                    val pairingId = call.request.queryParameters["pairingId"]
                    if (pairingId == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val png = QrCode.renderPng(pairingManager.qrContent(pairingId))
                    call.respondBytes(png, ContentType.Image.PNG)
                }

                get("/pair/status") {
                    val pairingId = call.request.queryParameters["pairingId"]
                    if (pairingId == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val deviceToken = pairingManager.isApproved(pairingId)
                    val payload = if (deviceToken != null) {
                        PairStatusPayload(status = "approved", deviceToken = deviceToken)
                    } else {
                        PairStatusPayload(status = "pending")
                    }
                    val encrypted = pairingManager.encryptForPairing(
                        pairingId,
                        json.encodeToString(PairStatusPayload.serializer(), payload).toByteArray()
                    )
                    if (encrypted == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    call.respond(encrypted.toEnvelope())
                }

                get("/api/accounts") {
                    val secret = call.deviceSecret() ?: return@get
                    val accounts = repository.accountsWithBalance().first().map { it.toDto() }
                    call.respondEncrypted(secret, json.encodeToString(accounts))
                }

                get("/api/categories") {
                    val secret = call.deviceSecret() ?: return@get
                    val categories = repository.categories.first().map { it.toDto() }
                    call.respondEncrypted(secret, json.encodeToString(categories))
                }

                get("/api/movements") {
                    val secret = call.deviceSecret() ?: return@get
                    val movements = repository.allMovements().first().map { it.toDto() }
                    call.respondEncrypted(secret, json.encodeToString(movements))
                }

                get("/api/totals") {
                    val secret = call.deviceSecret() ?: return@get
                    call.respondEncrypted(secret, json.encodeToString(repository.accountTotals().first().toDto()))
                }

                get("/api/export.csv") {
                    val secret = call.deviceSecret() ?: return@get
                    call.respondEncrypted(secret, repository.exportCsv())
                }

                post("/api/transactions") {
                    val secret = call.deviceSecret() ?: return@post
                    val bytes = call.decryptBody(secret) ?: return@post
                    val req = json.decodeFromString<NewTransactionRequest>(bytes.decodeToString())
                    val id = repository.addTransaction(
                        TransactionEntity(
                            accountId = req.accountId,
                            categoryId = req.categoryId,
                            amountCents = req.amountCents,
                            note = req.note,
                            date = req.date
                        )
                    )
                    call.respondEncrypted(secret, json.encodeToString(IdResponse(id)))
                }

                delete("/api/transactions/{id}") {
                    val secret = call.deviceSecret() ?: return@delete
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    val movement = repository.allMovements().first()
                        .filterIsInstance<Movement.TransactionMovement>()
                        .find { it.transaction.id == id }
                    if (movement == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@delete
                    }
                    repository.deleteTransaction(movement.transaction)
                    call.respondEncrypted(secret, json.encodeToString(OkResponse()))
                }

                post("/api/transfers") {
                    val secret = call.deviceSecret() ?: return@post
                    val bytes = call.decryptBody(secret) ?: return@post
                    val req = json.decodeFromString<NewTransferRequest>(bytes.decodeToString())
                    val id = repository.addTransfer(
                        TransferEntity(
                            fromAccountId = req.fromAccountId,
                            toAccountId = req.toAccountId,
                            categoryId = null,
                            amountCents = req.amountCents,
                            note = req.note,
                            date = req.date
                        )
                    )
                    call.respondEncrypted(secret, json.encodeToString(IdResponse(id)))
                }

                delete("/api/transfers/{id}") {
                    val secret = call.deviceSecret() ?: return@delete
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    val movement = repository.allMovements().first()
                        .filterIsInstance<Movement.TransferMovement>()
                        .find { it.transfer.id == id }
                    if (movement == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@delete
                    }
                    repository.deleteTransfer(movement.transfer)
                    call.respondEncrypted(secret, json.encodeToString(OkResponse()))
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        server = null
    }

    private suspend fun ApplicationCall.deviceSecret(): ByteArray? {
        val deviceToken = request.headers[DEVICE_HEADER]
        val secret = deviceToken?.let { pairingManager.secretForDevice(it) }
        if (secret == null) respond(HttpStatusCode.Unauthorized)
        return secret
    }

    private suspend fun ApplicationCall.respondEncrypted(secret: ByteArray, plaintextJson: String) {
        respond(SessionCrypto.encrypt(secret, plaintextJson.toByteArray()).toEnvelope())
    }

    private suspend fun ApplicationCall.decryptBody(secret: ByteArray): ByteArray? {
        val envelope = try {
            receive<EncryptedEnvelope>()
        } catch (e: Exception) {
            respond(HttpStatusCode.BadRequest)
            return null
        }
        return try {
            SessionCrypto.decrypt(
                secret,
                SessionCrypto.EncryptedPayload(
                    iv = Base64.getDecoder().decode(envelope.iv),
                    ciphertext = Base64.getDecoder().decode(envelope.ciphertext)
                )
            )
        } catch (e: Exception) {
            respond(HttpStatusCode.BadRequest)
            null
        }
    }

    private suspend fun serveAsset(call: ApplicationCall, requestedPath: String) {
        val assetPath = "web/$requestedPath"
        val bytes = runCatching {
            context.assets.open(assetPath).use { it.readBytes() }
        }.getOrNull() ?: runCatching {
            context.assets.open("web/index.html").use { it.readBytes() }
        }.getOrNull()

        if (bytes == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respondBytes(bytes, contentTypeFor(requestedPath))
        }
    }

    private fun contentTypeFor(path: String): ContentType = when {
        path.endsWith(".html") -> ContentType.Text.Html
        path.endsWith(".js") -> ContentType.Application.JavaScript
        path.endsWith(".css") -> ContentType.Text.CSS
        path.endsWith(".png") -> ContentType.Image.PNG
        path.endsWith(".svg") -> ContentType.Image.SVG
        else -> ContentType.Application.OctetStream
    }

    private fun SessionCrypto.EncryptedPayload.toEnvelope() = EncryptedEnvelope(
        iv = Base64.getEncoder().encodeToString(iv),
        ciphertext = Base64.getEncoder().encodeToString(ciphertext)
    )

    companion object {
        const val DEFAULT_PORT = 8765
        const val DEVICE_HEADER = "X-Takat-Device"
    }
}
