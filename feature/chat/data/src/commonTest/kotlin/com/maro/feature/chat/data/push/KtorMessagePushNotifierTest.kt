package com.maro.feature.chat.data.push

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.maro.core.data.network.HttpClientFactory
import com.maro.core.domain.auth.IdTokenProvider
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class KtorMessagePushNotifierTest {

    private val requests = mutableListOf<HttpRequestData>()
    private var status = HttpStatusCode.OK
    private var token: String? = "id-token"

    private fun notifier(baseUrl: String = "http://push.test/") = KtorMessagePushNotifier(
        client = HttpClientFactory.create(MockEngine { request -> answer(request) }),
        config = PushServerConfig(baseUrl),
        idTokens = object : IdTokenProvider {
            override suspend fun idToken(): String? = token
        },
    )

    private fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        requests += request
        return respond("""{"devices":1}""", status)
    }

    @Test
    fun `posts the ids with the sender's token`() = runTest {
        val result = notifier().messageSent("a_b", "m1")

        assertThat(result).isEqualTo(Result.Success(Unit))
        val request = requests.single()
        assertThat(request.url.toString()).isEqualTo("http://push.test/v1/notify")
        assertThat(request.headers[HttpHeaders.Authorization]).isEqualTo("Bearer id-token")
        assertThat((request.body as TextContent).text).isEqualTo("""{"chatId":"a_b","messageId":"m1"}""")
    }

    @Test
    fun `server answers become typed errors`() = runTest {
        status = HttpStatusCode.Forbidden
        assertThat(notifier().messageSent("a_b", "m1")).isEqualTo(Result.Error(DataError.Network.FORBIDDEN))

        status = HttpStatusCode.InternalServerError
        assertThat(notifier().messageSent("a_b", "m1")).isEqualTo(Result.Error(DataError.Network.SERVER_ERROR))
    }

    @Test
    fun `without a server address nothing is sent`() = runTest {
        assertThat(notifier(baseUrl = "").messageSent("a_b", "m1")).isEqualTo(Result.Success(Unit))
        assertThat(requests).isEmpty()
    }

    @Test
    fun `signed out means no request`() = runTest {
        token = null

        assertThat(notifier().messageSent("a_b", "m1")).isEqualTo(Result.Error(DataError.Network.UNAUTHORIZED))
        assertThat(requests).isEmpty()
    }
}
