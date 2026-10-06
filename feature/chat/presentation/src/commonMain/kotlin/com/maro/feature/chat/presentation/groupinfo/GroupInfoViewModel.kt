package com.maro.feature.chat.presentation.groupinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.chat.GroupRules
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.domain.user.UserDirectory
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.GroupRepository
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupInfoViewModel(
    private val chatId: String,
    messages: MessageRepository,
    private val groups: GroupRepository,
    private val users: UserDirectory,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupInfoState())
    val state = _state.asStateFlow()

    private val _events = Channel<GroupInfoEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Room, as everywhere: a change made here comes back through the chat list's listener and shows up by itself.
        messages.chatHeader(chatId)
            .onEach { header -> _state.update { it.copy(header = header) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: GroupInfoAction) {
        when (action) {
            GroupInfoAction.OnBackClick -> viewModelScope.launch { _events.send(GroupInfoEvent.NavigateBack) }

            GroupInfoAction.OnRenameClick -> _state.update {
                it.copy(renameDraft = it.header?.title.orEmpty(), error = null)
            }

            is GroupInfoAction.OnRenameChange -> _state.update {
                it.copy(renameDraft = action.value.take(GroupRules.MAX_TITLE_LENGTH))
            }

            GroupInfoAction.OnRenameDismiss -> _state.update { it.copy(renameDraft = null) }

            GroupInfoAction.OnRenameConfirm -> rename()

            is GroupInfoAction.OnAddQueryChange -> _state.update {
                it.copy(addQuery = sanitize(action.value), error = null)
            }

            GroupInfoAction.OnAddClick -> addMember()

            GroupInfoAction.OnLeaveClick -> _state.update { it.copy(isLeaveConfirmVisible = true) }

            GroupInfoAction.OnLeaveDismiss -> _state.update { it.copy(isLeaveConfirmVisible = false) }

            GroupInfoAction.OnLeaveConfirm -> leave()
        }
    }

    private fun rename() {
        val draft = _state.value.renameDraft ?: return
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = groups.rename(chatId, draft)) {
                is Result.Success -> _state.update { it.copy(isBusy = false, renameDraft = null) }
                is Result.Error -> _state.update { it.copy(isBusy = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun addMember() {
        val current = _state.value
        if (!current.canAdd) return
        _state.update { it.copy(isAdding = true, error = null) }
        viewModelScope.launch {
            val user = when (val found = users.findByUsername(current.addQuery)) {
                is Result.Error -> {
                    _state.update { it.copy(isAdding = false, error = found.error.toUiText()) }
                    return@launch
                }

                is Result.Success -> found.data
            }
            when (val result = groups.addMember(chatId, user)) {
                is Result.Success -> _state.update { it.copy(isAdding = false, addQuery = "") }
                is Result.Error -> _state.update { it.copy(isAdding = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun leave() {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, isLeaveConfirmVisible = false, error = null) }
        viewModelScope.launch {
            when (val result = groups.leave(chatId)) {
                is Result.Success -> {
                    _state.update { it.copy(isBusy = false) }
                    _events.send(GroupInfoEvent.LeftGroup)
                }

                is Result.Error -> _state.update { it.copy(isBusy = false, error = result.error.toUiText()) }
            }
        }
    }

    /** Lets only what a username may contain through (an "@" typed by habit is dropped). */
    private fun sanitize(raw: String): String = ProfileRules.normalizeUsername(raw)
        .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
        .take(ProfileRules.MAX_USERNAME_LENGTH)
}
