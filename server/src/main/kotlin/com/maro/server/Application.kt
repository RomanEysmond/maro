package com.maro.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class NotifyRequest(val chatId: String, val messageId: String)

@Serializable
data class NotifyResponse(val devices: Int)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT
    val firebase = FirebaseBackend.create()
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        maroModule(NotifyService(firebase.chatStore, firebase.pushSender), firebase.tokenVerifier)
    }.start(wait = true)
}

private const val DEFAULT_PORT = 8080
private val log = LoggerFactory.getLogger("com.maro.server")

fun Application.maroModule(notifyService: NotifyService, tokenVerifier: TokenVerifier) {
    install(ContentNegotiation) { json() }

    routing {
        get("/health") { call.respondText("ok") }

        // Called by the sender's app right after its message reached Firestore (there are no Cloud Functions on Spark
        // to react to the write itself). Authorization: Bearer <Firebase ID token of the sender>.
        post("/v1/notify") {
            val idToken = call.request.header(HttpHeaders.Authorization)
                ?.takeIf { it.startsWith(BEARER) }
                ?.removePrefix(BEARER)
                ?.trim()
            val uid = idToken?.takeIf { it.isNotEmpty() }?.let { tokenVerifier.verify(it) }
            if (uid == null) {
                log.warn("notify refused: no valid ID token")
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            val request = runCatching { call.receive<NotifyRequest>() }.getOrNull()
            if (request == null || !NotifyService.isValidId(request.chatId) || !NotifyService.isValidId(request.messageId)) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            when (val result = notifyService.notify(uid, request.chatId, request.messageId)) {
                is NotifyResult.Sent -> {
                    // Ids only in the log: no text, no tokens.
                    log.info("notify chat={} message={} devices={}", request.chatId, request.messageId, result.devices)
                    call.respond(NotifyResponse(result.devices))
                }
                NotifyResult.ChatNotFound, NotifyResult.MessageNotFound -> {
                    log.warn("notify refused: not found chat={} message={}", request.chatId, request.messageId)
                    call.respond(HttpStatusCode.NotFound)
                }
                NotifyResult.Forbidden -> {
                    log.warn("notify refused: forbidden chat={} message={}", request.chatId, request.messageId)
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }
    }
}

private const val BEARER = "Bearer "
