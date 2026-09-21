package com.maro.core.domain.profile

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlinx.coroutines.flow.StateFlow

/** Result of [UserProfileRepository.ensureProfile]. */
data class EnsuredProfile(
    val profile: UserProfile,
    /** `true` when the profile was just created, i.e. the user has never signed in before. */
    val isNew: Boolean,
)

/** The profile of the signed-in user. */
interface UserProfileRepository {
    /** Last known profile of the current user; `null` until it is loaded. Updates after [updateProfile]. */
    val profile: StateFlow<UserProfile?>

    /**
     * Called right after sign-in. If the signed-in user already has a profile, it wins and the given
     * values are ignored; otherwise a profile is created from them.
     */
    suspend fun ensureProfile(
        firstName: String,
        lastName: String,
        phone: String,
    ): Result<EnsuredProfile, DataError.Network>

    /** Reloads [profile] from the backend. */
    suspend fun refreshProfile(): EmptyResult<DataError.Network>

    suspend fun updateProfile(update: ProfileUpdate): EmptyResult<ProfileError>
}
