package com.maro.feature.chat.data.media

import com.maro.core.domain.auth.IdTokenProvider
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.data.push.PushServerConfig
import com.maro.feature.chat.domain.ChatImage
import com.maro.feature.chat.domain.ChatImageUrls
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.Serializable

/**
 * The network side of photos: our server hands out short-lived storage URLs (only to the chat's participants), the
 * bytes go straight to the storage and back. The storage keys never reach the app.
 */
interface ChatMediaRemoteDataSource : ChatImageUrls {
    /** Puts the picture of [messageId] where the message will say it is (`chats/{chatId}/{messageId}`). */
    suspend fun upload(chatId: String, messageId: String, bytes: ByteArray): EmptyResult<DataError.Network>
}

@Serializable
internal data class UploadUrlRequestDto(val chatId: String, val messageId: String)

@Serializable
internal data class DownloadUrlRequestDto(val chatId: String, val key: String)

@Serializable
internal data class MediaUrlDto(val key: String, val url: String)

/** `POST {baseUrl}/v1/media/upload|download` with the user's ID token (see server/), then the storage itself. */
class KtorChatMediaRemoteDataSource(
    private val client: HttpClient,
    private val config: PushServerConfig,
    private val idTokens: IdTokenProvider,
) : ChatMediaRemoteDataSource {

    override suspend fun upload(chatId: String, messageId: String, bytes: ByteArray): EmptyResult<DataError.Network> =
        networkCall {
            when (val url = mediaUrl(UPLOAD_PATH) { setBody(UploadUrlRequestDto(chatId, messageId)) }) {
                is Result.Error -> url

                is Result.Success -> {
                    val response = client.put(url.data) {
                        contentType(ContentType.Image.JPEG)
                        setBody(bytes)
                    }
                    if (response.status.isSuccess()) Result.Success(Unit) else Result.Error(response.toError())
                }
            }
        }

    override suspend fun downloadUrl(image: ChatImage): Result<String, DataError.Network> =
        networkCall { mediaUrl(DOWNLOAD_PATH) { setBody(DownloadUrlRequestDto(image.chatId, image.key)) } }

    private suspend fun mediaUrl(
        path: String,
        body: HttpRequestBuilder.() -> Unit,
    ): Result<String, DataError.Network> {
        // Without a server (a release build before hosting) there is nowhere to put or get photos.
        if (config.baseUrl.isBlank()) return Result.Error(DataError.Network.SERVICE_UNAVAILABLE)
        val token = idTokens.idToken() ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        var response = post(path, token, body)
        // A token the server finds expired (a device whose clock is off): once more with a fresh one.
        if (response.status.value == UNAUTHORIZED) {
            val fresh = idTokens.idToken(forceRefresh = true) ?: return Result.Error(DataError.Network.UNAUTHORIZED)
            response = post(path, fresh, body)
        }
        return if (response.status.isSuccess()) {
            Result.Success(response.body<MediaUrlDto>().url)
        } else {
            Result.Error(response.toError())
        }
    }

    private suspend fun post(path: String, token: String, body: HttpRequestBuilder.() -> Unit): HttpResponse =
        client.post("${config.baseUrl.trimEnd('/')}$path") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            body()
        }

    private inline fun <T> networkCall(block: () -> Result<T, DataError.Network>): Result<T, DataError.Network> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (e: IOException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: Exception) {
        Result.Error(DataError.Network.UNKNOWN)
    }

    private fun HttpResponse.toError(): DataError.Network = when (status.value) {
        BAD_REQUEST -> DataError.Network.BAD_REQUEST
        UNAUTHORIZED -> DataError.Network.UNAUTHORIZED
        FORBIDDEN -> DataError.Network.FORBIDDEN
        NOT_FOUND -> DataError.Network.NOT_FOUND
        SERVICE_UNAVAILABLE -> DataError.Network.SERVICE_UNAVAILABLE
        in SERVER_ERRORS -> DataError.Network.SERVER_ERROR
        else -> DataError.Network.UNKNOWN
    }

    private companion object {
        const val UPLOAD_PATH = "/v1/media/upload"
        const val DOWNLOAD_PATH = "/v1/media/download"
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val SERVICE_UNAVAILABLE = 503
        val SERVER_ERRORS = 500..599
    }
}
