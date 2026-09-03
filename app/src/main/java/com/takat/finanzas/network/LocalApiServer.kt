package com.takat.finanzas.network

import android.content.Context
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.network.crypto.SessionCrypto
import com.takat.finanzas.network.dto.EncryptedEnvelope
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
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
