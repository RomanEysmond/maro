package com.maro.feature.auth.domain

import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result

/** What happened after a verification code was requested. */
enum class SendCodeOutcome {
    /** An SMS was sent; the user has to enter the code. */
    CodeSent,

    /** The platform verified the number on its own (instant verification); the user is already signed in. */
    AutoVerified,
}

/** Sign-in by phone number + SMS code. The backend implementation lives in `feature:auth:data`. */
interface PhoneAuthenticator {
    /** [phone] is in international format, e.g. "+79001234567". */
    suspend fun sendCode(phone: String): Result<SendCodeOutcome, AuthError>

    suspend fun resendCode(phone: String): Result<SendCodeOutcome, AuthError>

    /** Checks the SMS code of the last requested number and signs the user in. */
    suspend fun verifyCode(code: String): EmptyResult<AuthError>
}
