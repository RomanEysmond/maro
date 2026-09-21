package com.maro.feature.profile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState(profile = repository.profile.value))
    val state = _state.asStateFlow()

    private val _events = Channel<ProfileEvent>()
    val events = _events.receiveAsFlow()

    init {
        // The repository publishes every change (for example after editing), so the screen stays current.
        repository.profile
            .onEach { profile -> _state.update { it.copy(profile = profile) } }
            .launchIn(viewModelScope)
        refresh()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.OnEditClick -> viewModelScope.launch { _events.send(ProfileEvent.NavigateToEdit) }
            ProfileAction.OnRetryClick -> refresh()
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repository.refreshProfile()
            val error = (result as? Result.Error)?.error?.toUiText()
            // A stale profile is still worth showing; the error matters only when there is nothing to show.
            _state.update { current -> current.copy(isLoading = false, error = error.takeIf { current.profile == null }) }
        }
    }
}
