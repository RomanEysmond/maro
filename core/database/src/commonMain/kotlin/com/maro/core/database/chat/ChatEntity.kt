package com.maro.core.database.chat

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room's copy of a chat document. A direct copy of what came from Firestore last time it was fetched
 * (see `feature:chatlist:data`) — Room is the single source of truth for the UI (see CLAUDE.md).
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    /** "direct" | "group". Groups arrive in stage 6; only "direct" is written for now. */
    val type: String,
    val otherUserId: String,
    val otherUserFirstName: String,
    val otherUserLastName: String,
    val otherUserUsername: String?,
    val lastMessageText: String?,
    val lastMessageSenderId: String?,
    /** Epoch millis; `null` until stage 4 sends the first message. */
    val lastMessageAt: Long?,
    /** Epoch millis; drives the list order and is bumped by every change to the chat. */
    val updatedAt: Long,
)
