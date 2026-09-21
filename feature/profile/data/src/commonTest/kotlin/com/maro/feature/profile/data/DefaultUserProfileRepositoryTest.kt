package com.maro.feature.profile.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.maro.core.domain.profile.BirthDate
import com.maro.core.domain.profile.EnsuredProfile
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DefaultUserProfileRepositoryTest {

    private class FakeRemote(
        var existing: Result<UserProfile?, DataError.Network> = Result.Success(null),
        var updateResult: EmptyResult<ProfileError> = Result.Success(Unit),
    ) : UserProfileRemoteDataSource {
        var createCalls = 0

        override suspend fun fetchCurrent() = existing

        override suspend fun createCurrent(
            firstName: String,
            lastName: String,
            phone: String,
        ): Result<UserProfile, DataError.Network> {
            createCalls++
            return Result.Success(UserProfile("uid", firstName, lastName, phone))
        }

        override suspend fun updateCurrent(update: ProfileUpdate) = updateResult
    }

    private val stored = UserProfile("uid", "Пётр", "Петров", "+79001234567", username = "petr_p")

    @Test
    fun `existing profile wins over the values from the form and is not new`() = runTest {
        val remote = FakeRemote(existing = Result.Success(stored))
        val repository = DefaultUserProfileRepository(remote)

        val result = repository.ensureProfile("Иван", "Иванов", "+79001234567")

        assertThat(result).isEqualTo(Result.Success(EnsuredProfile(stored, isNew = false)))
        assertThat(remote.createCalls).isEqualTo(0)
        assertThat(repository.profile.value).isEqualTo(stored)
    }

    @Test
    fun `missing profile is created from the form and is new`() = runTest {
        val remote = FakeRemote()
        val repository = DefaultUserProfileRepository(remote)

        val result = repository.ensureProfile("Иван", "Иванов", "+79001234567")

        val created = UserProfile("uid", "Иван", "Иванов", "+79001234567")
        assertThat(result).isEqualTo(Result.Success(EnsuredProfile(created, isNew = true)))
        assertThat(remote.createCalls).isEqualTo(1)
        assertThat(repository.profile.value).isEqualTo(created)
    }

    @Test
    fun `fetch failure is returned and nothing is created`() = runTest {
        val remote = FakeRemote(existing = Result.Error(DataError.Network.NO_INTERNET))
        val repository = DefaultUserProfileRepository(remote)

        val result = repository.ensureProfile("Иван", "Иванов", "+79001234567")

        assertThat(result).isInstanceOf<Result.Error<DataError.Network>>()
        assertThat(remote.createCalls).isEqualTo(0)
        assertThat(repository.profile.value).isNull()
    }

    @Test
    fun `successful update is applied to the cached profile`() = runTest {
        val repository = DefaultUserProfileRepository(FakeRemote(existing = Result.Success(stored)))
        repository.refreshProfile()
        val update = ProfileUpdate("Пётр", "Сидоров", "petr_new", "Привет", BirthDate(1990, 5, 7))

        val result = repository.updateProfile(update)

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(repository.profile.value).isEqualTo(stored.withUpdate(update))
    }

    @Test
    fun `failed update keeps the cached profile`() = runTest {
        val remote = FakeRemote(existing = Result.Success(stored))
        val repository = DefaultUserProfileRepository(remote)
        repository.refreshProfile()
        remote.updateResult = Result.Error(ProfileError.USERNAME_TAKEN)

        val result = repository.updateProfile(ProfileUpdate("Пётр", "Петров", "taken_name", "", null))

        assertThat(result).isEqualTo(Result.Error(ProfileError.USERNAME_TAKEN))
        assertThat(repository.profile.value).isEqualTo(stored)
    }

    @Test
    fun `refresh of a missing profile reports not found`() = runTest {
        val repository = DefaultUserProfileRepository(FakeRemote())

        val result = repository.refreshProfile()

        assertThat(result).isEqualTo(Result.Error(DataError.Network.NOT_FOUND))
    }
}
