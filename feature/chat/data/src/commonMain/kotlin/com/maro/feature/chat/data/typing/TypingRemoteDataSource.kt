package com.maro.feature.chat.data.typing

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

/** One participant's typing state as the server has it. */
data class RemoteTyping(
    val userId: String,
    val isTyping: Boolean,
    /** Epoch millis of the server timestamp of the last change. */
    val atMillis: Long,
)

interface TypingRemoteDataSource {
    /** Every participant's last typing state, live. Failures are swallowed: typing is best effort. */
    fun observeTyping(chatId: String): Flow<List<RemoteTyping>>

    suspend fun setTyping(chatId: String, userId: String, isTyping: Boolean): EmptyResult<DataError.Network>
}
