package com.maro.feature.chat.data

import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.chat.ChatMemberEntity
import com.maro.core.database.message.MessageEntity
import com.maro.core.database.message.MessageSyncEntity
import com.maro.core.database.message.MessageWithReceipts
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.ChatMember
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.domain.SystemEvent
import com.maro.feature.chat.domain.SystemEventKind

private const val MICROS_PER_MILLI = 1_000L

internal fun MessageEntity.toDomain(currentUserId: String?): Message = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    text = text,
    createdAt = createdAt,
    status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.FAILED),
    isOutgoing = senderId == currentUserId,
    systemEvent = if (type == MessageEntity.TYPE_SYSTEM) toSystemEvent() else null,
)

private fun MessageEntity.toSystemEvent(): SystemEvent? {
    val kind = when (eventKind) {
        "created" -> SystemEventKind.CREATED
        "added" -> SystemEventKind.ADDED
        "left" -> SystemEventKind.LEFT
        "renamed" -> SystemEventKind.RENAMED
        else -> return null
    }
    return SystemEvent(
        kind = kind,
        targetIds = eventTargets?.split(',')?.filter { it.isNotEmpty() }.orEmpty(),
        title = eventTitle,
    )
}

/**
 * An outgoing message the server has is SENT until the other side's marks pass it: delivered first, then read
 * (a read message is delivered too). Marks and message times are both server timestamps.
 */
internal fun MessageWithReceipts.toDomain(currentUserId: String?): Message {
    val base = message.toDomain(currentUserId)
    if (!base.isOutgoing || base.status != MessageStatus.SENT) return base
    val sentAt = message.createdAt
    val status = when {
        (peerReadAt ?: Long.MIN_VALUE) >= sentAt -> MessageStatus.READ
        (peerDeliveredAt ?: Long.MIN_VALUE) >= sentAt -> MessageStatus.DELIVERED
        else -> MessageStatus.SENT
    }
    return base.copy(status = status)
}

internal fun RemoteMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    senderId = senderId,
    text = text,
    createdAt = createdAtMicros / MICROS_PER_MILLI,
    status = MessageStatus.SENT.name,
    type = if (systemEvent != null) MessageEntity.TYPE_SYSTEM else MessageEntity.TYPE_TEXT,
    eventKind = systemEvent?.kind,
    eventTargets = systemEvent?.targets?.joinToString(","),
    eventTitle = systemEvent?.title,
)

internal fun MessageEntity.toRemote(): RemoteMessage = RemoteMessage(
    id = id,
    chatId = chatId,
    senderId = senderId,
    text = text,
    createdAtMicros = createdAt * MICROS_PER_MILLI,
)

internal val MessageSyncEntity.newest: MessageCursor?
    get() = cursorOf(newestAtMicros, newestId)

internal val MessageSyncEntity.oldest: MessageCursor?
    get() = cursorOf(oldestAtMicros, oldestId)

private fun cursorOf(createdAtMicros: Long?, id: String?): MessageCursor? =
    if (createdAtMicros != null && id != null) MessageCursor(createdAtMicros, id) else null

/** Epoch millis of the newest synced message, comparable with the chat's `lastMessageAt`. */
internal val MessageSyncEntity.newestAtMillis: Long?
    get() = newestAtMicros?.div(MICROS_PER_MILLI)

/**
 * The cached range grown by [page]. The page has to border on the range (right after it, right before it, or
 * the first page of a chat), which is what keeps the range free of gaps. Never shrinks: pages that arrive late
 * or overlap are harmless.
 */
internal fun MessageSyncEntity?.extendedWith(
    chatId: String,
    page: List<RemoteMessage>,
    reachedStart: Boolean = false,
): MessageSyncEntity {
    val cursors = page.map { it.cursor }
    val newest = listOfNotNull(this?.newest, cursors.maxOrNull()).maxOrNull()
    val oldest = listOfNotNull(this?.oldest, cursors.minOrNull()).minOrNull()
    return MessageSyncEntity(
        chatId = chatId,
        newestAtMicros = newest?.createdAtMicros,
        newestId = newest?.id,
        oldestAtMicros = oldest?.createdAtMicros,
        oldestId = oldest?.id,
        reachedStart = this?.reachedStart == true || reachedStart,
    )
}

internal fun ChatMemberEntity.toDomain(): ChatMember = ChatMember(
    id = userId,
    firstName = firstName,
    lastName = lastName,
    username = username,
    isMember = isMember,
)

internal fun ChatEntity.toHeader(members: List<ChatMember>, currentUserId: String?): ChatHeader {
    if (type == "group") {
        val title = title.orEmpty()
        return ChatHeader(
            title = title,
            initials = initialsOf(title.split(' ')),
            isGroup = true,
            members = members,
            canManage = createdBy != null && createdBy == currentUserId,
            createdBy = createdBy,
        )
    }
    val name = listOf(otherUserFirstName, otherUserLastName).filter { it.isNotBlank() }
    return ChatHeader(title = name.joinToString(" "), initials = initialsOf(name), members = members)
}

/** The first letters of the first two words: "Иван Иванов" -> "ИИ", "Поход в горы" -> "ПВ". */
private fun initialsOf(words: List<String>): String = words.filter { it.isNotBlank() }.take(2)
    .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
    .joinToString("")
    .ifEmpty { "?" }
