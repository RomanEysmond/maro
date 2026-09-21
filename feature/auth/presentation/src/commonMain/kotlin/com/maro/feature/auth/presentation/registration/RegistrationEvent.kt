package com.maro.feature.auth.presentation.registration

sealed interface RegistrationEvent {
    data object NavigateBack : RegistrationEvent
    data object NavigateNext : RegistrationEvent
}
