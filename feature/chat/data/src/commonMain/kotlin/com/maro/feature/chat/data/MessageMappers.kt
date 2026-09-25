package com.maro.feature.chat.data

import com.maro.core.database.message.MessageEntity
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageStatus

internal fun MessageEntity.toDomain(currentUserId: String?): Message = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    text = text,
    createdAt = createdAt,
    status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.FAILED),
    isOutgoing = senderId == currentUserId,
)

internal fun RemoteMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    senderId = senderId,
    text = text,
    createdAt = createdAt,
    status = MessageStatus.SENT.name,
)

internal fun MessageEntity.toRemote(): RemoteMessage = RemoteMessage(
    id = id,
    chatId = chatId,
    senderId = senderId,
    text = text,
    createdAt = createdAt,
)
