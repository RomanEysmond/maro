package com.maro.feature.chat.data

import androidx.paging.PagingSource
import androidx.paging.testing.asPagingSourceFactory
import com.maro.core.database.chat.ChatDao
import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.message.MessageDao
import com.maro.core.database.message.MessageEntity
import com.maro.core.database.message.MessageSyncEntity
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.connectivity.ConnectivityObserver
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.OutboxScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMessageDao : MessageDao {
    val rows = MutableStateFlow<List<MessageEntity>>(emptyList())
    val syncStates = MutableStateFlow<Map<String, MessageSyncEntity>>(emptyMap())

    /** A snapshot of the rows at the time it is asked for (Room's own source also follows later changes). */
    override fun pagingSource(chatId: String): PagingSource<Int, MessageEntity> =
        rows.value.filter { it.chatId == chatId }
            .sortedWith(compareByDescending<MessageEntity> { it.createdAt }.thenByDescending { it.id })
            .asPagingSourceFactory()
            .invoke()

    override suspend fun insert(message: MessageEntity) {
        rows.value = rows.value + message
    }

    override suspend fun upsertAll(messages: List<MessageEntity>) {
        val byId = rows.value.associateBy { it.id }.toMutableMap()
        messages.forEach { byId[it.id] = it }
        rows.value = byId.values.toList()
    }

    override suspend fun getById(id: String): MessageEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun getByStatus(status: String): List<MessageEntity> =
        rows.value.filter { it.status == status }.sortedWith(compareBy({ it.createdAt }, { it.id }))

    override suspend fun updateStatus(id: String, status: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    override suspend fun updateStatusFor(from: String, to: String) {
        rows.value = rows.value.map { if (it.status == from) it.copy(status = to) else it }
    }

    override suspend fun getSyncState(chatId: String): MessageSyncEntity? = syncStates.value[chatId]

    override suspend fun getAllSyncStates(): List<MessageSyncEntity> = syncStates.value.values.toList()

    override suspend fun upsertSyncState(state: MessageSyncEntity) {
        syncStates.value = syncStates.value + (state.chatId to state)
    }
}

class FakeChatDao : ChatDao {
    val chats = MutableStateFlow<List<ChatEntity>>(emptyList())

    override fun observeAll(): Flow<List<ChatEntity>> = chats

    override fun observeById(id: String): Flow<ChatEntity?> = chats.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun upsertAll(chats: List<ChatEntity>) {
        this.chats.value = chats
    }

    override suspend fun deleteExcept(chats: List<String>) = Unit
}

/** A server with real cursor semantics over [messages], so paging and catch-up can be checked end to end. */
class FakeMessageRemoteDataSource : MessageRemoteDataSource {
    var sendResult: EmptyResult<DataError.Network> = Result.Success(Unit)
    val sent = mutableListOf<RemoteMessage>()

    /** Everything the server has, in any order. The live listener follows changes. */
    val messages = MutableStateFlow<List<RemoteMessage>>(emptyList())

    /** While set, every read fails with it. */
    var readError: DataError.Network? = null

    /** While set, the live listener fails with it (right away). */
    var listenerError: DataError.Network? = null

    /** Every read, as "latest", "newer" or "older", to check what went over the network. */
    val reads = mutableListOf<String>()

    private fun sorted(chatId: String) = messages.value.filter { it.chatId == chatId }.sortedBy { it.cursor }

    override suspend fun send(message: RemoteMessage): EmptyResult<DataError.Network> {
        val result = sendResult
        if (result is Result.Success) sent += message
        return result
    }

    override suspend fun fetchLatest(chatId: String, limit: Int): Result<List<RemoteMessage>, DataError.Network> =
        read("latest") { sorted(chatId).asReversed().take(limit) }

    override suspend fun fetchNewer(
        chatId: String,
        after: MessageCursor?,
        limit: Int,
    ): Result<List<RemoteMessage>, DataError.Network> =
        read("newer") { sorted(chatId).filter { after == null || it.cursor > after }.take(limit) }

    override suspend fun fetchOlder(
        chatId: String,
        before: MessageCursor,
        limit: Int,
    ): Result<List<RemoteMessage>, DataError.Network> =
        read("older") { sorted(chatId).filter { it.cursor < before }.asReversed().take(limit) }

    override fun observeNewer(
        chatId: String,
        after: MessageCursor?,
    ): Flow<Result<List<RemoteMessage>, DataError.Network>> = messages.map {
        listenerError?.let { error -> Result.Error(error) }
            ?: Result.Success(sorted(chatId).filter { after == null || it.cursor > after })
    }

    private fun read(kind: String, page: () -> List<RemoteMessage>): Result<List<RemoteMessage>, DataError.Network> {
        reads += kind
        return readError?.let { Result.Error(it) } ?: Result.Success(page())
    }
}

class FakeConnectivityObserver(connected: Boolean = true) : ConnectivityObserver {
    override val isConnected = MutableStateFlow(connected)
}

class FakeOutboxScheduler : OutboxScheduler {
    var scheduleCalls = 0

    override fun schedule() {
        scheduleCalls++
    }
}

class FakeCurrentUserProvider(override var userId: String? = "me") : CurrentUserProvider

/** A server message `id` at [second] seconds (microsecond timestamps, like the real server). */
fun serverMessage(id: String, second: Long, chatId: String = "chat", senderId: String = "them", text: String = id) =
    RemoteMessage(id = id, chatId = chatId, senderId = senderId, text = text, createdAtMicros = second * 1_000_000)
