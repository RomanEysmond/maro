package com.maro.feature.chatlist.data

import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.chat.ChatWithUnread
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatParticipant
import com.maro.feature.chatlist.domain.ChatType
import com.maro.feature.chatlist.domain.LastMessage
import com.maro.feature.chatlist.domain.LastMessageReceipt

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
    myReadAt = myReadAt,
    peerReadAt = peerReadAt,
    peerDeliveredAt = peerDeliveredAt,
)

fun ChatWithUnread.toDomain(currentUserId: String?): Chat {
    val base = chat.toDomain()
    val lastMessage = base.lastMessage?.let { message ->
        if (message.senderId != currentUserId) return@let message
        val receipt = when {
            (chat.peerReadAt ?: Long.MIN_VALUE) >= message.sentAt -> LastMessageReceipt.READ
            (chat.peerDeliveredAt ?: Long.MIN_VALUE) >= message.sentAt -> LastMessageReceipt.DELIVERED
            else -> LastMessageReceipt.SENT
        }
        message.copy(receipt = receipt)
    }
    return base.copy(lastMessage = lastMessage, unreadCount = unreadCount)
}

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
    myReadAt = myReadAt,
    peerReadAt = peerReadAt,
    peerDeliveredAt = peerDeliveredAt,
)
