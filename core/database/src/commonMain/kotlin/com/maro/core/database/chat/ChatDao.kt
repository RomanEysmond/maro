package com.maro.core.database.chat

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE id = :id")
    fun observeById(id: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    /** The list with the number of unread messages: from others, after the user's own read mark. */
    @Query(
        """
        SELECT chats.*, (
            SELECT COUNT(*) FROM messages
            WHERE messages.chatId = chats.id AND messages.senderId != :userId
              AND messages.createdAt > COALESCE(chats.myReadAt, 0)
        ) AS unreadCount
        FROM chats ORDER BY updatedAt DESC
        """,
    )
    fun observeAllWithUnread(userId: String): Flow<List<ChatWithUnread>>

    @Query("SELECT * FROM chats")
    suspend fun getAll(): List<ChatEntity>

    /** Only ever moves forward: an older value (a late server copy) never undoes a newer local one. */
    @Query("UPDATE chats SET myReadAt = :readAt WHERE id = :id AND (myReadAt IS NULL OR myReadAt < :readAt)")
    suspend fun advanceMyReadAt(id: String, readAt: Long)

    @Upsert
    suspend fun upsertAll(chats: List<ChatEntity>)

    /** Firestore's [chats] is the authoritative full list for this user: anything else cached locally is stale. */
    @Query("DELETE FROM chats WHERE id NOT IN (:chats)")
    suspend fun deleteExcept(chats: List<String>)

    /**
     * Replaces the local cache with the current server state in one go. The user's own read mark is kept when the
     * local one is newer: it is written to the server in the background and the server copy may not have caught up.
     */
    @Transaction
    suspend fun replaceAll(chats: List<ChatEntity>) {
        val localReadAt = getAll().associate { it.id to it.myReadAt }
        upsertAll(chats.map { chat -> chat.copy(myReadAt = maxOfNullable(chat.myReadAt, localReadAt[chat.id])) })
        deleteExcept(chats.map { it.id })
    }
}

data class ChatWithUnread(
    @Embedded val chat: ChatEntity,
    val unreadCount: Int,
)

private fun maxOfNullable(a: Long?, b: Long?): Long? = if (a == null) b else if (b == null) a else maxOf(a, b)
