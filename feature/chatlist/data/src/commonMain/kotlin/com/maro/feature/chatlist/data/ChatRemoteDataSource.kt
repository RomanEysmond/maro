package com.maro.feature.chatlist.data

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.Chat
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    /** One-shot fetch of every chat the current user is a participant of. */
    suspend fun fetchChats(): Result<List<Chat>, DataError.Network>

    /**
     * Live updates of the same query. Each emission is the full current server-side list (not a delta),
     * which keeps Room's cache correct without a separate sync cursor — the chat list is small enough
     * that resending the whole thing is cheap. Failures are emitted, not thrown.
     */
    fun observeChats(): Flow<Result<List<Chat>, DataError.Network>>
}
