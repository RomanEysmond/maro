package com.maro.feature.auth.presentation.registration

sealed interface RegistrationAction {
    data class OnFirstNameChange(val value: String) : RegistrationAction
    data class OnLastNameChange(val value: String) : RegistrationAction
    data class OnPhoneNumberChange(val value: String) : RegistrationAction
    data object OnContinueClick : RegistrationAction
    data object OnBackClick : RegistrationAction
}
