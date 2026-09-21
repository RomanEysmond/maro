package com.maro.feature.auth.presentation.registration

sealed interface RegistrationEvent {
    data object NavigateBack : RegistrationEvent

    /** The SMS was sent: the user continues on the code-entry screen. */
    data class NavigateToVerifyCode(
        val firstName: String,
        val lastName: String,
        val phone: String,
    ) : RegistrationEvent

    /** The platform verified the number without an SMS code and the profile is ready. */
    data class Authenticated(val isNewUser: Boolean) : RegistrationEvent
}
