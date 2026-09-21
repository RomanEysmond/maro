package com.maro.feature.profile.data

import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result

/** Profile of the currently signed-in user in the backend. */
interface UserProfileRemoteDataSource {
    /** `null` in the success case means the user has no profile yet. */
    suspend fun fetchCurrent(): Result<UserProfile?, DataError.Network>

    suspend fun createCurrent(
        firstName: String,
        lastName: String,
        phone: String,
    ): Result<UserProfile, DataError.Network>

    /** Also claims the new username (and releases the old one); fails with USERNAME_TAKEN if it is not free. */
    suspend fun updateCurrent(update: ProfileUpdate): EmptyResult<ProfileError>
}
