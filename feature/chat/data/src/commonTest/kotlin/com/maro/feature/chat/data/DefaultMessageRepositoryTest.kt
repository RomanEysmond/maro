package com.maro.feature.chat.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.maro.core.database.chat.ChatEntity
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.MessageRules
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.domain.OutboxResult
import com.maro.feature.chat.domain.SendError
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMessageRepositoryTest {

    private val dao = FakeMessageDao()
    private val chatDao = FakeChatDao()
    private val remote = FakeMessageRemoteDataSource()
    private val scheduler = FakeOutboxScheduler()
    private val user = FakeCurrentUserProvider("me")
    private var nextId = 0
    private var clock = 1_000L

    private val repository = DefaultMessageRepository(
        messageDao = dao,
        chatDao = chatDao,
        remote = remote,
        scheduler = scheduler,
        currentUser = user,
        // Runs the immediate send eagerly, on the test thread, so the outcome is there when sendMessage returns.
        scope = CoroutineScope(UnconfinedTestDispatcher()),
        newId = { "id-${nextId++}" },
        now = { clock++ },
    )

    private fun statusOf(id: String): String? = dao.rows.value.firstOrNull { it.id == id }?.status

    @Test
    fun `a sent message is stored trimmed, goes to the server and ends up SENT`() = runTest {
        val result = repository.sendMessage("chat", "  Привет  ")

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(remote.sent.map { it.text }).isEqualTo(listOf("Привет"))
        assertThat(remote.sent.single().senderId).isEqualTo("me")
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENT.name)
        assertThat(scheduler.scheduleCalls).isEqualTo(1)
    }

    @Test
    fun `blank text too long text and a missing session are rejected without touching the queue`() = runTest {
        assertThat(repository.sendMessage("chat", "   ")).isEqualTo(Result.Error(SendError.EMPTY))
        assertThat(repository.sendMessage("chat", "а".repeat(MessageRules.MAX_LENGTH + 1)))
            .isEqualTo(Result.Error(SendError.TOO_LONG))
        user.userId = null
        assertThat(repository.sendMessage("chat", "hi")).isEqualTo(Result.Error(SendError.NOT_SIGNED_IN))

        assertThat(dao.rows.value).hasSize(0)
        assertThat(scheduler.scheduleCalls).isEqualTo(0)
    }

    @Test
    fun `without a network the message stays SENDING and the outbox asks to be retried`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)

        repository.sendMessage("chat", "hi")

        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENDING.name)
        // No network is never counted against the message, however many attempts have passed.
        assertThat(repository.flushOutbox(attempt = 100)).isEqualTo(OutboxResult.RETRY)
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENDING.name)
    }

    @Test
    fun `when the network is back a later flush delivers the message`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendMessage("chat", "hi")

        remote.sendResult = Result.Success(Unit)
        val outcome = repository.flushOutbox(attempt = 1)

        assertThat(outcome).isEqualTo(OutboxResult.DONE)
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENT.name)
    }

    @Test
    fun `a permanent failure marks the message FAILED at once`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.FORBIDDEN)

        repository.sendMessage("chat", "hi")

        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.FAILED.name)
    }

    @Test
    fun `other failures are retried a few times and then given up on`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.SERVER_ERROR)
        repository.sendMessage("chat", "hi")

        assertThat(repository.flushOutbox(attempt = 1)).isEqualTo(OutboxResult.RETRY)
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENDING.name)

        assertThat(repository.flushOutbox(attempt = 4)).isEqualTo(OutboxResult.DONE)
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.FAILED.name)
    }

    @Test
    fun `messages go out oldest first`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendMessage("chat", "first")
        repository.sendMessage("chat", "second")

        remote.sendResult = Result.Success(Unit)
        repository.flushOutbox(attempt = 0)

        assertThat(remote.sent.map { it.text }).isEqualTo(listOf("first", "second"))
    }

    @Test
    fun `retrying a FAILED message sends it again`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.FORBIDDEN)
        repository.sendMessage("chat", "hi")
        remote.sendResult = Result.Success(Unit)

        repository.retryMessage("id-0")

        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENT.name)
    }

    @Test
    fun `retrying a message that is not FAILED does nothing`() = runTest {
        repository.sendMessage("chat", "hi")
        val sentBefore = remote.sent.size

        repository.retryMessage("id-0")

        assertThat(remote.sent).hasSize(sentBefore)
    }

    @Test
    fun `the server copy of a message replaces the local one and its time`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendMessage("chat", "hi")
        remote.updates = listOf(Result.Success(listOf(RemoteMessage("id-0", "chat", "me", "hi", createdAt = 5_000L))))

        val outcome = repository.syncMessages("chat")

        assertThat(outcome).isEqualTo(Result.Success(Unit))
        val row = dao.rows.value.single()
        assertThat(row.status).isEqualTo(MessageStatus.SENT.name)
        assertThat(row.createdAt).isEqualTo(5_000L)
    }

    @Test
    fun `sync stops on a permanent failure and reports it`() = runTest {
        remote.updates = listOf(Result.Error(DataError.Network.FORBIDDEN))

        val outcome = repository.syncMessages("chat")

        assertThat(outcome).isEqualTo(Result.Error(DataError.Network.FORBIDDEN))
    }

    @Test
    fun `messages tell outgoing from incoming by the signed in user`() = runTest {
        dao.upsertAll(
            listOf(
                RemoteMessage("a", "chat", "me", "mine", 1L).toEntity(),
                RemoteMessage("b", "chat", "them", "theirs", 2L).toEntity(),
            ),
        )

        repository.messages("chat").test {
            val messages = awaitItem()
            assertThat(messages.map { it.isOutgoing }).isEqualTo(listOf(true, false))
            assertThat(messages.map { it.text }).isEqualTo(listOf("mine", "theirs"))
        }
    }

    @Test
    fun `the header is built from the cached chat and is null for an unknown one`() = runTest {
        chatDao.chats.value = listOf(
            ChatEntity("chat", "direct", "u", "иван", "Иванов", null, null, null, null, 0L),
        )

        repository.chatHeader("chat").test {
            assertThat(awaitItem()).isEqualTo(ChatHeader("иван Иванов", "ИИ"))
        }
        repository.chatHeader("other").test {
            assertThat(awaitItem()).isNull()
        }
    }
}
