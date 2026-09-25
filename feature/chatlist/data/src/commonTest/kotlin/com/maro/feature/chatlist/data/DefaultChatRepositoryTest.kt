package com.maro.feature.chatlist.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatParticipant
import com.maro.feature.chatlist.domain.ChatType
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun repository(dao: FakeChatDao, remote: FakeChatRemoteDataSource) =
        DefaultChatRepository(dao, remote, listenerScope = CoroutineScope(UnconfinedTestDispatcher()))

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
}
