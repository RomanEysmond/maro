package com.maro.feature.chat.data.typing

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

private class FakeTypingRemote : TypingRemoteDataSource {
    val states = MutableStateFlow<List<RemoteTyping>>(emptyList())
    val writes = mutableListOf<Boolean>()

    override fun observeTyping(chatId: String): Flow<List<RemoteTyping>> = states

    override suspend fun setTyping(chatId: String, userId: String, isTyping: Boolean): EmptyResult<DataError.Network> {
        writes += isTyping
        return Result.Success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTypingRepositoryTest {

    private val remote = FakeTypingRemote()
    private val me = object : CurrentUserProvider {
        override val userId: String = "me"
        override val userIdFlow = MutableStateFlow<String?>("me")
    }

    private fun TestScope.repository() = DefaultTypingRepository(
        remote = remote,
        currentUser = me,
        scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        now = { testScheduler.currentTime },
    )

    @Test
    fun `typing is written at most every few seconds and stopping is written once`() = runTest {
        val repository = repository()

        repository.setTyping("chat", true)
        advanceTimeBy(1_000)
        repository.setTyping("chat", true)
        advanceTimeBy(DefaultTypingRepository.REFRESH_MS)
        repository.setTyping("chat", true)
        repository.setTyping("chat", false)
        repository.setTyping("chat", false)

        assertThat(remote.writes).isEqualTo(listOf(true, true, false))
    }

    @Test
    fun `others typing show up, expire on their own and stop when they say so`() = runTest {
        val repository = repository()

        repository.typingUsers("chat").test {
            assertThat(awaitItem()).isEqualTo(emptySet())

            remote.states.value = listOf(
                RemoteTyping("them", isTyping = true, atMillis = testScheduler.currentTime),
                // The user's own entry never counts.
                RemoteTyping("me", isTyping = true, atMillis = testScheduler.currentTime),
            )
            assertThat(awaitItem()).isEqualTo(setOf("them"))

            // No refresh: gone after the time to live.
            advanceTimeBy(DefaultTypingRepository.TTL_MS + 1_000)
            assertThat(awaitItem()).isEqualTo(emptySet())

            remote.states.value = listOf(RemoteTyping("them", isTyping = true, atMillis = testScheduler.currentTime))
            assertThat(awaitItem()).isEqualTo(setOf("them"))

            remote.states.value = listOf(RemoteTyping("them", isTyping = false, atMillis = testScheduler.currentTime))
            assertThat(awaitItem()).isEqualTo(emptySet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a stale entry in the first snapshot is ignored`() = runTest {
        advanceTimeBy(60_000)
        remote.states.value = listOf(RemoteTyping("them", isTyping = true, atMillis = 1_000))
        val repository = repository()

        repository.typingUsers("chat").test {
            assertThat(awaitItem()).isEqualTo(emptySet())
            advanceTimeBy(2_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
