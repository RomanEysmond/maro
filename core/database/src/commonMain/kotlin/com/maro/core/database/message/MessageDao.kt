package com.maro.core.database.message

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC, id ASC")
    fun observeByChat(chatId: String): Flow<List<MessageEntity>>

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
}
