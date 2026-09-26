package com.maro.feature.chat.presentation

import androidx.paging.PagingData
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.ChatSyncStatus
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.OutboxResult
import com.maro.feature.chat.domain.SendError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeMessageRepository : MessageRepository {
    /** Newest first, as the real repository pages them. */
    var messagesList: List<Message> = emptyList()
    val headerFlow = MutableStateFlow<ChatHeader?>(null)
    val syncStatus = MutableStateFlow<ChatSyncStatus>(ChatSyncStatus.CatchingUp)

    var sendResult: EmptyResult<SendError> = Result.Success(Unit)

    val sent = mutableListOf<String>()
    val retried = mutableListOf<String>()
    val messagesRequestedFor = mutableListOf<String>()

    override fun messages(chatId: String): Flow<PagingData<Message>> {
        messagesRequestedFor += chatId
        return flowOf(PagingData.from(messagesList))
    }

    override fun chatHeader(chatId: String): Flow<ChatHeader?> = headerFlow

    override suspend fun sendMessage(chatId: String, text: String): EmptyResult<SendError> {
        sent += text
        return sendResult
    }

    override suspend fun retryMessage(messageId: String) {
        retried += messageId
    }

    override fun syncMessages(chatId: String): Flow<ChatSyncStatus> = syncStatus

    override suspend fun keepAllChatsInSync() = Unit

    override suspend fun flushOutbox(attempt: Int): OutboxResult = OutboxResult.DONE
}
