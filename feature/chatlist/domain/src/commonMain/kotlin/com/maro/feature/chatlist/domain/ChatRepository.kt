package com.maro.feature.chatlist.domain

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    /** Backed by Room; always current with whatever was last synced. `.value` is a safe synchronous read. */
    val chats: StateFlow<List<Chat>>

    /** One-shot catch-up fetch (called on screen start and on manual retry); upserts into Room on success. */
    suspend fun sync(): EmptyResult<DataError.Network>
}
