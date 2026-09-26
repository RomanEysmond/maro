package com.maro.feature.chat.domain

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Error
import kotlinx.coroutines.flow.Flow

enum class SendError : Error {
    EMPTY,
    TOO_LONG,
    NOT_SIGNED_IN,
}

enum class OutboxResult {
    /** Nothing left to send (everything went out or was marked FAILED). */
    DONE,

    /** Something is still pending: try again later. */
    RETRY,
}

interface MessageRepository {
    /** Backed by Room, oldest first; the screen never reads the network directly. */
    fun messages(chatId: String): Flow<List<Message>>

    /** `null` while the chat is not in the local cache. */
    fun chatHeader(chatId: String): Flow<ChatHeader?>

    /**
     * Optimistic send: the message is in Room as SENDING before this returns, and goes out in the background
     * (right away, then through [OutboxScheduler] while the network or the server is not cooperating).
     */
    suspend fun sendMessage(chatId: String, text: String): EmptyResult<SendError>

    /** Puts a FAILED message back into the queue. */
    suspend fun retryMessage(messageId: String)

    /** Keeps Room current with the server for this chat until cancelled; returns on a permanent failure. */
    suspend fun syncMessages(chatId: String): EmptyResult<DataError.Network>

    /** Sends everything that is SENDING, oldest first. [attempt] counts the previous background runs. */
    suspend fun flushOutbox(attempt: Int): OutboxResult
}

/** Asks the platform to call [MessageRepository.flushOutbox] later (Android: WorkManager, once the network is up). */
interface OutboxScheduler {
    fun schedule()
}
