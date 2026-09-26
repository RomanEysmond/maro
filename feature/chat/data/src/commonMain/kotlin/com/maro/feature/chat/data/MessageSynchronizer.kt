package com.maro.feature.chat.data

import com.maro.core.database.chat.ChatDao
import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.message.MessageDao
import com.maro.core.database.message.MessageSyncEntity
import com.maro.core.domain.connectivity.ConnectivityObserver
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatSyncStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps each chat's cached history one unbroken range (see [MessageSyncEntity]) and moves its edges:
 * [catchUp] and the live listener extend it forwards from the newest cursor, [loadOlder] backwards from the
 * oldest. The cursors are the reliability; the listener only makes new messages show up sooner.
 */
internal class MessageSynchronizer(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val remote: MessageRemoteDataSource,
    private val connectivity: ConnectivityObserver,
    private val retryDelay: Duration = RETRY_DELAY,
) {
    // One sync step per chat at a time: two catch-ups that both start from "nothing synced" would each fetch
    // "the latest page" at different moments, and those two pages need not touch each other.
    private val locksGuard = Mutex()
    private val chatLocks = mutableMapOf<String, Mutex>()

    private suspend fun <T> withChatLock(chatId: String, block: suspend () -> T): T {
        val lock = locksGuard.withLock { chatLocks.getOrPut(chatId) { Mutex() } }
        return lock.withLock { block() }
    }

    /**
     * Fetches everything after the newest synced message, page by page. A chat that was never synced gets its
     * latest page only; the history before it is loaded when the user scrolls ([loadOlder]).
     */
    suspend fun catchUp(chatId: String): EmptyResult<DataError.Network> = withChatLock(chatId) { catchUpLocked(chatId) }

    /** One page of history before the oldest synced message; `true` once the start of the chat is reached. */
    suspend fun loadOlder(chatId: String): Result<Boolean, DataError.Network> = withChatLock(chatId) { loadOlderLocked(chatId) }

    private suspend fun catchUpLocked(chatId: String): EmptyResult<DataError.Network> {
        val state = messageDao.getSyncState(chatId)
        if (state == null) {
            return when (val result = remote.fetchLatest(chatId, PAGE_SIZE)) {
                is Result.Error -> Result.Error(result.error)
                is Result.Success -> {
                    save(chatId, result.data, reachedStart = result.data.size < PAGE_SIZE)
                    Result.Success(Unit)
                }
            }
        }

        var after = state.newest
        while (true) {
            val page = when (val result = remote.fetchNewer(chatId, after, CATCH_UP_PAGE_SIZE)) {
                is Result.Error -> return Result.Error(result.error)
                is Result.Success -> result.data
            }
            save(chatId, page)
            if (page.size < CATCH_UP_PAGE_SIZE) return Result.Success(Unit)
            after = page.last().cursor
        }
    }

    private suspend fun loadOlderLocked(chatId: String): Result<Boolean, DataError.Network> {
        // Not synced yet: the catch-up of the open chat fills Room, and the list asks again after that.
        val state = messageDao.getSyncState(chatId) ?: return Result.Success(false)
        val oldest = state.oldest
        if (state.reachedStart || oldest == null) return Result.Success(true)

        val page = when (val result = remote.fetchOlder(chatId, oldest, PAGE_SIZE)) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> result.data
        }
        val reachedStart = page.size < PAGE_SIZE
        save(chatId, page, reachedStart)
        return Result.Success(reachedStart)
    }

    /** See [com.maro.feature.chat.domain.MessageRepository.syncMessages]. */
    fun syncMessages(chatId: String): Flow<ChatSyncStatus> = channelFlow {
        while (true) {
            send(ChatSyncStatus.CatchingUp)
            val result = catchUp(chatId)
            if (result is Result.Success) break
            val error = (result as Result.Error).error
            if (!error.isConnectivity()) {
                send(ChatSyncStatus.Failed(error))
                return@channelFlow
            }
            send(ChatSyncStatus.WaitingForNetwork)
            connectivity.isConnected.first { it }
            // "Connected" can come a moment before the server is reachable again: do not hammer it.
            delay(retryDelay)
        }

        // Caught up: from here on the listener keeps Room current (the server resumes it after a lost
        // connection by itself), and the status just follows the network.
        val status = launch {
            connectivity.isConnected.collect { connected ->
                send(if (connected) ChatSyncStatus.Live else ChatSyncStatus.WaitingForNetwork)
            }
        }
        var failure: DataError.Network? = null
        val start = messageDao.getSyncState(chatId)?.newest
        remote.observeNewer(chatId, start)
            .takeWhile { update ->
                when (update) {
                    is Result.Success -> {
                        withChatLock(chatId) { save(chatId, update.data) }
                        true
                    }
                    is Result.Error -> {
                        failure = update.error
                        false
                    }
                }
            }
            .collect()
        status.cancel()
        failure?.let { send(ChatSyncStatus.Failed(it)) }
    }

    /** See [com.maro.feature.chat.domain.MessageRepository.keepAllChatsInSync]. */
    suspend fun keepAllChatsInSync() {
        connectivity.isConnected.collectLatest { connected ->
            if (!connected) return@collectLatest
            // The chat list's own listener keeps `lastMessageAt` current, so this reacts to every new message.
            chatDao.observeAll().collect { chats ->
                val synced = messageDao.getAllSyncStates().associateBy { it.chatId }
                chats.filter { it.isAheadOf(synced[it.id]) }
                    // A failure is simply retried on the next change of the list or the next reconnect.
                    .forEach { catchUp(it.id) }
            }
        }
    }

    private fun ChatEntity.isAheadOf(state: MessageSyncEntity?): Boolean {
        val lastMessageAt = lastMessageAt ?: return false
        val syncedUntil = state?.newestAtMillis ?: return true
        return lastMessageAt > syncedUntil
    }

    private suspend fun save(chatId: String, page: List<RemoteMessage>, reachedStart: Boolean = false) {
        val state = messageDao.getSyncState(chatId).extendedWith(chatId, page, reachedStart)
        messageDao.saveSynced(page.map { it.toEntity() }, state)
    }

    companion object {
        /** A page of history, both the first page of a chat and every older one. */
        const val PAGE_SIZE = 50

        /** Catching up reads in bigger pages: all of it is needed anyway. */
        const val CATCH_UP_PAGE_SIZE = 100

        private val RETRY_DELAY = 3.seconds
    }
}

internal fun DataError.Network.isConnectivity(): Boolean =
    this == DataError.Network.NO_INTERNET || this == DataError.Network.REQUEST_TIMEOUT
