package com.maro.feature.chatlist.data

import com.maro.core.database.chat.ChatEntity
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatParticipant
import com.maro.feature.chatlist.domain.ChatType
import com.maro.feature.chatlist.domain.LastMessage

fun Chat.toEntity(): ChatEntity = ChatEntity(
    id = id,
    type = when (type) {
        ChatType.DIRECT -> "direct"
        ChatType.GROUP -> "group"
    },
    otherUserId = otherParticipant.id,
    otherUserFirstName = otherParticipant.firstName,
    otherUserLastName = otherParticipant.lastName,
    otherUserUsername = otherParticipant.username,
    lastMessageText = lastMessage?.text,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageAt = lastMessage?.sentAt,
    updatedAt = updatedAt,
)

fun ChatEntity.toDomain(): Chat = Chat(
    id = id,
    type = if (type == "group") ChatType.GROUP else ChatType.DIRECT,
    otherParticipant = ChatParticipant(
        id = otherUserId,
        firstName = otherUserFirstName,
        lastName = otherUserLastName,
        username = otherUserUsername,
    ),
    lastMessage = lastMessageText?.let { text ->
        LastMessage(text = text, senderId = lastMessageSenderId.orEmpty(), sentAt = lastMessageAt ?: 0L)
    },
    updatedAt = updatedAt,
)
