package com.maro.feature.chat.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.maro.core.database.chat.ChatEntity
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatSyncStatus
import kotlin.test.Test
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MessageSynchronizerTest {

    private val dao = FakeMessageDao()
    private val chatDao = FakeChatDao()
    private val remote = FakeMessageRemoteDataSource()
    private val connectivity = FakeConnectivityObserver()
    private val synchronizer = MessageSynchronizer(dao, chatDao, remote, connectivity, retryDelay = Duration.ZERO)

    private fun serverHas(seconds: IntRange, chatId: String = "chat") {
        remote.messages.value += seconds.map { serverMessage(idOf(it, chatId), it.toLong(), chatId = chatId) }
    }

    // Ids are unique across chats, as the real UUIDs are (Room keys messages by id alone).
    private fun idOf(second: Int, chatId: String) = if (chatId == "chat") "m$second" else "$chatId-m$second"

    private fun cachedIds(chatId: String = "chat"): Set<String> =
        dao.rows.value.filter { it.chatId == chatId }.map { it.id }.toSet()

    private fun ids(seconds: IntRange, chatId: String = "chat"): Set<String> = seconds.map { idOf(it, chatId) }.toSet()

    private fun state(chatId: String = "chat") = dao.syncStates.value[chatId]

    @Test
    fun `the first catch-up of a chat loads only its latest page and remembers both edges`() = runTest {
        serverHas(1..120)

        assertThat(synchronizer.catchUp("chat")).isEqualTo(Result.Success(Unit))

        assertThat(cachedIds()).isEqualTo(ids(71..120))
        assertThat(state()?.newestId).isEqualTo("m120")
        assertThat(state()?.oldestId).isEqualTo("m71")
        assertThat(state()?.reachedStart).isEqualTo(false)
        assertThat(remote.reads).containsExactly("latest")
    }

    @Test
    fun `a short chat is complete after the first page`() = runTest {
        serverHas(1..3)

        synchronizer.catchUp("chat")

        assertThat(cachedIds()).isEqualTo(ids(1..3))
        assertThat(state()?.reachedStart).isEqualTo(true)
    }

    @Test
    fun `a later catch-up fetches everything after the newest cursor, page by page, without a gap`() = runTest {
        serverHas(1..120)
        synchronizer.catchUp("chat")
        serverHas(121..370)
        remote.reads.clear()

        synchronizer.catchUp("chat")

        assertThat(cachedIds()).isEqualTo(ids(71..370))
        assertThat(state()?.newestId).isEqualTo("m370")
        assertThat(state()?.oldestId).isEqualTo("m71")
        assertThat(remote.reads).containsExactly("newer", "newer", "newer")
    }

    @Test
    fun `older pages are loaded before the oldest cursor until the start of the chat`() = runTest {
        serverHas(1..120)
        synchronizer.catchUp("chat")

        assertThat(synchronizer.loadOlder("chat")).isEqualTo(Result.Success(false))
        assertThat(cachedIds()).isEqualTo(ids(21..120))

        assertThat(synchronizer.loadOlder("chat")).isEqualTo(Result.Success(true))
        assertThat(cachedIds()).isEqualTo(ids(1..120))
        assertThat(state()?.reachedStart).isEqualTo(true)

        remote.reads.clear()
        assertThat(synchronizer.loadOlder("chat")).isEqualTo(Result.Success(true))
        assertThat(remote.reads).isEmpty()
    }

    @Test
    fun `older history is not asked for before the chat was ever synced`() = runTest {
        serverHas(1..10)

        assertThat(synchronizer.loadOlder("chat")).isEqualTo(Result.Success(false))

        assertThat(remote.reads).isEmpty()
    }

    @Test
    fun `a failed read changes nothing and is reported`() = runTest {
        serverHas(1..120)
        synchronizer.catchUp("chat")
        val before = state()
        remote.readError = DataError.Network.NO_INTERNET

        assertThat(synchronizer.loadOlder("chat")).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
        assertThat(synchronizer.catchUp("chat")).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))

        assertThat(state()).isEqualTo(before)
        assertThat(cachedIds()).isEqualTo(ids(71..120))
    }

    @Test
    fun `an empty chat is remembered as synced and its first messages come from the very start`() = runTest {
        synchronizer.catchUp("chat")
        assertThat(state()).isNotNull()
        assertThat(state()?.newestId).isNull()
        assertThat(state()?.reachedStart).isEqualTo(true)

        serverHas(1..2)
        synchronizer.catchUp("chat")

        assertThat(cachedIds()).isEqualTo(ids(1..2))
        assertThat(state()?.oldestId).isEqualTo("m1")
        assertThat(state()?.newestId).isEqualTo("m2")
        assertThat(remote.reads).containsExactly("latest", "newer")
    }

    @Test
    fun `sync waits for the network, then catches up and goes live`() = runTest {
        serverHas(1..3)
        connectivity.isConnected.value = false
        remote.readError = DataError.Network.NO_INTERNET

        synchronizer.syncMessages("chat").test {
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.CatchingUp)
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.WaitingForNetwork)

            remote.readError = null
            connectivity.isConnected.value = true

            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.CatchingUp)
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.Live)
            assertThat(cachedIds()).isEqualTo(ids(1..3))

            // Losing the network after that only changes the status: the listener resumes by itself.
            connectivity.isConnected.value = false
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.WaitingForNetwork)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `while live new messages land in Room and move the newest cursor`() = runTest {
        serverHas(1..3)

        synchronizer.syncMessages("chat").test {
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.CatchingUp)
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.Live)

            serverHas(4..5)
            runCurrent()

            assertThat(cachedIds()).isEqualTo(ids(1..5))
            assertThat(state()?.newestId).isEqualTo("m5")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a permanent failure ends the sync`() = runTest {
        remote.readError = DataError.Network.FORBIDDEN

        synchronizer.syncMessages("chat").test {
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.CatchingUp)
            assertThat(awaitItem()).isEqualTo(ChatSyncStatus.Failed(DataError.Network.FORBIDDEN))
            awaitComplete()
        }
    }

    private fun chat(id: String, lastMessageSecond: Long?) = ChatEntity(
        id = id,
        type = "direct",
        otherUserId = "them",
        otherUserFirstName = "Иван",
        otherUserLastName = "",
        otherUserUsername = null,
        lastMessageText = null,
        lastMessageSenderId = null,
        lastMessageAt = lastMessageSecond?.times(1_000),
        updatedAt = 0L,
    )

    private fun TestScope.keepInSyncInBackground() {
        backgroundScope.launch { synchronizer.keepAllChatsInSync() }
        runCurrent()
    }

    @Test
    fun `in the background only chats with messages newer than Room are caught up`() = runTest {
        serverHas(1..5, chatId = "a")
        serverHas(1..2, chatId = "b")
        synchronizer.catchUp("b")
        remote.reads.clear()
        chatDao.chats.value = listOf(chat("a", lastMessageSecond = 5), chat("b", lastMessageSecond = 2), chat("c", null))

        keepInSyncInBackground()

        assertThat(cachedIds("a")).isEqualTo(ids(1..5, "a"))
        assertThat(remote.reads).containsExactly("latest")

        // A new message in "b" shows up in the chat list: only "b" is fetched.
        serverHas(3..3, chatId = "b")
        chatDao.chats.value = listOf(chat("a", lastMessageSecond = 5), chat("b", lastMessageSecond = 3), chat("c", null))
        runCurrent()

        assertThat(cachedIds("b")).isEqualTo(ids(1..3, "b"))
        assertThat(remote.reads).containsExactly("latest", "newer")
    }

    @Test
    fun `in the background nothing is fetched offline, and catching up resumes when the network is back`() = runTest {
        serverHas(1..5, chatId = "a")
        connectivity.isConnected.value = false
        chatDao.chats.value = listOf(chat("a", lastMessageSecond = 5))

        keepInSyncInBackground()
        assertThat(remote.reads).isEmpty()

        connectivity.isConnected.value = true
        runCurrent()

        assertThat(cachedIds("a")).isEqualTo(ids(1..5, "a"))
    }

    @Test
    fun `cursors keep microseconds, so messages in the same millisecond keep their order`() {
        val first = MessageCursor(createdAtMicros = 1_000_001, id = "b")
        val second = MessageCursor(createdAtMicros = 1_000_002, id = "a")

        assertThat(first < second).isTrue()
        assertThat(MessageCursor(5, "a") < MessageCursor(5, "b")).isTrue()
        assertThat(second < first).isFalse()
    }
}
