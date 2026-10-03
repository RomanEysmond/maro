package com.maro.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class NotifyServiceTest {

    private val store = FakeChatStore().apply {
        chats["a_b"] = listOf("a", "b")
        messages["a_b" to "m1"] = "a"
        devices["a"] = mutableListOf(Device("a-phone", "token-a"))
        devices["b"] = mutableListOf(Device("b-phone", "token-b1"), Device("b-tablet", "token-b2"))
    }
    private val sender = FakePushSender()
    private val service = NotifyService(store, sender)

    @Test
    fun `the other participant gets an ids-only push on every device, the sender gets none`() = runTest {
        val result = service.notify("a", "a_b", "m1")

        assertEquals(NotifyResult.Sent(2), result)
        assertEquals(listOf("token-b1", "token-b2"), sender.sent.map { it.first })
        assertEquals(
            mapOf("type" to "message", "chatId" to "a_b", "messageId" to "m1"),
            sender.sent.first().second,
        )
    }

    @Test
    fun `only a participant who wrote the message may announce it`() = runTest {
        store.messages["a_b" to "m2"] = "b"

        assertEquals(NotifyResult.Forbidden, service.notify("stranger", "a_b", "m1"))
        assertEquals(NotifyResult.Forbidden, service.notify("a", "a_b", "m2"))
        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun `unknown chats and messages are reported as such`() = runTest {
        assertEquals(NotifyResult.ChatNotFound, service.notify("a", "nope", "m1"))
        assertEquals(NotifyResult.MessageNotFound, service.notify("a", "a_b", "nope"))
    }

    @Test
    fun `dead tokens are forgotten, other failures keep the device`() = runTest {
        sender.outcomes["token-b1"] = PushOutcome.TOKEN_GONE
        sender.outcomes["token-b2"] = PushOutcome.FAILED

        val result = service.notify("a", "a_b", "m1")

        assertEquals(NotifyResult.Sent(0), result)
        assertEquals(listOf("b-tablet"), store.devices["b"]?.map { it.id })
    }

    @Test
    fun `ids are limited to what chat and message ids look like`() {
        assertTrue(NotifyService.isValidId("uidA_uidB"))
        assertTrue(NotifyService.isValidId("0f8fad5b-d9cb-469f-a165-70867728950e"))
        assertEquals(false, NotifyService.isValidId("a/../users"))
        assertEquals(false, NotifyService.isValidId(""))
    }
}
