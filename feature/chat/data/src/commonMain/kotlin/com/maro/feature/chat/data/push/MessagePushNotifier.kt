package com.maro.feature.chat.data.push

import com.maro.core.domain.auth.IdTokenProvider
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.Serializable

/**
 * Asks the push server to wake the other participants up once a message has reached the server. Only speeds delivery
 * up: without it the recipients still get the message from their listeners and catch-up, just later.
 */
interface MessagePushNotifier {
    suspend fun messageSent(chatId: String, messageId: String): EmptyResult<DataError.Network>
}

/** Where the push server is; an empty [baseUrl] switches pushes off (for example a build without a server yet). */
data class PushServerConfig(val baseUrl: String)

@Serializable
internal data class NotifyRequestDto(val chatId: String, val messageId: String)

/** `POST {baseUrl}/v1/notify` with the sender's ID token (see server/). */
class KtorMessagePushNotifier(
    private val client: HttpClient,
    private val config: PushServerConfig,
    private val idTokens: IdTokenProvider,
) : MessagePushNotifier {

    override suspend fun messageSent(chatId: String, messageId: String): EmptyResult<DataError.Network> {
        if (config.baseUrl.isBlank()) return Result.Success(Unit)
        val token = idTokens.idToken() ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        return try {
            var status = post(token, chatId, messageId)
            // The cached token may have expired by the server's clock while it still looks valid by the device's
            // (a device whose clock is off): once, with a freshly issued token.
            if (status == UNAUTHORIZED) {
                val fresh = idTokens.idToken(forceRefresh = true) ?: return Result.Error(DataError.Network.UNAUTHORIZED)
                status = post(fresh, chatId, messageId)
            }
            when (status) {
                in 200..299 -> Result.Success(Unit)
                400 -> Result.Error(DataError.Network.BAD_REQUEST)
                401 -> Result.Error(DataError.Network.UNAUTHORIZED)
                403 -> Result.Error(DataError.Network.FORBIDDEN)
                404 -> Result.Error(DataError.Network.NOT_FOUND)
                in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
                else -> Result.Error(DataError.Network.UNKNOWN)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: IOException) {
            Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }
    }

    private suspend fun post(token: String, chatId: String, messageId: String): Int =
        client.post("${config.baseUrl.trimEnd('/')}/v1/notify") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(NotifyRequestDto(chatId, messageId))
        }.status.value

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
