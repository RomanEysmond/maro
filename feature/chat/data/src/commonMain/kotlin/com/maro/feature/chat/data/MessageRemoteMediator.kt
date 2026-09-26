package com.maro.feature.chat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.maro.core.database.message.MessageEntity
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.LoadMessagesException

/**
 * Pages Room is out of: the list is newest first, so APPEND means older history. Newer messages are not its
 * job — [MessageSynchronizer] catches up and listens while the chat is open — so REFRESH and PREPEND do nothing.
 */
@OptIn(ExperimentalPagingApi::class)
internal class MessageRemoteMediator(
    private val chatId: String,
    private val synchronizer: MessageSynchronizer,
) : RemoteMediator<Int, MessageEntity>() {

    override suspend fun initialize(): InitializeAction = InitializeAction.SKIP_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MessageEntity>): MediatorResult =
        when (loadType) {
            LoadType.REFRESH -> MediatorResult.Success(endOfPaginationReached = false)
            LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> when (val result = synchronizer.loadOlder(chatId)) {
                is Result.Success -> MediatorResult.Success(endOfPaginationReached = result.data)
                is Result.Error -> MediatorResult.Error(LoadMessagesException(result.error))
            }
        }
}
