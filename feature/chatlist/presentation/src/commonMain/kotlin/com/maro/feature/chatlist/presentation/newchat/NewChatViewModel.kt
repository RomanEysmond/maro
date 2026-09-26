package com.maro.feature.chatlist.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.NewChatRepository
import com.maro.feature.chatlist.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewChatViewModel(
    private val repository: NewChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NewChatState())
    val state = _state.asStateFlow()

    private val _events = Channel<NewChatEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: NewChatAction) {
        when (action) {
            is NewChatAction.OnQueryChange -> _state.update {
                it.copy(query = sanitize(action.value), foundUser = null, error = null)
            }
            NewChatAction.OnSearchClick -> search()
            NewChatAction.OnUserClick -> startChat()
            NewChatAction.OnBackClick -> viewModelScope.launch { _events.send(NewChatEvent.NavigateBack) }
        }
    }

    private fun search() {
        val current = _state.value
        if (!current.canSearch) return

        _state.update { it.copy(isSearching = true, foundUser = null, error = null) }
        viewModelScope.launch {
            when (val result = repository.findUser(current.query)) {
                is Result.Success -> _state.update { it.copy(isSearching = false, foundUser = result.data) }
                is Result.Error -> _state.update { it.copy(isSearching = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun startChat() {
        val user = _state.value.foundUser ?: return
        if (_state.value.isStarting) return

        _state.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.startChat(user)) {
                is Result.Success -> {
                    _state.update { it.copy(isStarting = false) }
                    _events.send(NewChatEvent.NavigateToChat(result.data))
                }
                is Result.Error -> _state.update { it.copy(isStarting = false, error = result.error.toUiText()) }
            }
        }
    }

    /** Lets only what a username may contain through (an "@" typed by habit is dropped). */
    private fun sanitize(raw: String): String =
        ProfileRules.normalizeUsername(raw)
            .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
            .take(ProfileRules.MAX_USERNAME_LENGTH)
}
