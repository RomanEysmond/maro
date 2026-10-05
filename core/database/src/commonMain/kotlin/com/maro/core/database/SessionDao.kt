package com.maro.core.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction

/** Everything stored here belongs to the signed-in user: on sign-out all of it goes. */
@Dao
interface SessionDao {
    @Query("DELETE FROM messages")
    suspend fun deleteMessages()

    @Query("DELETE FROM message_sync")
    suspend fun deleteSyncStates()

    @Query("DELETE FROM chat_members")
    suspend fun deleteMembers()

    @Query("DELETE FROM chat_drafts")
    suspend fun deleteDrafts()

    @Query("DELETE FROM chats")
    suspend fun deleteChats()

    @Transaction
    suspend fun clearAll() {
        deleteMessages()
        deleteSyncStates()
        deleteMembers()
        deleteDrafts()
        deleteChats()
    }
}
