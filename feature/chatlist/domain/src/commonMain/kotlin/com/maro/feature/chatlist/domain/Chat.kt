package com.maro.feature.chatlist.domain

enum class ChatType {
    DIRECT,

    /** Arrives in stage 6. */
    GROUP,
}

/** Snapshot of a chat participant, as cached on the chat itself (no extra read to show the list). */
data class ChatParticipant(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String?,
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

/** How far the user's own last message got; `null` when the last message is someone else's. */
enum class LastMessageReceipt {
    SENT,
    DELIVERED,
    READ,
}

data class LastMessage(
    val text: String,
    val senderId: String,
    /** Epoch millis. */
    val sentAt: Long,
    val receipt: LastMessageReceipt? = null,
)

data class Chat(
    val id: String,
    val type: ChatType,
    /** The other side of a direct chat. Group chats (stage 6) will need a different shape here. */
    val otherParticipant: ChatParticipant,
    /** `null` until the first message is sent (stage 4). */
    val lastMessage: LastMessage?,
    /** Epoch millis; drives the list order. */
    val updatedAt: Long,
    /** Messages from others after the user's read mark, as cached in Room. */
    val unreadCount: Int = 0,
    /** Read / delivered marks (epoch millis of the message they point at), see `ChatEntity`. */
    val myReadAt: Long? = null,
    val peerReadAt: Long? = null,
    val peerDeliveredAt: Long? = null,
)
