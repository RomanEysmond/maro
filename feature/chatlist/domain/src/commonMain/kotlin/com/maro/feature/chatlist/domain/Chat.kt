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

data class LastMessage(
    val text: String,
    val senderId: String,
    /** Epoch millis. */
    val sentAt: Long,
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
)
