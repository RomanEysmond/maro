package com.maro.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.toUiText
import com.maro.feature.chat.domain.ChatSyncStatus
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.MessageRules
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val repository: MessageRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private val _events = Channel<ChatEvent>()
    val events = _events.receiveAsFlow()

    /** Newest first, straight from Room; older pages are fetched as the list is scrolled up. */
    val messages: Flow<PagingData<Message>> = repository.messages(chatId).cachedIn(viewModelScope)

    init {
        // The screen only ever reads Room; the server feeds Room through `syncMessages` below.
        repository.chatHeader(chatId)
            .onEach { header -> _state.update { it.copy(header = header) } }
            .launchIn(viewModelScope)
        // Runs until the screen is left; it only completes early after a permanent failure.
        repository.syncMessages(chatId)
            .onEach { status -> _state.update { it.with(status) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnInputChange -> {
                _state.update { it.copy(input = action.value.take(MessageRules.MAX_LENGTH)) }
            }
            ChatAction.OnSendClick -> send()
            is ChatAction.OnRetryClick -> viewModelScope.launch { repository.retryMessage(action.messageId) }
            ChatAction.OnBackClick -> viewModelScope.launch { _events.send(ChatEvent.NavigateBack) }
        }
    }

    private fun send() {
        val text = _state.value.input
        if (text.isBlank()) return
        // Cleared at once: the message is in Room (and on screen) before the network is even asked.
        _state.update { it.copy(input = "") }
        viewModelScope.launch {
            val result = repository.sendMessage(chatId, text)
            // Only reachable for input the UI already rules out; keep the text rather than lose it.
            if (result is Result.Error) _state.update { it.copy(input = text) }
        }
    }

    private fun ChatState.with(status: ChatSyncStatus): ChatState = when (status) {
        ChatSyncStatus.CatchingUp -> copy(connection = ChatConnection.UPDATING)
        ChatSyncStatus.WaitingForNetwork -> copy(connection = ChatConnection.WAITING_FOR_NETWORK)
        ChatSyncStatus.Live -> copy(connection = null, isCaughtUp = true)
        is ChatSyncStatus.Failed -> copy(connection = null, error = status.error.toUiText())
    }
}
