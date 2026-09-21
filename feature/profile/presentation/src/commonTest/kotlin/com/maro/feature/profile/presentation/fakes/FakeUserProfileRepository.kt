package com.maro.feature.profile.presentation.fakes

import com.maro.core.domain.profile.EnsuredProfile
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUserProfileRepository(initial: UserProfile? = null) : UserProfileRepository {
    private val _profile = MutableStateFlow(initial)
    override val profile: StateFlow<UserProfile?> = _profile

    var refreshResult: EmptyResult<DataError.Network> = Result.Success(Unit)
    var profileAfterRefresh: UserProfile? = null
    var updateResult: EmptyResult<ProfileError> = Result.Success(Unit)

    var refreshCalls = 0
    val updates = mutableListOf<ProfileUpdate>()

    override suspend fun ensureProfile(
        firstName: String,
        lastName: String,
        phone: String,
    ): Result<EnsuredProfile, DataError.Network> = Result.Error(DataError.Network.UNKNOWN)

    override suspend fun refreshProfile(): EmptyResult<DataError.Network> {
        refreshCalls++
        profileAfterRefresh?.let { _profile.value = it }
        return refreshResult
    }

    override suspend fun updateProfile(update: ProfileUpdate): EmptyResult<ProfileError> {
        updates += update
        if (updateResult is Result.Success) _profile.value = _profile.value?.withUpdate(update)
        return updateResult
    }
}
