package com.maro.feature.auth.presentation.verifycode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.toUiText
import com.maro.feature.auth.domain.PhoneAuthenticator
import com.maro.feature.auth.domain.SendCodeOutcome
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.feature.auth.presentation.VerifyCodeRoute
import com.maro.feature.auth.presentation.util.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyCodeViewModel(
    private val route: VerifyCodeRoute,
    private val phoneAuthenticator: PhoneAuthenticator,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VerifyCodeState(phoneNumber = route.phone))
    val state = _state.asStateFlow()

    private val _events = Channel<VerifyCodeEvent>()
    val events = _events.receiveAsFlow()

    // The code was accepted but saving the profile failed: "Confirm" then only retries the profile step.
    private var isCodeVerified = false
    private var resendTimerJob: Job? = null

    init {
        startResendTimer()
    }

    fun onAction(action: VerifyCodeAction) {
        when (action) {
            is VerifyCodeAction.OnCodeChange -> onCodeChange(action.value)
            VerifyCodeAction.OnConfirmClick -> confirm()
            VerifyCodeAction.OnResendClick -> resend()
            VerifyCodeAction.OnBackClick -> viewModelScope.launch { _events.send(VerifyCodeEvent.NavigateBack) }
        }
    }

    private fun onCodeChange(raw: String) {
        val code = raw.filter { it.isDigit() }.take(VerifyCodeState.CODE_LENGTH)
        _state.update { it.copy(code = code, error = null) }
        if (code.length == VerifyCodeState.CODE_LENGTH) confirm()
    }

    private fun confirm() {
        val current = _state.value
        if (!current.isConfirmEnabled) return

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            if (!isCodeVerified) {
                val result = phoneAuthenticator.verifyCode(current.code)
                if (result is Result.Error) {
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                    return@launch
                }
                isCodeVerified = true
            }
            completeProfile()
        }
    }

    private fun resend() {
        if (!_state.value.canResend) return

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = phoneAuthenticator.resendCode(route.phone)) {
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                is Result.Success -> when (result.data) {
                    SendCodeOutcome.CodeSent -> {
                        _state.update { it.copy(isLoading = false, code = "") }
                        startResendTimer()
                    }
                    SendCodeOutcome.AutoVerified -> {
                        isCodeVerified = true
                        completeProfile()
                    }
                }
            }
        }
    }

    private suspend fun completeProfile() {
        when (val result = userProfileRepository.ensureProfile(route.firstName, route.lastName, route.phone)) {
            is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            // isLoading stays true: the screen is about to be replaced.
            is Result.Success -> _events.send(VerifyCodeEvent.Authenticated(result.data.isNew))
        }
    }

    private fun startResendTimer() {
        resendTimerJob?.cancel()
        resendTimerJob = viewModelScope.launch {
            for (secondsLeft in VerifyCodeState.RESEND_DELAY_SECONDS downTo 0) {
                _state.update { it.copy(resendSecondsLeft = secondsLeft) }
                if (secondsLeft > 0) delay(1_000)
            }
        }
    }
}
