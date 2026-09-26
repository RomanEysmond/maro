package com.maro.feature.chatlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.toUiText
import com.maro.feature.chatlist.domain.ChatRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state = _state.asStateFlow()

    private val _events = Channel<ChatListEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Room is the source of truth: this keeps the screen current with whatever the repository has cached,
        // including updates the background listener writes after `sync()` below has already returned.
        repository.chats
            // Data arriving (from the background listener, after a failed first sync) makes an old error obsolete.
            .onEach { chats -> _state.update { it.copy(chats = chats, error = it.error.takeIf { chats.isEmpty() }) } }
            .launchIn(viewModelScope)
        sync()
    }

    fun onAction(action: ChatListAction) {
        when (action) {
            is ChatListAction.OnChatClick -> {
                viewModelScope.launch { _events.send(ChatListEvent.NavigateToChat(action.chatId)) }
            }
            ChatListAction.OnRetryClick -> sync()
        }
    }

    private fun sync() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repository.sync()
            val error = (result as? Result.Error)?.error?.toUiText()
            // Reads the repository's own current value, not `state.chats`: the `onEach` above updates state
            // from a separate coroutine, so state could still be stale at this exact point.
            _state.update { it.copy(isLoading = false, error = error.takeIf { repository.chats.value.isEmpty() }) }
        }
    }
}
