package com.maro.core.database.chat

import androidx.room3.Entity

/**
 * A person of a chat, as copied on the chat document (`participantInfo`): their names for the sender line of group
 * messages, "Anna is typing…" and system messages. People who left a group stay here with [isMember] `false`, so
 * the history keeps showing who wrote what.
 */
@Entity(tableName = "chat_members", primaryKeys = ["chatId", "userId"])
data class ChatMemberEntity(
    val chatId: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val username: String?,
    val isMember: Boolean,
)
