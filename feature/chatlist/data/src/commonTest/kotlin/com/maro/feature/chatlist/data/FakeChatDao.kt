package com.maro.feature.chatlist.data

import com.maro.core.database.chat.ChatDao
import com.maro.core.database.chat.ChatEntity
import com.maro.core.database.chat.ChatMemberEntity
import com.maro.core.database.chat.ChatWithUnread
import com.maro.core.database.message.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeChatDao : ChatDao {
    private val _chats = MutableStateFlow<List<ChatEntity>>(emptyList())
    private val CHATS get() = _chats

    /** Messages the unread counter is computed from (Room counts the `messages` table). */
    val messagesForUnread = MutableStateFlow<List<MessageEntity>>(emptyList())

    val members = MutableStateFlow<List<ChatMemberEntity>>(emptyList())

    override fun observeMembers(chatId: String): Flow<List<ChatMemberEntity>> =
        members.map { list -> list.filter { it.chatId == chatId } }

    override suspend fun getMembers(chatId: String): List<ChatMemberEntity> = members.value.filter { it.chatId == chatId }

    override suspend fun upsertMembers(members: List<ChatMemberEntity>) {
        val byKey = this.members.value.associateBy { it.chatId to it.userId }.toMutableMap()
        members.forEach { byKey[it.chatId to it.userId] = it }
        this.members.value = byKey.values.toList()
    }

    override suspend fun deleteMembersExcept(chats: List<String>) {
        members.value = members.value.filter { it.chatId in chats }
    }

    override fun observeAllWithUnread(userId: String): Flow<List<ChatWithUnread>> =
        combine(CHATS, messagesForUnread) { chats, messages ->
            chats.map { chat ->
                ChatWithUnread(
                    chat = chat,
                    unreadCount = messages.count {
                        it.chatId == chat.id && it.senderId != userId && it.createdAt > (chat.myReadAt ?: 0L)
                    },
                )
            }
        }

    override suspend fun getAll(): List<ChatEntity> = CHATS.value

    override suspend fun advanceMyReadAt(id: String, readAt: Long) {
        CHATS.value = CHATS.value.map { chat ->
            if (chat.id == id && (chat.myReadAt ?: Long.MIN_VALUE) < readAt) chat.copy(myReadAt = readAt) else chat
        }
    }

    override fun observeAll(): Flow<List<ChatEntity>> = _chats

    override fun observeById(id: String): Flow<ChatEntity?> = _chats.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun upsertAll(chats: List<ChatEntity>) {
        val byId = _chats.value.associateBy { it.id }.toMutableMap()
        chats.forEach { byId[it.id] = it }
        _chats.value = byId.values.sortedByDescending { it.updatedAt }
    }

    override suspend fun deleteExcept(chats: List<String>) {
        _chats.value = _chats.value.filter { it.id in chats }
    }
}
