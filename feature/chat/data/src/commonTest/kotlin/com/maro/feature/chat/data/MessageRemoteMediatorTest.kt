package com.maro.feature.chat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.maro.core.database.message.MessageEntity
import com.maro.core.domain.util.DataError
import com.maro.feature.chat.domain.LoadMessagesException
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediatorTest {

    private val dao = FakeMessageDao()
    private val remote = FakeMessageRemoteDataSource()
    private val synchronizer = MessageSynchronizer(dao, FakeChatDao(), remote, FakeConnectivityObserver())
    private val mediator = MessageRemoteMediator("chat", synchronizer)

    private val pagingState = PagingState<Int, MessageEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = MessageSynchronizer.PAGE_SIZE),
        leadingPlaceholderCount = 0,
    )

    private suspend fun load(loadType: LoadType) = mediator.load(loadType, pagingState)

    private fun MediatorResult.reachedEnd(): Boolean = (this as MediatorResult.Success).endOfPaginationReached

    @Test
    fun `refresh and newer pages are left to the sync and touch nothing`() = runTest {
        assertThat(load(LoadType.REFRESH).reachedEnd()).isEqualTo(false)
        assertThat(load(LoadType.PREPEND).reachedEnd()).isEqualTo(true)
        assertThat(remote.reads).isEmpty()
    }

    @Test
    fun `append loads older history until the start of the chat`() = runTest {
        remote.messages.value = (1..60L).map { serverMessage("m$it", it) }
        synchronizer.catchUp("chat")

        assertThat(load(LoadType.APPEND).reachedEnd()).isEqualTo(true)
        assertThat(dao.rows.value.size).isEqualTo(60)
    }

    @Test
    fun `a failed page is an error the list can show and retry`() = runTest {
        remote.messages.value = (1..60L).map { serverMessage("m$it", it) }
        synchronizer.catchUp("chat")
        remote.readError = DataError.Network.NO_INTERNET

        val result = load(LoadType.APPEND)

        assertThat(result).isInstanceOf<MediatorResult.Error>()
            .prop(MediatorResult.Error::throwable)
            .isInstanceOf<LoadMessagesException>()
            .prop(LoadMessagesException::error)
            .isEqualTo(DataError.Network.NO_INTERNET)
    }
}
