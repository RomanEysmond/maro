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
              AND messages.type != 'system'
              AND messages.createdAt > COALESCE(chats.myReadAt, 0)
        ) AS unreadCount, (
            SELECT firstName FROM chat_members
            WHERE chat_members.chatId = chats.id AND chat_members.userId = chats.lastMessageSenderId
        ) AS lastMessageSenderFirstName, (
            SELECT text FROM chat_drafts WHERE chat_drafts.chatId = chats.id
        ) AS draft
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

    /** Everyone ever in the chat, current members first. */
    @Query("SELECT * FROM chat_members WHERE chatId = :chatId ORDER BY isMember DESC, firstName, lastName")
    fun observeMembers(chatId: String): Flow<List<ChatMemberEntity>>

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId")
    suspend fun getMembers(chatId: String): List<ChatMemberEntity>

    @Upsert
    suspend fun upsertMembers(members: List<ChatMemberEntity>)

    @Query("DELETE FROM chat_members WHERE chatId NOT IN (:chats)")
    suspend fun deleteMembersExcept(chats: List<String>)

    @Query("SELECT text FROM chat_drafts WHERE chatId = :chatId")
    suspend fun getDraft(chatId: String): String?

    @Upsert
    suspend fun upsertDraft(draft: ChatDraftEntity)

    @Query("DELETE FROM chat_drafts WHERE chatId = :chatId")
    suspend fun deleteDraft(chatId: String)

    /** Drafts of chats the user is no longer in (left a group, the chat is gone). */
    @Query("DELETE FROM chat_drafts WHERE chatId NOT IN (:chats)")
    suspend fun deleteDraftsExcept(chats: List<String>)

    /**
     * Replaces the local cache with the current server state in one go. The user's own read mark is kept when the
     * local one is newer: it is written to the server in the background and the server copy may not have caught up.
     */
    @Transaction
    suspend fun replaceAll(chats: List<ChatEntity>, members: List<ChatMemberEntity> = emptyList()) {
        val localReadAt = getAll().associate { it.id to it.myReadAt }
        upsertAll(chats.map { chat -> chat.copy(myReadAt = maxOfNullable(chat.myReadAt, localReadAt[chat.id])) })
        deleteExcept(chats.map { it.id })
        upsertMembers(members)
        deleteMembersExcept(chats.map { it.id })
        deleteDraftsExcept(chats.map { it.id })
    }
}

data class ChatWithUnread(
    @Embedded val chat: ChatEntity,
    val unreadCount: Int,
    /** Groups: who wrote the last message, for the "Anna: …" preview. */
    val lastMessageSenderFirstName: String? = null,
    /** The unsent text typed in this chat, shown instead of the last message. */
    val draft: String? = null,
)

private fun maxOfNullable(a: Long?, b: Long?): Long? = if (a == null) {
    b
} else if (b == null) {
    a
} else {
    maxOf(a, b)
}
