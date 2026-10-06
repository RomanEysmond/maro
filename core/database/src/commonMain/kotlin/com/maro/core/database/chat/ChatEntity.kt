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
    /** "direct" | "group". */
    val type: String,
    /** Direct chats only (empty in a group): the other participant. Everyone is in [ChatMemberEntity] too. */
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
    /**
     * Epoch millis of the newest message the signed-in user has read (their `last_read_message_id`): everything
     * from others after it is unread. Set locally as soon as the chat is read, so it may run ahead of the server.
     */
    val myReadAt: Long? = null,
    /** Epoch millis up to which the other side has read: the user's messages up to here show as READ. */
    val peerReadAt: Long? = null,
    /** Epoch millis up to which the other side's device has received: the user's messages up to here are DELIVERED. */
    val peerDeliveredAt: Long? = null,
    /** Groups only. */
    val title: String? = null,
    /** Groups only: the member who may rename the group and add people. */
    val createdBy: String? = null,
    /** "text" | "image": a photo shows as "Photo" in the list. `null` for chats synced before photos existed. */
    val lastMessageType: String? = null,
)
