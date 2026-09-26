package com.maro.feature.chat.data

import com.maro.core.database.chat.ChatDao
import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.message.MessageDao
import com.maro.core.database.message.MessageEntity
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.OutboxScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

class FakeMessageDao : MessageDao {
    val rows = MutableStateFlow<List<MessageEntity>>(emptyList())

    override fun observeByChat(chatId: String): Flow<List<MessageEntity>> =
        rows.map { list -> list.filter { it.chatId == chatId }.sortedWith(compareBy({ it.createdAt }, { it.id })) }

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

class FakeMessageRemoteDataSource : MessageRemoteDataSource {
    var sendResult: EmptyResult<DataError.Network> = Result.Success(Unit)
    val sent = mutableListOf<RemoteMessage>()

    /** What the fake "server" delivers to a listener; the flow ends after the last item (the real one never does). */
    var updates: List<Result<List<RemoteMessage>, DataError.Network>> = emptyList()

    override suspend fun send(message: RemoteMessage): EmptyResult<DataError.Network> {
        val result = sendResult
        if (result is Result.Success) sent += message
        return result
    }

    override fun observeMessages(chatId: String): Flow<Result<List<RemoteMessage>, DataError.Network>> = updates.asFlow()
}


class FakeOutboxScheduler : OutboxScheduler {
    var scheduleCalls = 0

    override fun schedule() {
        scheduleCalls++
    }
}

class FakeCurrentUserProvider(override var userId: String? = "me") : CurrentUserProvider
