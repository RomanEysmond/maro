package com.maro.feature.chat.presentation

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.OutboxResult
import com.maro.feature.chat.domain.SendError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMessageRepository : MessageRepository {
    val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    val headerFlow = MutableStateFlow<ChatHeader?>(null)

    var sendResult: EmptyResult<SendError> = Result.Success(Unit)
    var syncResult: EmptyResult<DataError.Network> = Result.Success(Unit)

    val sent = mutableListOf<String>()
    val retried = mutableListOf<String>()

    override fun messages(chatId: String): Flow<List<Message>> = messagesFlow

    override fun chatHeader(chatId: String): Flow<ChatHeader?> = headerFlow

    override suspend fun sendMessage(chatId: String, text: String): EmptyResult<SendError> {
        sent += text
        return sendResult
    }

    override suspend fun retryMessage(messageId: String) {
        retried += messageId
    }

    override suspend fun syncMessages(chatId: String): EmptyResult<DataError.Network> = syncResult

    override suspend fun flushOutbox(attempt: Int): OutboxResult = OutboxResult.DONE
}
