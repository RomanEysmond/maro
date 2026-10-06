package com.maro.feature.chat.data

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/** A position in a chat's history: the server order is by time, then by id for messages written at the same time. */
data class MessageCursor(
    /** Microseconds, the full precision of the server timestamp. */
    val createdAtMicros: Long,
    val id: String,
) : Comparable<MessageCursor> {
    override fun compareTo(other: MessageCursor): Int =
        compareValuesBy(this, other, MessageCursor::createdAtMicros, MessageCursor::id)
}

/** A message as the server has it. */
data class RemoteMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    /** Microseconds of the server timestamp (ignored when sending: the server sets it). */
    val createdAtMicros: Long,
    /** Set for a system message (group events): `kind` is "created" | "added" | "left" | "renamed". */
    val systemEvent: RemoteSystemEvent? = null,
    /** Set for a photo: the file in the media storage and its size; [text] is empty then. */
    val image: RemoteImage? = null,
) {
    val cursor: MessageCursor
        get() = MessageCursor(createdAtMicros, id)
}

data class RemoteImage(val key: String, val width: Int, val height: Int)

data class RemoteSystemEvent(val kind: String, val targets: List<String> = emptyList(), val title: String? = null)

/**
 * Every read goes to the server (never a local cache), so a failure is reported as one instead of looking like
 * "no messages". Lists are in the order the method names; failures are returned or emitted, never thrown.
 */
interface MessageRemoteDataSource {
    /**
     * Idempotent: sending the same [RemoteMessage.id] twice writes it once. Also bumps the chat's
     * `lastMessage*` / `updatedAt`, so the chat list moves the conversation to the top.
     */
    suspend fun send(message: RemoteMessage): EmptyResult<DataError.Network>

    /** The newest [limit] messages of the chat, newest first. */
    suspend fun fetchLatest(chatId: String, limit: Int): Result<List<RemoteMessage>, DataError.Network>

    /** Up to [limit] messages right after [after] (from the very first one when `null`), oldest first. */
    suspend fun fetchNewer(
        chatId: String,
        after: MessageCursor?,
        limit: Int,
    ): Result<List<RemoteMessage>, DataError.Network>

    /** Up to [limit] messages right before [before], newest first. */
    suspend fun fetchOlder(
        chatId: String,
        before: MessageCursor,
        limit: Int,
    ): Result<List<RemoteMessage>, DataError.Network>

    /**
     * Live: every message after [after] (all of them when `null`), oldest first. Each emission is the full
     * current list, not a delta.
     */
    fun observeNewer(chatId: String, after: MessageCursor?): Flow<Result<List<RemoteMessage>, DataError.Network>>

    /**
     * Moves [userId]'s read or delivered mark in the chat to the message [messageId] sent at [atMillis]
     * (`lastRead` / `lastDelivered` on the chat: one entry per participant, never a flag per message).
     */
    suspend fun reportReceipt(
        chatId: String,
        userId: String,
        kind: ReceiptKind,
        messageId: String,
        atMillis: Long,
    ): EmptyResult<DataError.Network>
}

enum class ReceiptKind {
    /** The user's device has the message. */
    DELIVERED,

    /** The user has seen it. */
    READ,
}
