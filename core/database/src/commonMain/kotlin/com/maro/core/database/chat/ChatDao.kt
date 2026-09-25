package com.maro.core.database.chat

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Upsert
    suspend fun upsertAll(chats: List<ChatEntity>)

    /** Firestore's [chats] is the authoritative full list for this user: anything else cached locally is stale. */
    @Query("DELETE FROM chats WHERE id NOT IN (:chats)")
    suspend fun deleteExcept(chats: List<String>)

    /** Replaces the local cache with the current server state in one go. */
    @Transaction
    suspend fun replaceAll(chats: List<ChatEntity>) {
        upsertAll(chats)
        deleteExcept(chats.map { it.id })
    }
}
