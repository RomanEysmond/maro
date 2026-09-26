package com.maro.feature.chat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.maro.core.database.chat.ChatDao
import com.maro.core.database.message.MessageDao
import com.maro.core.database.message.MessageEntity
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.connectivity.ConnectivityObserver
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.ChatSyncStatus
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.MessageRules
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.domain.OutboxResult
import com.maro.feature.chat.domain.OutboxScheduler
import com.maro.feature.chat.domain.SendError
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalUuidApi::class)
private fun randomMessageId(): String = Uuid.random().toString()

private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

class DefaultMessageRepository(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val remote: MessageRemoteDataSource,
    private val scheduler: OutboxScheduler,
    private val currentUser: CurrentUserProvider,
    connectivity: ConnectivityObserver,
    // Same story as DefaultChatRepository: no lifecycle owner yet, so the process is the scope by default;
    // overridable so tests can drive the immediate send with a TestScope.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val newId: () -> String = ::randomMessageId,
    private val now: () -> Long = ::currentTimeMillis,
) : MessageRepository {

    // One flush at a time: the immediate send and the WorkManager run must not race over the same rows
    // (a double send would be harmless anyway — the server write is idempotent — but it is wasted work).
    private val outboxMutex = Mutex()

    private val synchronizer = MessageSynchronizer(messageDao, chatDao, remote, connectivity)

    @OptIn(ExperimentalPagingApi::class)
    override fun messages(chatId: String): Flow<PagingData<Message>> =
        Pager(
            config = PagingConfig(pageSize = MessageSynchronizer.PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = MessageRemoteMediator(chatId, synchronizer),
            pagingSourceFactory = { messageDao.pagingSource(chatId) },
        ).flow.map { page ->
            val userId = currentUser.userId
            page.map { it.toDomain(userId) }
        }

    override fun chatHeader(chatId: String): Flow<ChatHeader?> =
        chatDao.observeById(chatId).map { chat ->
            chat?.let {
                val name = listOf(it.otherUserFirstName, it.otherUserLastName).filter { part -> part.isNotBlank() }
                ChatHeader(
                    participantName = name.joinToString(" "),
                    initials = name.mapNotNull { part -> part.trim().firstOrNull()?.uppercaseChar() }
                        .joinToString("").ifEmpty { "?" },
                )
            }
        }

    override suspend fun sendMessage(chatId: String, text: String): EmptyResult<SendError> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.Error(SendError.EMPTY)
        if (trimmed.length > MessageRules.MAX_LENGTH) return Result.Error(SendError.TOO_LONG)
        val userId = currentUser.userId ?: return Result.Error(SendError.NOT_SIGNED_IN)

        messageDao.insert(
            MessageEntity(
                id = newId(),
                chatId = chatId,
                senderId = userId,
                text = trimmed,
                createdAt = now(),
                status = MessageStatus.SENDING.name,
            ),
        )
        dispatchOutbox()
        return Result.Success(Unit)
    }

    override suspend fun retryMessage(messageId: String) {
        val message = messageDao.getById(messageId) ?: return
        if (message.status != MessageStatus.FAILED.name) return
        messageDao.updateStatus(messageId, MessageStatus.SENDING.name)
        dispatchOutbox()
    }

    override fun syncMessages(chatId: String): Flow<ChatSyncStatus> = synchronizer.syncMessages(chatId)

    override suspend fun keepAllChatsInSync() = synchronizer.keepAllChatsInSync()

    override suspend fun flushOutbox(attempt: Int): OutboxResult = outboxMutex.withLock {
        for (message in messageDao.getByStatus(MessageStatus.SENDING.name)) {
            when (val result = remote.send(message.toRemote())) {
                is Result.Success -> messageDao.updateStatus(message.id, MessageStatus.SENT.name)
                is Result.Error -> when {
                    result.error.isPermanent() -> messageDao.updateStatus(message.id, MessageStatus.FAILED.name)
                    // No network is not the message's fault: keep waiting, however long it takes.
                    result.error.isConnectivity() -> return@withLock OutboxResult.RETRY
                    attempt >= MAX_ATTEMPTS -> {
                        messageDao.updateStatusFor(MessageStatus.SENDING.name, MessageStatus.FAILED.name)
                        return@withLock OutboxResult.DONE
                    }
                    else -> return@withLock OutboxResult.RETRY
                }
            }
        }
        OutboxResult.DONE
    }

    /** Right away in-process for latency; the scheduler is the safety net (survives the app being closed). */
    private fun dispatchOutbox() {
        scheduler.schedule()
        scope.launch { flushOutbox(attempt = 0) }
    }

    private fun DataError.Network.isPermanent(): Boolean = when (this) {
        DataError.Network.BAD_REQUEST,
        DataError.Network.UNAUTHORIZED,
        DataError.Network.FORBIDDEN,
        DataError.Network.NOT_FOUND,
        DataError.Network.CONFLICT,
        DataError.Network.PAYLOAD_TOO_LARGE,
        DataError.Network.SERIALIZATION,
        -> true
        else -> false
    }

    private companion object {
        /** Background runs that may fail for reasons other than connectivity before the message is given up on. */
        const val MAX_ATTEMPTS = 4
    }
}
