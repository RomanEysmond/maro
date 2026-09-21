package com.maro.feature.auth.presentation.verifycode

sealed interface VerifyCodeAction {
    data class OnCodeChange(val value: String) : VerifyCodeAction
    data object OnConfirmClick : VerifyCodeAction
    data object OnResendClick : VerifyCodeAction
    data object OnBackClick : VerifyCodeAction
}
