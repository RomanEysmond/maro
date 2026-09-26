package com.maro.feature.chatlist.data

import com.maro.core.database.chat.ChatDao
import com.maro.core.database.chat.ChatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeChatDao : ChatDao {
    private val _chats = MutableStateFlow<List<ChatEntity>>(emptyList())

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
