package com.maro.feature.chatlist.data

import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.chat.ChatMemberEntity
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
    otherUserId = otherParticipant?.id.orEmpty(),
    otherUserFirstName = otherParticipant?.firstName.orEmpty(),
    otherUserLastName = otherParticipant?.lastName.orEmpty(),
    otherUserUsername = otherParticipant?.username,
    lastMessageText = lastMessage?.text,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageAt = lastMessage?.sentAt,
    updatedAt = updatedAt,
    myReadAt = myReadAt,
    peerReadAt = peerReadAt,
    peerDeliveredAt = peerDeliveredAt,
    title = title,
    createdBy = createdBy,
)

fun Chat.toMemberEntities(): List<ChatMemberEntity> = participants.map { participant ->
    ChatMemberEntity(
        chatId = id,
        userId = participant.id,
        firstName = participant.firstName,
        lastName = participant.lastName,
        username = participant.username,
        isMember = participant.isMember,
    )
}

fun ChatWithUnread.toDomain(currentUserId: String?): Chat {
    val base = chat.toDomain()
    val lastMessage = base.lastMessage?.let { message ->
        if (message.senderId != currentUserId) {
            return@let if (base.type == ChatType.GROUP) message.copy(senderName = lastMessageSenderFirstName) else message
        }
        val receipt = when {
            (chat.peerReadAt ?: Long.MIN_VALUE) >= message.sentAt -> LastMessageReceipt.READ
            (chat.peerDeliveredAt ?: Long.MIN_VALUE) >= message.sentAt -> LastMessageReceipt.DELIVERED
            else -> LastMessageReceipt.SENT
        }
        message.copy(receipt = receipt)
    }
    return base.copy(lastMessage = lastMessage, unreadCount = unreadCount)
}

fun ChatEntity.toDomain(): Chat {
    val isGroup = type == "group"
    return Chat(
        id = id,
        type = if (isGroup) ChatType.GROUP else ChatType.DIRECT,
        otherParticipant = if (isGroup) {
            null
        } else {
            ChatParticipant(
                id = otherUserId,
                firstName = otherUserFirstName,
                lastName = otherUserLastName,
                username = otherUserUsername,
            )
        },
        title = title,
        createdBy = createdBy,
        lastMessage = lastMessageText?.let { text ->
            LastMessage(text = text, senderId = lastMessageSenderId.orEmpty(), sentAt = lastMessageAt ?: 0L)
        },
        updatedAt = updatedAt,
        myReadAt = myReadAt,
        peerReadAt = peerReadAt,
        peerDeliveredAt = peerDeliveredAt,
    )
}
