package com.maro.feature.auth.presentation.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrationViewModel : ViewModel() {

    private val _state = MutableStateFlow(RegistrationState())
    val state = _state.asStateFlow()

    private val _events = Channel<RegistrationEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.OnFirstNameChange -> _state.update { it.copy(firstName = action.value) }
            is RegistrationAction.OnLastNameChange -> _state.update { it.copy(lastName = action.value) }
            is RegistrationAction.OnPhoneNumberChange -> {
                _state.update { it.copy(phoneNumber = normalizePhoneInput(action.value)) }
            }
            RegistrationAction.OnBackClick -> send(RegistrationEvent.NavigateBack)
            RegistrationAction.OnContinueClick -> {
                if (_state.value.isContinueEnabled) {
                    // Stage 2 replaces this with sending the SMS code and opening the code-entry screen.
                    send(RegistrationEvent.NavigateNext)
                }
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
