package com.maro.server

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class NotifyRouteTest {

    private val store = FakeChatStore().apply {
        chats["a_b"] = listOf("a", "b")
        messages["a_b" to "m1"] = "a"
        devices["b"] = mutableListOf(Device("b-phone", "token-b"))
    }
    private val sender = FakePushSender()

    // "good-a" is a valid ID token of user "a"; anything else is rejected.
    private val verifier = TokenVerifier { token -> if (token == "good-a") "a" else null }

    private fun test(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { maroModule(NotifyService(store, sender), verifier) }
        block()
    }

    private suspend fun ApplicationTestBuilder.notify(token: String?, body: String) = client.post("/v1/notify") {
        token?.let { bearerAuth(it) }
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    @Test
    fun `health answers ok`() = test {
        assertEquals("ok", client.get("/health").bodyAsText())
    }

    @Test
    fun `a valid request pushes to the recipient`() = test {
        val response = notify("good-a", """{"chatId":"a_b","messageId":"m1"}""")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"devices":1}""", response.bodyAsText())
        assertEquals(listOf("token-b"), sender.sent.map { it.first })
    }

    @Test
    fun `no token or a bad one is 401`() = test {
        assertEquals(HttpStatusCode.Unauthorized, notify(null, """{"chatId":"a_b","messageId":"m1"}""").status)
        assertEquals(HttpStatusCode.Unauthorized, notify("forged", """{"chatId":"a_b","messageId":"m1"}""").status)
        assertEquals(emptyList(), sender.sent)
    }

    @Test
    fun `a malformed body or id is 400`() = test {
        assertEquals(HttpStatusCode.BadRequest, notify("good-a", """{"chatId":"a_b"}""").status)
        assertEquals(HttpStatusCode.BadRequest, notify("good-a", """{"chatId":"a/b","messageId":"m1"}""").status)
    }

    @Test
    fun `someone else's message is 403, an unknown one 404`() = test {
        store.messages["a_b" to "m2"] = "b"

        assertEquals(HttpStatusCode.Forbidden, notify("good-a", """{"chatId":"a_b","messageId":"m2"}""").status)
        assertEquals(HttpStatusCode.NotFound, notify("good-a", """{"chatId":"a_b","messageId":"m9"}""").status)
    }
}
