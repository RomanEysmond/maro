package com.maro.feature.chat.data

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/** A message as the server has it. */
data class RemoteMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    /** Epoch millis of the server timestamp. */
    val createdAt: Long,
)

interface MessageRemoteDataSource {
    /**
     * Idempotent: sending the same [RemoteMessage.id] twice writes it once. Also bumps the chat's
     * `lastMessage*` / `updatedAt`, so the chat list moves the conversation to the top.
     */
    suspend fun send(message: RemoteMessage): EmptyResult<DataError.Network>

    /** The latest messages of the chat, live. Each emission is the full current list; failures are emitted, not thrown. */
    fun observeMessages(chatId: String): Flow<Result<List<RemoteMessage>, DataError.Network>>
}
