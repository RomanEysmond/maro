package com.maro.feature.chat.data

import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.message.MessageEntity
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chat.data.media.PhotoUploader
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.ChatSyncStatus
import com.maro.feature.chat.domain.IncomingMessage
import com.maro.feature.chat.domain.MessageRules
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.domain.OutboxResult
import com.maro.feature.chat.domain.SendError
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMessageRepositoryTest {

    private val chatDao = FakeChatDao()
    private val dao = FakeMessageDao().also { it.chatDao = chatDao }
    private val remote = FakeMessageRemoteDataSource()
    private val scheduler = FakeOutboxScheduler()
    private val user = FakeCurrentUserProvider("me")
    private val pushNotifier = FakePushNotifier()
    private val media = FakeChatMedia()
    private val imageFiles = FakeImageFiles()
    private var nextId = 0
    private var clock = 1_000L

    private val repository = DefaultMessageRepository(
        messageDao = dao,
        chatDao = chatDao,
        remote = remote,
        scheduler = scheduler,
        currentUser = user,
        connectivity = FakeConnectivityObserver(),
        pushNotifier = pushNotifier,
        photos = PhotoUploader(media, imageFiles, dao),
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
        remote.messages.value = listOf(serverMessage("id-0", second = 5, senderId = "me", text = "hi"))

        repository.syncMessages("chat").first { it == ChatSyncStatus.Live }

        val row = dao.rows.value.single()
        assertThat(row.status).isEqualTo(MessageStatus.SENT.name)
        assertThat(row.createdAt).isEqualTo(5_000L)
    }

    @Test
    fun `sync stops on a permanent failure and reports it`() = runTest {
        remote.listenerError = DataError.Network.FORBIDDEN

        val last = repository.syncMessages("chat").last()

        assertThat(last).isEqualTo(ChatSyncStatus.Failed(DataError.Network.FORBIDDEN))
    }

    @Test
    fun `messages come newest first and tell outgoing from incoming by the signed in user`() = runTest {
        dao.upsertAll(
            listOf(
                serverMessage("a", second = 1, senderId = "me", text = "mine").toEntity(),
                serverMessage("b", second = 2, senderId = "them", text = "theirs").toEntity(),
            ),
        )

        val messages = repository.messages("chat").asSnapshot()

        assertThat(messages.map { it.text }).isEqualTo(listOf("theirs", "mine"))
        assertThat(messages.map { it.isOutgoing }).isEqualTo(listOf(false, true))
    }

    @Test
    fun `the recipients are notified once a message reached the server, not before`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendMessage("chat", "hi")
        assertThat(pushNotifier.notified).isEqualTo(emptyList())

        remote.sendResult = Result.Success(Unit)
        repository.flushOutbox(attempt = 1)

        assertThat(pushNotifier.notified).isEqualTo(listOf("chat" to "id-0"))
    }

    @Test
    fun `a failing push server does not affect the message`() = runTest {
        pushNotifier.result = Result.Error(DataError.Network.SERVER_ERROR)

        repository.sendMessage("chat", "hi")

        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENT.name)
    }

    @Test
    fun `an incoming message is described by its text and the other participant, own messages are not`() = runTest {
        chatDao.chats.value = listOf(
            ChatEntity("chat", "direct", "u", "Анна", "Петрова", null, null, null, null, 0L),
        )
        dao.upsertAll(
            listOf(
                serverMessage("in", second = 1, senderId = "them", text = "Привет").toEntity(),
                serverMessage("out", second = 2, senderId = "me", text = "Мой").toEntity(),
            ),
        )

        assertThat(repository.incomingMessage("chat", "in"))
            .isEqualTo(IncomingMessage("chat", "in", "Анна Петрова", "Привет"))
        assertThat(repository.incomingMessage("chat", "out")).isNull()
        assertThat(repository.incomingMessage("chat", "missing")).isNull()
        assertThat(repository.incomingMessage("other", "in")).isNull()
    }

    @Test
    fun `a chat counts as open only while its sync is collected`() = runTest {
        assertThat(repository.isChatOpen("chat")).isEqualTo(false)

        repository.syncMessages("chat").test {
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.CatchingUp)
            assertThat(repository.isChatOpen("chat")).isEqualTo(true)
            assertThat(repository.isChatOpen("other")).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(repository.isChatOpen("chat")).isEqualTo(false)
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

    private fun chatWithMarks(peerDeliveredAt: Long? = null, peerReadAt: Long? = null) = ChatEntity(
        id = "chat", type = "direct", otherUserId = "them", otherUserFirstName = "Анна", otherUserLastName = "",
        otherUserUsername = null, lastMessageText = null, lastMessageSenderId = null, lastMessageAt = null,
        updatedAt = 0L, peerReadAt = peerReadAt, peerDeliveredAt = peerDeliveredAt,
    )

    @Test
    fun `outgoing messages become DELIVERED and READ as the other side's marks pass them`() = runTest {
        dao.upsertAll(
            listOf(
                serverMessage("old", second = 1, senderId = "me").toEntity(),
                serverMessage("new", second = 2, senderId = "me").toEntity(),
                serverMessage("theirs", second = 3, senderId = "them").toEntity(),
            ),
        )
        chatDao.chats.value = listOf(chatWithMarks(peerDeliveredAt = 2_000L, peerReadAt = 1_000L))

        val statuses = repository.messages("chat").asSnapshot().associate { it.id to it.status }

        assertThat(statuses["old"]).isEqualTo(MessageStatus.READ)
        assertThat(statuses["new"]).isEqualTo(MessageStatus.DELIVERED)
        // Incoming messages carry no receipts of their own.
        assertThat(statuses["theirs"]).isEqualTo(MessageStatus.SENT)
    }

    @Test
    fun `reading a chat moves the local mark at once and reports the newest incoming message`() = runTest {
        chatDao.chats.value = listOf(chatWithMarks())
        dao.upsertAll(
            listOf(
                serverMessage("in1", second = 1, senderId = "them").toEntity(),
                serverMessage("in2", second = 2, senderId = "them").toEntity(),
                serverMessage("mine", second = 3, senderId = "me").toEntity(),
            ),
        )

        repository.markRead("chat")

        assertThat(chatDao.chats.value.single().myReadAt).isEqualTo(2_000L)
        assertThat(remote.receipts).isEqualTo(listOf(listOf("chat", "me", ReceiptKind.READ, "in2")))

        // Nothing new: no second write.
        repository.markRead("chat")
        assertThat(remote.receipts).hasSize(1)
    }

    @Test
    fun `a failed receipt is written again next time`() = runTest {
        chatDao.chats.value = listOf(chatWithMarks())
        dao.upsertAll(listOf(serverMessage("in", second = 1, senderId = "them").toEntity()))
        remote.receiptResult = Result.Error(DataError.Network.NO_INTERNET)

        repository.markRead("chat")
        remote.receiptResult = Result.Success(Unit)
        repository.markRead("chat")

        assertThat(remote.receipts).hasSize(2)
    }

    @Test
    fun `messages from others that arrive by catch-up are reported as delivered`() = runTest {
        remote.messages.value = listOf(serverMessage("in", second = 1, senderId = "them"))

        repository.catchUp("chat")

        assertThat(remote.receipts).isEqualTo(listOf(listOf("chat", "me", ReceiptKind.DELIVERED, "in")))
    }

    @Test
    fun `a draft is kept per chat and a blank one is removed`() = runTest {
        repository.saveDraft("chat", "half a thought")
        repository.saveDraft("other", "elsewhere")
        assertThat(repository.draft("chat")).isEqualTo("half a thought")

        repository.saveDraft("chat", "   ")

        assertThat(repository.draft("chat")).isEqualTo("")
        assertThat(repository.draft("other")).isEqualTo("elsewhere")
    }

    @Test
    fun `unsent counts queued and failed messages, not delivered ones`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendMessage("chat", "queued")
        dao.upsertAll(
            listOf(
                MessageEntity("failed", "chat", "me", "x", 1L, MessageStatus.FAILED.name),
                MessageEntity("sent", "chat", "me", "y", 2L, MessageStatus.SENT.name),
            ),
        )

        assertThat(repository.unsentCount()).isEqualTo(2)
    }

    @Test
    fun `a photo is uploaded first, then sent as a message pointing at it`() = runTest {
        val result = repository.sendImage("chat", "content://picked/1")

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(media.uploads).isEqualTo(listOf(Triple("chat", "id-0", FakeImageFiles.PREPARED_SIZE)))
        val sent = remote.sent.single()
        assertThat(sent.image).isEqualTo(RemoteImage("chats/chat/id-0", 1600, 1200))
        assertThat(sent.text).isEqualTo("")
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENT.name)
        assertThat(dao.localMedia["id-0"]?.uploaded).isEqualTo(true)
    }

    @Test
    fun `a failed upload sends nothing and a retry uploads again`() = runTest {
        media.uploadResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendImage("chat", "content://picked/1")
        assertThat(remote.sent).hasSize(0)
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENDING.name)

        media.uploadResult = Result.Success(Unit)
        repository.flushOutbox(attempt = 1)

        assertThat(media.uploads).hasSize(2)
        assertThat(remote.sent).hasSize(1)
    }

    @Test
    fun `an uploaded photo is not uploaded again when only the send has to be repeated`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendImage("chat", "content://picked/1")
        assertThat(media.uploads).hasSize(1)

        remote.sendResult = Result.Success(Unit)
        repository.flushOutbox(attempt = 1)

        assertThat(media.uploads).hasSize(1)
        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.SENT.name)
    }

    @Test
    fun `a picture that cannot be read is refused and nothing is queued`() = runTest {
        assertThat(repository.sendImage("chat", "content://gone"))
            .isEqualTo(Result.Error(SendError.UNREADABLE_IMAGE))
        assertThat(dao.rows.value).hasSize(0)
    }

    @Test
    fun `a photo whose local copy is gone fails for good`() = runTest {
        remote.sendResult = Result.Error(DataError.Network.NO_INTERNET)
        repository.sendImage("chat", "content://picked/1")
        imageFiles.files.clear()
        dao.localMedia.clear()

        remote.sendResult = Result.Success(Unit)
        repository.flushOutbox(attempt = 1)

        assertThat(statusOf("id-0")).isEqualTo(MessageStatus.FAILED.name)
    }
}
