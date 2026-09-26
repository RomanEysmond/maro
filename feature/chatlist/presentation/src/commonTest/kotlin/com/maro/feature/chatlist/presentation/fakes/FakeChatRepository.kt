package com.maro.feature.chatlist.presentation.fakes

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeChatRepository : ChatRepository {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    override val chats: StateFlow<List<Chat>> = _chats

    var syncResult: EmptyResult<DataError.Network> = Result.Success(Unit)
    /** What `sync()` writes into [chats] on success, simulating the background upsert into Room. */
    var chatsAfterSync: List<Chat>? = null
    var syncCalls = 0

    /** Simulates the background listener writing into Room. */
    fun setChats(chats: List<Chat>) {
        _chats.value = chats
    }

    override suspend fun sync(): EmptyResult<DataError.Network> {
        syncCalls++
        chatsAfterSync?.let { _chats.value = it }
        return syncResult
    }
}
