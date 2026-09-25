package com.maro.feature.chat.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageRules
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.domain.SendError
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
class ChatViewModelTest {

    private lateinit var repository: FakeMessageRepository

    private val message = Message("m1", "chat", "me", "Привет", 1L, MessageStatus.SENT, isOutgoing = true)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeMessageRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChatViewModel("chat", repository)

    @Test
    fun `the header and the messages come from the repository and follow its updates`() {
        repository.headerFlow.value = ChatHeader("Иван Иванов", "ИИ")
        val viewModel = viewModel()

        assertThat(viewModel.state.value.header).isEqualTo(ChatHeader("Иван Иванов", "ИИ"))
        assertThat(viewModel.state.value.messages).isEqualTo(emptyList())

        repository.messagesFlow.value = listOf(message)
        assertThat(viewModel.state.value.messages).isEqualTo(listOf(message))
    }

    @Test
    fun `send is possible only with text`() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.canSend).isFalse()

        viewModel.onAction(ChatAction.OnInputChange("   "))
        assertThat(viewModel.state.value.canSend).isFalse()

        viewModel.onAction(ChatAction.OnInputChange("Привет"))
        assertThat(viewModel.state.value.canSend).isTrue()
    }

    @Test
    fun `sending clears the input at once and hands the text to the repository`() {
        val viewModel = viewModel()
        viewModel.onAction(ChatAction.OnInputChange("Привет"))

        viewModel.onAction(ChatAction.OnSendClick)

        assertThat(viewModel.state.value.input).isEqualTo("")
        assertThat(repository.sent).isEqualTo(listOf("Привет"))
    }

    @Test
    fun `sending blank text does nothing`() {
        val viewModel = viewModel()
        viewModel.onAction(ChatAction.OnInputChange("  "))

        viewModel.onAction(ChatAction.OnSendClick)

        assertThat(repository.sent).isEqualTo(emptyList())
    }

    @Test
    fun `the text comes back when the repository refuses it`() {
        repository.sendResult = Result.Error(SendError.NOT_SIGNED_IN)
        val viewModel = viewModel()
        viewModel.onAction(ChatAction.OnInputChange("Привет"))

        viewModel.onAction(ChatAction.OnSendClick)

        assertThat(viewModel.state.value.input).isEqualTo("Привет")
    }

    @Test
    fun `the input is capped at the message limit`() {
        val viewModel = viewModel()

        viewModel.onAction(ChatAction.OnInputChange("а".repeat(MessageRules.MAX_LENGTH + 50)))

        assertThat(viewModel.state.value.input.length).isEqualTo(MessageRules.MAX_LENGTH)
    }

    @Test
    fun `a permanent sync failure is shown`() {
        repository.syncResult = Result.Error(DataError.Network.FORBIDDEN)

        val viewModel = viewModel()

        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `no error while the sync is fine`() {
        assertThat(viewModel().state.value.error).isNull()
    }

    @Test
    fun `retry asks the repository to resend that message`() {
        val viewModel = viewModel()

        viewModel.onAction(ChatAction.OnRetryClick("m9"))

        assertThat(repository.retried).isEqualTo(listOf("m9"))
    }

    @Test
    fun `back emits NavigateBack`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ChatAction.OnBackClick)

            assertThat(awaitItem()).isEqualTo(ChatEvent.NavigateBack)
        }
    }
}
