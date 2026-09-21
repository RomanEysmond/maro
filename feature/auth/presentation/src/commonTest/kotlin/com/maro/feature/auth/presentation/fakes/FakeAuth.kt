package com.maro.feature.auth.presentation.fakes

import com.maro.core.domain.profile.EnsuredProfile
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.auth.domain.AuthError
import com.maro.feature.auth.domain.PhoneAuthenticator
import com.maro.feature.auth.domain.SendCodeOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePhoneAuthenticator : PhoneAuthenticator {
    var sendResult: Result<SendCodeOutcome, AuthError> = Result.Success(SendCodeOutcome.CodeSent)
    var resendResult: Result<SendCodeOutcome, AuthError> = Result.Success(SendCodeOutcome.CodeSent)
    var verifyResult: EmptyResult<AuthError> = Result.Success(Unit)

    val sentTo = mutableListOf<String>()
    val resentTo = mutableListOf<String>()
    val verifiedCodes = mutableListOf<String>()

    override suspend fun sendCode(phone: String): Result<SendCodeOutcome, AuthError> {
        sentTo += phone
        return sendResult
    }

    override suspend fun resendCode(phone: String): Result<SendCodeOutcome, AuthError> {
        resentTo += phone
        return resendResult
    }

    override suspend fun verifyCode(code: String): EmptyResult<AuthError> {
        verifiedCodes += code
        return verifyResult
    }
}

class FakeUserProfileRepository : UserProfileRepository {
    var result: Result<EnsuredProfile, DataError.Network>? = null
    var isNew = true
    var calls = 0

    private val _profile = MutableStateFlow<UserProfile?>(null)
    override val profile: StateFlow<UserProfile?> = _profile

    override suspend fun ensureProfile(
        firstName: String,
        lastName: String,
        phone: String,
    ): Result<EnsuredProfile, DataError.Network> {
        calls++
        return result ?: Result.Success(EnsuredProfile(UserProfile("uid", firstName, lastName, phone), isNew))
    }

    override suspend fun refreshProfile(): EmptyResult<DataError.Network> = Result.Success(Unit)

    override suspend fun updateProfile(update: ProfileUpdate): EmptyResult<ProfileError> = Result.Success(Unit)
}
