package com.maro.feature.chat.domain

enum class MessageStatus {
    /** Written locally, not confirmed by the server yet (also while waiting for the network). */
    SENDING,
    SENT,

    /** Gave up after a permanent failure or too many attempts; the user can retry by hand. */
    FAILED,
}

data class Message(
    /** The `client_id` (UUID): also the id of the document on the server. */
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    /** Epoch millis. */
    val createdAt: Long,
    val status: MessageStatus,
    val isOutgoing: Boolean,
)

/** What the conversation screen shows in its top bar. */
data class ChatHeader(
    val participantName: String,
    val initials: String,
)

object MessageRules {
    const val MAX_LENGTH = 4000
}
