package com.maro.feature.auth.presentation.verifycode

import com.maro.core.presentation.util.UiText

data class VerifyCodeState(
    /** The number the SMS was sent to, in international format. */
    val phoneNumber: String,
    val code: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val resendSecondsLeft: Int = RESEND_DELAY_SECONDS,
) {
    val isConfirmEnabled: Boolean
        get() = code.length == CODE_LENGTH && !isLoading

    val canResend: Boolean
        get() = resendSecondsLeft == 0 && !isLoading

    companion object {
        const val CODE_LENGTH = 6
        const val RESEND_DELAY_SECONDS = 60
    }
}
