package com.maro.feature.profile.presentation.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.profile.BirthDate
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.SavedDraft
import com.maro.core.presentation.util.toUiText
import com.maro.feature.profile.presentation.util.toUiText
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

/** The fields of the form as edited, kept across process death. [birthDate] is "yyyy-MM-dd". */
@Serializable
private data class EditProfileDraft(
    val firstName: String,
    val lastName: String,
    val username: String,
    val bio: String,
    val birthDate: String?,
)

class EditProfileViewModel(
    private val mode: EditProfileMode,
    private val repository: UserProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState(mode = mode))
    val state = _state.asStateFlow()

    private val draft = SavedDraft(savedStateHandle, EditProfileDraft.serializer())

    private val _events = Channel<EditProfileEvent>()
    val events = _events.receiveAsFlow()

    init {
        val restored = draft.restore()
        val known = repository.profile.value
        if (restored != null) {
            // Back after process death: the user's unsaved edits win over the stored profile.
            _state.update {
                it.copy(
                    firstName = restored.firstName,
                    lastName = restored.lastName,
                    username = restored.username,
                    bio = restored.bio,
                    birthDate = restored.birthDate?.let(BirthDate::parse),
                )
            }
        } else if (known != null) {
            fill(known)
        } else {
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                when (val result = repository.refreshProfile()) {
                    is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                    is Result.Success -> {
                        repository.profile.value?.let(::fill)
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    init {
        // Only once the form shows the profile: an empty form still loading is not a draft.
        _state.map { state ->
            state.takeUnless { it.isLoading }?.let {
                EditProfileDraft(it.firstName, it.lastName, it.username, it.bio, it.birthDate?.toIsoString())
            }
        }
            .distinctUntilChanged()
            .onEach { it?.let(draft::save) }
            .launchIn(viewModelScope)
    }

    fun onAction(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.OnFirstNameChange -> _state.update {
                it.copy(firstName = action.value.take(ProfileRules.MAX_NAME_LENGTH), error = null)
            }
            is EditProfileAction.OnLastNameChange -> _state.update {
                it.copy(lastName = action.value.take(ProfileRules.MAX_NAME_LENGTH), error = null)
            }
            is EditProfileAction.OnUsernameChange -> _state.update {
                it.copy(username = sanitizeUsername(action.value), usernameError = null, error = null)
            }
            is EditProfileAction.OnBioChange -> _state.update {
                it.copy(bio = action.value.take(ProfileRules.MAX_BIO_LENGTH), error = null)
            }
            is EditProfileAction.OnBirthDateChange -> _state.update { it.copy(birthDate = action.value, error = null) }
            EditProfileAction.OnSaveClick -> save()
            EditProfileAction.OnSkipClick, EditProfileAction.OnBackClick -> close()
        }
    }

    private fun fill(profile: UserProfile) {
        _state.update {
            it.copy(
                firstName = profile.firstName,
                lastName = profile.lastName,
                username = profile.username.orEmpty(),
                bio = profile.bio,
                birthDate = profile.birthDate,
            )
        }
    }

    private fun save() {
        val current = _state.value
        if (!current.canSave) return

        val update = ProfileUpdate(
            firstName = current.firstName.trim(),
            lastName = current.lastName.trim(),
            username = current.username.ifEmpty { null },
            bio = current.bio.trim(),
            birthDate = current.birthDate,
        )
        _state.update { it.copy(isSaving = true, error = null, usernameError = null) }
        viewModelScope.launch {
            when (val result = repository.updateProfile(update)) {
                is Result.Success -> _events.send(EditProfileEvent.Close)
                is Result.Error -> _state.update {
                    if (result.error == ProfileError.USERNAME_TAKEN) {
                        it.copy(isSaving = false, usernameError = result.error.toUiText())
                    } else {
                        it.copy(isSaving = false, error = result.error.toUiText())
                    }
                }
            }
        }
    }

    private fun close() {
        viewModelScope.launch { _events.send(EditProfileEvent.Close) }
    }

    /** Lets only what a username may contain through, so the field can never hold an invalid character. */
    private fun sanitizeUsername(raw: String): String =
        ProfileRules.normalizeUsername(raw)
            .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
            .take(ProfileRules.MAX_USERNAME_LENGTH)
}
