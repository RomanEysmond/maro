package com.maro.feature.chatlist.presentation.newgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.chat.GroupRules
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.SavedDraft
import com.maro.core.presentation.util.UiText
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.domain.NewChatRepository
import com.maro.feature.chatlist.presentation.generated.resources.Res
import com.maro.feature.chatlist.presentation.generated.resources.new_group_already_added
import com.maro.feature.chatlist.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class SavedMember(val id: String, val firstName: String, val lastName: String, val username: String)

/** The group being put together, kept across process death. */
@Serializable
private data class NewGroupDraft(val title: String, val query: String, val members: List<SavedMember>)

class NewGroupViewModel(private val repository: NewChatRepository, savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _state = MutableStateFlow(NewGroupState())
    val state = _state.asStateFlow()

    private val draft = SavedDraft(savedStateHandle, NewGroupDraft.serializer())

    init {
        draft.restore()?.let { saved ->
            _state.update { state ->
                state.copy(
                    title = saved.title,
                    query = saved.query,
                    members = saved.members.map { FoundUser(it.id, it.firstName, it.lastName, it.username) },
                )
            }
        }
        _state.map { state ->
            NewGroupDraft(
                title = state.title,
                query = state.query,
                members = state.members.map { SavedMember(it.id, it.firstName, it.lastName, it.username) },
            )
        }
            .distinctUntilChanged()
            .onEach(draft::save)
            .launchIn(viewModelScope)
    }

    private val _events = Channel<NewGroupEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: NewGroupAction) {
        when (action) {
            is NewGroupAction.OnTitleChange -> _state.update {
                it.copy(title = action.value.take(GroupRules.MAX_TITLE_LENGTH), error = null)
            }

            is NewGroupAction.OnQueryChange -> _state.update { it.copy(query = sanitize(action.value), error = null) }

            NewGroupAction.OnAddClick -> addMember()

            is NewGroupAction.OnRemoveClick -> _state.update { state ->
                state.copy(members = state.members.filter { it.id != action.userId })
            }

            NewGroupAction.OnCreateClick -> create()

            NewGroupAction.OnBackClick -> viewModelScope.launch { _events.send(NewGroupEvent.NavigateBack) }
        }
    }

    private fun addMember() {
        val current = _state.value
        if (!current.canAdd) return

        _state.update { it.copy(isSearching = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.findUser(current.query)) {
                is Result.Error -> _state.update { it.copy(isSearching = false, error = result.error.toUiText()) }

                is Result.Success -> _state.update { state ->
                    if (state.members.any { it.id == result.data.id }) {
                        state.copy(isSearching = false, error = UiText.Resource(Res.string.new_group_already_added))
                    } else {
                        // Ready for the next name right away.
                        state.copy(isSearching = false, query = "", members = state.members + result.data)
                    }
                }
            }
        }
    }

    private fun create() {
        val current = _state.value
        if (!current.canCreate) return

        _state.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.createGroup(current.title, current.members)) {
                is Result.Success -> {
                    _state.update { it.copy(isCreating = false) }
                    _events.send(NewGroupEvent.NavigateToChat(result.data))
                }

                is Result.Error -> _state.update { it.copy(isCreating = false, error = result.error.toUiText()) }
            }
        }
    }

    /** Lets only what a username may contain through (an "@" typed by habit is dropped). */
    private fun sanitize(raw: String): String = ProfileRules.normalizeUsername(raw)
        .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
        .take(ProfileRules.MAX_USERNAME_LENGTH)
}
