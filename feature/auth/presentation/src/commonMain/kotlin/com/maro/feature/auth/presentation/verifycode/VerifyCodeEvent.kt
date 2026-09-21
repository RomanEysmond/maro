package com.maro.feature.auth.presentation.verifycode

sealed interface VerifyCodeEvent {
    data object NavigateBack : VerifyCodeEvent

    /** The code is correct and the profile is ready. */
    data class Authenticated(val isNewUser: Boolean) : VerifyCodeEvent
}
