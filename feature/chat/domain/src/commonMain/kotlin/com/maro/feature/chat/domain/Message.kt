package com.maro.feature.chat.domain

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result

enum class MessageStatus {
    /** Written locally, not confirmed by the server yet (also while waiting for the network). */
    SENDING,
    SENT,

    /** The other side's device has received it (outgoing messages only). */
    DELIVERED,

    /** The other side has read it (in a group: at least one participant has). */
    READ,

    /** Gave up after a permanent failure or too many attempts; the user can retry by hand. */
    FAILED,
}

enum class SystemEventKind {
    CREATED,
    ADDED,
    LEFT,
    RENAMED,
}

/**
 * What happened to a group, told in the conversation ("Anna added Peter"). Only ids and data: the text is put
 * together on the device, in its language, with the names it knows.
 */
data class SystemEvent(
    val kind: SystemEventKind,
    /** Who the event is about (added people); empty for the others. */
    val targetIds: List<String> = emptyList(),
    /** The title the group got (created, renamed). */
    val title: String? = null,
)

data class Message(
    /** The `client_id` (UUID): also the id of the document on the server. */
    val id: String,
    val chatId: String,
    /** For a system message: who did it. */
    val senderId: String,
    /** Empty for a system message. */
    val text: String,
    /** Epoch millis. */
    val createdAt: Long,
    val status: MessageStatus,
    val isOutgoing: Boolean,
    /** Set for a system message (group events), `null` for an ordinary one. */
    val systemEvent: SystemEvent? = null,
    /** Set for a photo; [text] is empty then. */
    val image: MessageImage? = null,
)

/** A photo in a conversation. */
data class MessageImage(
    /** Where the picture comes from: the storage, through the server (see [ChatImage]). */
    val source: ChatImage,
    /** Pixels: the bubble takes the picture's shape before it has loaded. */
    val width: Int,
    val height: Int,
    /** The sender's own copy on this device: shown at once, even before (and without) any upload. */
    val localPath: String? = null,
)

/**
 * A chat's file in the media storage, as an image loader asks for it: the server gives a short-lived URL for [key]
 * to the chat's participants only. [key] alone identifies the picture (the URL changes every time).
 */
data class ChatImage(val chatId: String, val key: String)

/** Getting a [ChatImage] from the storage; the server checks that the user may see it. */
interface ChatImageUrls {
    /** A URL valid for a few minutes. */
    suspend fun downloadUrl(image: ChatImage): Result<String, DataError.Network>
}

data class ChatMember(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String?,
    /** `false` for someone who has left the group; they stay known so the history keeps their name. */
    val isMember: Boolean = true,
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

/** What the conversation screen shows in its top bar, and the group screen about the group. */
data class ChatHeader(
    /** The other person's name in a direct chat, the group's title in a group. */
    val title: String,
    val initials: String,
    val isGroup: Boolean = false,
    /** Everyone known on the chat, current members first. */
    val members: List<ChatMember> = emptyList(),
    /** Groups: the user created it (or got the rights when the creator left) and may rename it and add people. */
    val canManage: Boolean = false,
    /** Groups: who holds the creator's rights. */
    val createdBy: String? = null,
) {
    val memberCount: Int
        get() = members.count { it.isMember }
}

object MessageRules {
    const val MAX_LENGTH = 4000
}
