package com.maro.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
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

@Serializable
data class UploadUrlRequest(val chatId: String, val messageId: String)

@Serializable
data class DownloadUrlRequest(val chatId: String, val key: String)

@Serializable
data class MediaUrlResponse(val key: String, val url: String)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT
    val firebase = FirebaseBackend.create()
    // Media is optional: without the S3_* variables the server still sends pushes, and media requests get 503.
    val mediaStore = S3Config.from(System::getenv)?.let(::S3MediaStore)
    if (mediaStore == null) log.warn("media storage is not configured (S3_* variables): media requests get 503")
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        maroModule(
            notifyService = NotifyService(firebase.chatStore, firebase.pushSender),
            tokenVerifier = firebase.tokenVerifier,
            mediaService = MediaService(firebase.chatStore, mediaStore),
        )
    }.start(wait = true)
}

private const val DEFAULT_PORT = 8080
private val log = LoggerFactory.getLogger("com.maro.server")

fun Application.maroModule(
    notifyService: NotifyService,
    tokenVerifier: TokenVerifier,
    mediaService: MediaService = MediaService(store = NoChats, media = null),
) {
    install(ContentNegotiation) { json() }

    routing {
        get("/health") { call.respondText("ok") }

        // Called by the sender's app right after its message reached Firestore (there are no Cloud Functions on Spark
        // to react to the write itself). Authorization: Bearer <Firebase ID token of the sender>.
        post("/v1/notify") {
            val uid = call.verifiedUid(tokenVerifier)
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

        // A URL to PUT a chat's file to (the file goes straight to the storage, not through this server).
        post("/v1/media/upload") {
            val uid = call.verifiedUid(tokenVerifier)
            if (uid == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val request = runCatching { call.receive<UploadUrlRequest>() }.getOrNull()
            if (request == null || !NotifyService.isValidId(request.chatId) || !NotifyService.isValidId(request.messageId)) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            call.respondMedia(mediaService.uploadUrl(uid, request.chatId, request.messageId), request.chatId)
        }

        // A URL to GET a chat's file from: only for the chat's participants.
        post("/v1/media/download") {
            val uid = call.verifiedUid(tokenVerifier)
            if (uid == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val request = runCatching { call.receive<DownloadUrlRequest>() }.getOrNull()
            if (request == null || !NotifyService.isValidId(request.chatId)) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            call.respondMedia(mediaService.downloadUrl(uid, request.chatId, request.key), request.chatId)
        }
    }
}

/** The uid of a valid `Authorization: Bearer <Firebase ID token>`, `null` otherwise. */
private suspend fun ApplicationCall.verifiedUid(tokenVerifier: TokenVerifier): String? =
    request.header(HttpHeaders.Authorization)
        ?.takeIf { it.startsWith(BEARER) }
        ?.removePrefix(BEARER)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { tokenVerifier.verify(it) }

private suspend fun ApplicationCall.respondMedia(result: MediaResult, chatId: String) {
    when (result) {
        is MediaResult.Url -> respond(MediaUrlResponse(result.key, result.url))
        MediaResult.ChatNotFound -> respond(HttpStatusCode.NotFound)
        MediaResult.Forbidden -> {
            // Ids only: never the URL (it grants access to the file while it is valid).
            log.warn("media refused: forbidden chat={}", chatId)
            respond(HttpStatusCode.Forbidden)
        }
        MediaResult.Unavailable -> respond(HttpStatusCode.ServiceUnavailable)
    }
}

/** For a server started without media in tests: no chats, nothing to sign. */
private object NoChats : ChatStore {
    override suspend fun participants(chatId: String): List<String>? = null
    override suspend fun messageSender(chatId: String, messageId: String): String? = null
    override suspend fun devices(uid: String): List<Device> = emptyList()
    override suspend fun removeDevice(uid: String, deviceId: String) = Unit
}

private const val BEARER = "Bearer "
