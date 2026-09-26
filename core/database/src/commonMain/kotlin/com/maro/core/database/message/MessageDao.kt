package com.maro.core.database.message

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter

@Dao
// Room 3 knows PagingSource only through this converter from room3-paging.
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface MessageDao {
    /** Newest first: the chat screen draws the list bottom-up and pages towards older messages. */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC, id DESC")
    fun pagingSource(chatId: String): PagingSource<Int, MessageEntity>

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
