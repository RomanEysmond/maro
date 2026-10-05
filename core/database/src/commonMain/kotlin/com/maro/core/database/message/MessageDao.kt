package com.maro.core.database.message

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import kotlinx.coroutines.flow.Flow

@Dao
// Room 3 knows PagingSource only through this converter from room3-paging.
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface MessageDao {
    /**
     * Newest first: the chat screen draws the list bottom-up and pages towards older messages. Each row comes with
     * the other side's read / delivered marks, so a change of either refreshes the statuses on screen.
     */
    @Query(
        """
        SELECT messages.*, chats.peerReadAt AS peerReadAt, chats.peerDeliveredAt AS peerDeliveredAt
        FROM messages LEFT JOIN chats ON chats.id = messages.chatId
        WHERE messages.chatId = :chatId
        ORDER BY messages.createdAt DESC, messages.id DESC
        """,
    )
    fun pagingSource(chatId: String): PagingSource<Int, MessageWithReceipts>

    /** The newest message someone else sent in the chat, as cached. */
    @Query(
        """
        SELECT * FROM messages WHERE chatId = :chatId AND senderId != :userId
        ORDER BY createdAt DESC, id DESC LIMIT 1
        """,
    )
    fun observeNewestIncoming(chatId: String, userId: String): Flow<MessageEntity?>

    @Query(
        """
        SELECT * FROM messages WHERE chatId = :chatId AND senderId != :userId
        ORDER BY createdAt DESC, id DESC LIMIT 1
        """,
    )
    suspend fun getNewestIncoming(chatId: String, userId: String): MessageEntity?

    @Insert
    suspend fun insert(message: MessageEntity)

    /** Messages that came from the server: they replace the local row with the same id (and mark it SENT). */
    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    /** Oldest first: messages must reach the server in the order they were written. */
    @Query("SELECT * FROM messages WHERE status = :status ORDER BY createdAt ASC, id ASC")
    suspend fun getByStatus(status: String): List<MessageEntity>

    /** The user's messages that have not reached the server: still queued or given up on. */
    @Query("SELECT COUNT(*) FROM messages WHERE status IN ('SENDING', 'FAILED')")
    suspend fun countUnsent(): Int

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE messages SET status = :to WHERE status = :from")
    suspend fun updateStatusFor(from: String, to: String)

    @Query("SELECT * FROM message_sync WHERE chatId = :chatId")
    suspend fun getSyncState(chatId: String): MessageSyncEntity?

    @Query("SELECT * FROM message_sync")
    suspend fun getAllSyncStates(): List<MessageSyncEntity>

    @Upsert
    suspend fun upsertSyncState(state: MessageSyncEntity)

    /** A page from the server and the range it extends land together: the cursors never point past the rows. */
    @Transaction
    suspend fun saveSynced(messages: List<MessageEntity>, state: MessageSyncEntity) {
        upsertAll(messages)
        upsertSyncState(state)
    }
}

data class MessageWithReceipts(
    @Embedded val message: MessageEntity,
    val peerReadAt: Long?,
    val peerDeliveredAt: Long?,
)
