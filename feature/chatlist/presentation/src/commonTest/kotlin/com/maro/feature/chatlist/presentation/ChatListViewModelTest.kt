package com.maro.feature.chatlist.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatParticipant
import com.maro.feature.chatlist.domain.ChatType
import com.maro.feature.chatlist.presentation.fakes.FakeChatRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModelTest {

    private val chat = Chat(
        id = "chat-1",
        type = ChatType.DIRECT,
        otherParticipant = ChatParticipant("uid", "Иван", "Иванов", "ivan"),
        lastMessage = null,
        updatedAt = 0L,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `syncs on start and shows the fetched chats`() {
        val repository = FakeChatRepository().apply { chatsAfterSync = listOf(chat) }

        val viewModel = ChatListViewModel(repository)

        assertThat(repository.syncCalls).isEqualTo(1)
        assertThat(viewModel.state.value.chats).isEqualTo(listOf(chat))
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `sync failure with no cached chats is shown as an error`() {
        val repository = FakeChatRepository().apply { syncResult = Result.Error(DataError.Network.NO_INTERNET) }

        val viewModel = ChatListViewModel(repository)

        assertThat(viewModel.state.value.chats).isEqualTo(emptyList())
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `sync failure with chats already cached keeps them and hides the error`() {
        val repository = FakeChatRepository().apply {
            chatsAfterSync = listOf(chat)
            syncResult = Result.Error(DataError.Network.NO_INTERNET)
        }

        val viewModel = ChatListViewModel(repository)

        assertThat(viewModel.state.value.chats).isEqualTo(listOf(chat))
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `chats that arrive later clear the error of a failed first sync`() {
        val repository = FakeChatRepository().apply { syncResult = Result.Error(DataError.Network.NO_INTERNET) }
        val viewModel = ChatListViewModel(repository)
        assertThat(viewModel.state.value.error).isNotNull()

        repository.setChats(listOf(chat))

        assertThat(viewModel.state.value.chats).isEqualTo(listOf(chat))
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `retry calls sync again`() {
        val repository = FakeChatRepository()
        val viewModel = ChatListViewModel(repository)

        viewModel.onAction(ChatListAction.OnRetryClick)

        assertThat(repository.syncCalls).isEqualTo(2)
    }

    @Test
    fun `chat click emits NavigateToChat`() = runTest {
        val viewModel = ChatListViewModel(FakeChatRepository())

        viewModel.events.test {
            viewModel.onAction(ChatListAction.OnChatClick("chat-1"))

            assertThat(awaitItem()).isEqualTo(ChatListEvent.NavigateToChat("chat-1"))
        }
    }
}
