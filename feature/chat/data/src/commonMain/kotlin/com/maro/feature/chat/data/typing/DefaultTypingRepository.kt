package com.maro.feature.chat.data.typing

import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.feature.chat.domain.TypingRepository
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Writing: "typing" is refreshed at most every [REFRESH_MS] while the user types, and "stopped" is written once.
 * Reading: someone counts as typing for [TTL_MS] after their last refresh, so a client that vanished mid-word
 * (no "stopped" ever written) stops showing by itself. Expiry is measured on this device's clock from the moment
 * the update arrived; the server time is only used to ignore stale entries in the very first snapshot.
 */
class DefaultTypingRepository(
    private val remote: TypingRemoteDataSource,
    private val currentUser: CurrentUserProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val now: () -> Long = ::currentTimeMillis,
) : TypingRepository {

    private val mutex = Mutex()

    // chatId -> what was last written and when.
    private val lastWritten = mutableMapOf<String, Pair<Boolean, Long>>()

    override fun setTyping(chatId: String, isTyping: Boolean) {
        scope.launch { write(chatId, isTyping) }
    }

    private suspend fun write(chatId: String, isTyping: Boolean) {
        val userId = currentUser.userId ?: return
        val at = now()
        val needed = mutex.withLock {
            val last = lastWritten[chatId]
            val needed = if (isTyping) {
                last == null || !last.first || at - last.second >= REFRESH_MS
            } else {
                last?.first == true
            }
            if (needed) lastWritten[chatId] = isTyping to at
            needed
        }
        // Offline the write would wait for the server; a typing state that late is useless anyway.
        if (needed) withTimeoutOrNull(WRITE_TIMEOUT_MS) { remote.setTyping(chatId, userId, isTyping) }
    }

    override fun typingUsers(chatId: String): Flow<Set<String>> = channelFlow {
        val me = currentUser.userId
        val expiresAt = mutableMapOf<String, Long>()
        val lastServerAt = mutableMapOf<String, Long>()
        var firstSnapshot = true
        val lock = Mutex()

        suspend fun current(): Set<String> = lock.withLock {
            val time = now()
            expiresAt.filterValues { it > time }.keys
        }

        // Expiry needs no server event: re-evaluate every second.
        launch {
            while (true) {
                delay(TICK_MS)
                send(current())
            }
        }

        remote.observeTyping(chatId).collect { states ->
            lock.withLock {
                val time = now()
                for (state in states) {
                    if (state.userId == me) continue
                    val fresh = if (firstSnapshot) {
                        abs(time - state.atMillis) < TTL_MS
                    } else {
                        lastServerAt[state.userId] != state.atMillis
                    }
                    lastServerAt[state.userId] = state.atMillis
                    when {
                        !state.isTyping -> expiresAt.remove(state.userId)
                        fresh -> expiresAt[state.userId] = time + TTL_MS
                    }
                }
                firstSnapshot = false
            }
            send(current())
        }
    }.distinctUntilChanged()

    companion object {
        const val REFRESH_MS = 3_000L
        const val TTL_MS = 6_000L
        private const val TICK_MS = 1_000L
        private const val WRITE_TIMEOUT_MS = 5_000L
    }
}
