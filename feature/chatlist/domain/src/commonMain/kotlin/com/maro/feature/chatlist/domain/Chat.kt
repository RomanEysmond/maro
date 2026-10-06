package com.maro.feature.chatlist.domain

enum class ChatType {
    DIRECT,

    GROUP,
}

/** Snapshot of a chat participant, as cached on the chat itself (no extra read to show the list). */
data class ChatParticipant(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String?,
    /** `false` for someone who has left the group (their name stays known for the history). */
    val isMember: Boolean = true,
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
    /** Empty for a photo without a caption. */
    val text: String,
    val senderId: String,
    /** Epoch millis. */
    val sentAt: Long,
    val receipt: LastMessageReceipt? = null,
    /**
     * Groups: the first name of whoever wrote it ("Anna: …");
     * `null` in a direct chat or for the user's own message.
     */
    val senderName: String? = null,
    /** A photo: the list says "Photo" instead of the text. */
    val isImage: Boolean = false,
)

data class Chat(
    val id: String,
    val type: ChatType,
    /** The other side of a direct chat; `null` for a group. */
    val otherParticipant: ChatParticipant?,
    /** Groups only. */
    val title: String? = null,
    /** Groups only: who may rename it and add people. */
    val createdBy: String? = null,
    /** Everyone known on the chat (as written on it), including people who have left a group. */
    val participants: List<ChatParticipant> = emptyList(),
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
    /** Text typed in this chat but not sent; the list shows it instead of the last message. */
    val draft: String? = null,
) {
    /** What the list shows as the chat's name. */
    val displayName: String
        get() = if (type == ChatType.GROUP) title.orEmpty() else otherParticipant?.fullName.orEmpty()

    val initials: String
        get() = if (type == ChatType.GROUP) groupInitials(title.orEmpty()) else otherParticipant?.initials ?: "?"
}

/** "Поход в горы" -> "ПВ": the first letters of the first two words. */
fun groupInitials(title: String): String = title.split(' ').filter { it.isNotBlank() }.take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("")
    .ifEmpty { "?" }
