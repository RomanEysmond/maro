package com.maro.feature.chatlist.data

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.Chat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeChatRemoteDataSource : ChatRemoteDataSource {
    var fetchResult: Result<List<Chat>, DataError.Network> = Result.Success(emptyList())
    var fetchCalls = 0

    private val updates = MutableSharedFlow<Result<List<Chat>, DataError.Network>>(replay = 1)

    override suspend fun fetchChats(): Result<List<Chat>, DataError.Network> {
        fetchCalls++
        return fetchResult
    }

    override fun observeChats(): Flow<Result<List<Chat>, DataError.Network>> = updates

    suspend fun emit(result: Result<List<Chat>, DataError.Network>) = updates.emit(result)
}
