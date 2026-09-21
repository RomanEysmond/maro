package com.maro.feature.auth.presentation.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.toUiText
import com.maro.feature.auth.domain.PhoneAuthenticator
import com.maro.feature.auth.domain.SendCodeOutcome
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.feature.auth.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val phoneAuthenticator: PhoneAuthenticator,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrationState())
    val state = _state.asStateFlow()

    private val _events = Channel<RegistrationEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.OnFirstNameChange -> _state.update { it.copy(firstName = action.value, error = null) }
            is RegistrationAction.OnLastNameChange -> _state.update { it.copy(lastName = action.value, error = null) }
            is RegistrationAction.OnPhoneNumberChange -> {
                _state.update { it.copy(phoneNumber = normalizePhoneInput(action.value), error = null) }
            }
            RegistrationAction.OnBackClick -> send(RegistrationEvent.NavigateBack)
            RegistrationAction.OnContinueClick -> requestCode()
        }
    }

    private fun requestCode() {
        val current = _state.value
        if (!current.isContinueEnabled || current.isLoading) return

        val firstName = current.firstName.trim()
        val lastName = current.lastName.trim()
        val phone = current.fullPhoneNumber

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = phoneAuthenticator.sendCode(phone)) {
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
                is Result.Success -> when (result.data) {
                    SendCodeOutcome.CodeSent -> {
                        _state.update { it.copy(isLoading = false) }
                        _events.send(RegistrationEvent.NavigateToVerifyCode(firstName, lastName, phone))
                    }
                    SendCodeOutcome.AutoVerified -> completeProfile(firstName, lastName, phone)
                }
            }
        }
    }

    private suspend fun completeProfile(firstName: String, lastName: String, phone: String) {
        when (val result = userProfileRepository.ensureProfile(firstName, lastName, phone)) {
            is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            is Result.Success -> {
                _state.update { it.copy(isLoading = false) }
                _events.send(RegistrationEvent.Authenticated(result.data.isNew))
            }
        }
    }

    private fun send(event: RegistrationEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    /**
     * Keeps only digits and at most [RegistrationState.PHONE_LENGTH] of them. A pasted number with a
     * country code ("+7…" or "8…", 11 digits) loses that leading code, so it fits after the fixed "+7" prefix.
     */
    private fun normalizePhoneInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        val national = if (digits.length > RegistrationState.PHONE_LENGTH && digits.first() in COUNTRY_CODE_DIGITS) {
            digits.drop(1)
        } else {
            digits
        }
        return national.take(RegistrationState.PHONE_LENGTH)
    }

    private companion object {
        val COUNTRY_CODE_DIGITS = setOf('7', '8')
    }
}
