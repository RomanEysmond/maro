package com.maro.feature.profile.data

import com.maro.core.domain.profile.EnsuredProfile
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.core.domain.util.map
import com.maro.core.domain.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DefaultUserProfileRepository(
    private val remote: UserProfileRemoteDataSource,
) : UserProfileRepository {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    override val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    override suspend fun ensureProfile(
        firstName: String,
        lastName: String,
        phone: String,
    ): Result<EnsuredProfile, DataError.Network> {
        return when (val existing = remote.fetchCurrent()) {
            is Result.Error -> existing
            is Result.Success -> {
                val found = existing.data
                if (found != null) {
                    _profile.value = found
                    Result.Success(EnsuredProfile(found, isNew = false))
                } else {
                    remote.createCurrent(firstName, lastName, phone)
                        .onSuccess { _profile.value = it }
                        .map { EnsuredProfile(it, isNew = true) }
                }
            }
        }
    }

    override suspend fun refreshProfile(): EmptyResult<DataError.Network> {
        return when (val result = remote.fetchCurrent()) {
            is Result.Error -> result
            is Result.Success -> {
                val found = result.data ?: return Result.Error(DataError.Network.NOT_FOUND)
                _profile.value = found
                Result.Success(Unit)
            }
        }
    }

    override suspend fun updateProfile(update: ProfileUpdate): EmptyResult<ProfileError> {
        return remote.updateCurrent(update).onSuccess {
            _profile.update { it?.withUpdate(update) }
        }
    }
}
