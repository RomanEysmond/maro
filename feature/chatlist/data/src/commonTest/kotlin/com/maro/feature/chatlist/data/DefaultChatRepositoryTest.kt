package com.maro.feature.chatlist.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.maro.core.database.message.MessageEntity
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatParticipant
import com.maro.feature.chatlist.domain.ChatType
import com.maro.feature.chatlist.domain.LastMessage
import com.maro.feature.chatlist.domain.LastMessageReceipt
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultChatRepositoryTest {

    private val chatA = Chat(
        id = "chat-a",
        type = ChatType.DIRECT,
        otherParticipant = ChatParticipant("uid-a", "Иван", "Иванов", "ivan"),
        lastMessage = null,
        updatedAt = 1_000L,
    )
    private val chatB = chatA.copy(id = "chat-b", updatedAt = 2_000L)

    /** Who is signed in; tests switch it to play sign-out and another account in the same process. */
    private val signedIn = MutableStateFlow<String?>("me")

    private fun repository(dao: FakeChatDao, remote: FakeChatRemoteDataSource) = DefaultChatRepository(
        chatDao = dao,
        remote = remote,
        currentUser = object : CurrentUserProvider {
            override val userIdFlow = signedIn
            override val userId: String? get() = signedIn.value
        },
        listenerScope = CoroutineScope(UnconfinedTestDispatcher()),
    )

    @Test
    fun `sync upserts the fetched chats into the local cache`() = runTest {
        val dao = FakeChatDao()
        val remote = FakeChatRemoteDataSource().apply { fetchResult = Result.Success(listOf(chatA, chatB)) }
        val repository = repository(dao, remote)

        val result = repository.sync()

        assertThat(result).isEqualTo(Result.Success(Unit))
        repository.chats.test {
            assertThat(awaitItem()).isEqualTo(listOf(chatB, chatA)) // newest first
        }
    }

    @Test
    fun `sync failure is returned and the cache is untouched`() = runTest {
        val dao = FakeChatDao()
        val remote = FakeChatRemoteDataSource().apply { fetchResult = Result.Error(DataError.Network.NO_INTERNET) }
        val repository = repository(dao, remote)

        val result = repository.sync()

        assertThat(result).isInstanceOf<Result.Error<DataError.Network>>()
        repository.chats.test {
            assertThat(awaitItem()).isEqualTo(emptyList())
        }
    }

    @Test
    fun `background updates from the listener reach the chats flow`() = runTest {
        val dao = FakeChatDao()
        val remote = FakeChatRemoteDataSource()
        val repository = repository(dao, remote)

        repository.chats.test {
            assertThat(awaitItem()).isEqualTo(emptyList())

            remote.emit(Result.Success(listOf(chatA)))
            assertThat(awaitItem()).isEqualTo(listOf(chatA))
        }
    }

    @Test
    fun `a chat missing from a later sync is dropped from the cache`() = runTest {
        val dao = FakeChatDao()
        val remote = FakeChatRemoteDataSource().apply { fetchResult = Result.Success(listOf(chatA, chatB)) }
        val repository = repository(dao, remote)
        repository.sync()

        remote.fetchResult = Result.Success(listOf(chatB))
        repository.sync()

        repository.chats.test {
            assertThat(awaitItem()).isEqualTo(listOf(chatB))
        }
    }

    private fun message(id: String, senderId: String, at: Long) =
        MessageEntity(id = id, chatId = chatA.id, senderId = senderId, text = id, createdAt = at, status = "SENT")

    @Test
    fun `unread counts messages from others after the read mark, and a newer local mark survives a sync`() = runTest {
        val dao = FakeChatDao()
        val remote = FakeChatRemoteDataSource().apply {
            fetchResult =
                Result.Success(listOf(chatA.copy(myReadAt = 1_000L)))
        }
        val repository = repository(dao, remote)
        dao.messagesForUnread.value = listOf(
            message("read", "them", 1_000L),
            message("new1", "them", 2_000L),
            message("new2", "them", 3_000L),
            message("mine", "me", 4_000L),
        )
        repository.sync()
        assertThat(repository.chats.value.single().unreadCount).isEqualTo(2)

        // Read on this device (ahead of the server), then the server's older copy arrives again.
        dao.advanceMyReadAt(chatA.id, 3_000L)
        repository.sync()

        assertThat(repository.chats.value.single().unreadCount).isEqualTo(0)
    }

    @Test
    fun `the user's last message shows how far it got`() = runTest {
        val dao = FakeChatDao()
        val mine = chatA.copy(lastMessage = LastMessage("Привет", "me", 2_000L))
        val remote = FakeChatRemoteDataSource()
        val repository = repository(dao, remote)

        remote.fetchResult = Result.Success(listOf(mine))
        repository.sync()
        assertThat(repository.chats.value.single().lastMessage?.receipt).isEqualTo(LastMessageReceipt.SENT)

        remote.fetchResult = Result.Success(listOf(mine.copy(peerDeliveredAt = 2_000L)))
        repository.sync()
        assertThat(repository.chats.value.single().lastMessage?.receipt).isEqualTo(LastMessageReceipt.DELIVERED)

        remote.fetchResult = Result.Success(listOf(mine.copy(peerDeliveredAt = 2_000L, peerReadAt = 2_000L)))
        repository.sync()
        assertThat(repository.chats.value.single().lastMessage?.receipt).isEqualTo(LastMessageReceipt.READ)

        // Someone else's message carries no receipt.
        remote.fetchResult = Result.Success(listOf(chatA.copy(lastMessage = LastMessage("Hi", "them", 3_000L))))
        repository.sync()
        assertThat(repository.chats.value.single().lastMessage?.receipt).isEqualTo(null)
    }

    @Test
    fun `another account in the same process gets its own unread counts`() = runTest {
        val dao = FakeChatDao()
        val repository = repository(dao, FakeChatRemoteDataSource())
        dao.upsertAll(listOf(chatA.toEntity()))
        // Written by "me": not unread for me, unread for Anna.
        dao.messagesForUnread.value = listOf(MessageEntity("m1", "chat-a", "me", "hi", 5_000L, "SENT"))
        assertThat(repository.chats.value.single().unreadCount).isEqualTo(0)

        signedIn.value = "anna"

        assertThat(repository.chats.value.single().unreadCount).isEqualTo(1)
    }

    @Test
    fun `with nobody signed in the list is empty`() = runTest {
        val dao = FakeChatDao()
        val repository = repository(dao, FakeChatRemoteDataSource())
        dao.upsertAll(listOf(chatA.toEntity()))

        signedIn.value = null

        assertThat(repository.chats.value).isEmpty()
    }

    @Test
    fun `a chat shows its draft and a blank one counts as none`() = runTest {
        val dao = FakeChatDao()
        val repository = repository(dao, FakeChatRemoteDataSource())
        dao.upsertAll(listOf(chatA.toEntity(), chatB.toEntity()))

        dao.drafts.value = mapOf("chat-a" to "half a thought", "chat-b" to "  ")

        val drafts = repository.chats.value.associate { it.id to it.draft }
        assertThat(drafts).isEqualTo(mapOf("chat-a" to "half a thought", "chat-b" to null))
    }
}
