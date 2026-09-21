package com.maro.feature.auth.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.maro.core.data.await
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.auth.domain.AuthError
import com.maro.feature.auth.domain.PhoneAuthenticator
import com.maro.feature.auth.domain.SendCodeOutcome
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal class FirebasePhoneAuthenticator(
    private val auth: FirebaseAuth,
    private val activityProvider: ActivityProvider,
) : PhoneAuthenticator {

    // Lives as long as the process. After process death the code screen reports CODE_EXPIRED and offers a resend.
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var tokenPhone: String? = null

    override suspend fun sendCode(phone: String): Result<SendCodeOutcome, AuthError> =
        requestCode(phone, forceResendingToken = null)

    override suspend fun resendCode(phone: String): Result<SendCodeOutcome, AuthError> =
        requestCode(phone, forceResendingToken = resendToken.takeIf { tokenPhone == phone })

    override suspend fun verifyCode(code: String): EmptyResult<AuthError> {
        val id = verificationId ?: return Result.Error(AuthError.CODE_EXPIRED)
        return try {
            auth.signInWithCredential(PhoneAuthProvider.getCredential(id, code)).await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toAuthError())
        }
    }

    private suspend fun requestCode(
        phone: String,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken?,
    ): Result<SendCodeOutcome, AuthError> {
        val activity = activityProvider.current ?: return Result.Error(AuthError.UNKNOWN)

        return suspendCancellableCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Instant verification. If the code was already sent (SMS auto-retrieval arriving later),
                    // the continuation is done and the user simply enters the code.
                    if (!continuation.isActive) return
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (!continuation.isActive) return@addOnCompleteListener
                        continuation.resume(
                            if (task.isSuccessful) {
                                Result.Success(SendCodeOutcome.AutoVerified)
                            } else {
                                Result.Error(task.exception.toAuthError())
                            },
                        )
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (continuation.isActive) continuation.resume(Result.Error(e.toAuthError()))
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken = token
                    tokenPhone = phone
                    if (continuation.isActive) continuation.resume(Result.Success(SendCodeOutcome.CodeSent))
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .apply { forceResendingToken?.let(::setForceResendingToken) }
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    private companion object {
        const val VERIFICATION_TIMEOUT_SECONDS = 60L
    }
}

private fun Throwable?.toAuthError(): AuthError = when (this) {
    is FirebaseNetworkException -> AuthError.NETWORK
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
    is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        "ERROR_INVALID_PHONE_NUMBER" -> AuthError.INVALID_PHONE
        "ERROR_SESSION_EXPIRED" -> AuthError.CODE_EXPIRED
        else -> AuthError.INVALID_CODE
    }
    is FirebaseAuthException -> when (errorCode) {
        "ERROR_QUOTA_EXCEEDED" -> AuthError.TOO_MANY_REQUESTS
        "ERROR_SESSION_EXPIRED" -> AuthError.CODE_EXPIRED
        else -> AuthError.UNKNOWN
    }
    else -> AuthError.UNKNOWN
}
