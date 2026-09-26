package com.maro.feature.chatlist.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.profile.EnsuredProfile
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.domain.NewChatError
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class DefaultNewChatRepositoryTest {

    private class FakeRemote : NewChatRemoteDataSource {
        var users: Map<String, FoundUser> = emptyMap()
        var findError: DataError.Network? = null
        var createResult: EmptyResult<DataError.Network> = Result.Success(Unit)
        var lastLookup: String? = null
        var created: Pair<String, List<ParticipantCard>>? = null

        override suspend fun findUser(username: String): Result<FoundUser?, DataError.Network> {
            lastLookup = username
            return findError?.let { Result.Error(it) } ?: Result.Success(users[username])
        }

        override suspend fun createChatIfAbsent(
            chatId: String,
            participants: List<ParticipantCard>,
        ): EmptyResult<DataError.Network> {
            created = chatId to participants
            return createResult
        }
    }

    private class FakeProfiles(initial: UserProfile?, private val loadable: UserProfile? = null) : UserProfileRepository {
        private val _profile = MutableStateFlow(initial)
        override val profile: StateFlow<UserProfile?> = _profile
        var refreshCalls = 0

        override suspend fun ensureProfile(firstName: String, lastName: String, phone: String):
            Result<EnsuredProfile, DataError.Network> = Result.Error(DataError.Network.UNKNOWN)

        override suspend fun refreshProfile(): EmptyResult<DataError.Network> {
            refreshCalls++
            loadable?.let { _profile.value = it }
            return Result.Success(Unit)
        }

        override suspend fun updateProfile(update: ProfileUpdate): EmptyResult<ProfileError> = Result.Success(Unit)
    }

    private val me = UserProfile("uid-m", "Иван", "Иванов", "+79001234567", username = "ivan_i")
    private val anna = FoundUser("uid-a", "Анна", "Петрова", "anna_p")
    private val remote = FakeRemote().apply { users = mapOf("anna_p" to anna, "ivan_i" to FoundUser("uid-m", "Иван", "Иванов", "ivan_i")) }
    private val currentUser = object : CurrentUserProvider {
        override val userId: String = "uid-m"
    }

    private fun repository(profiles: FakeProfiles = FakeProfiles(me)) = DefaultNewChatRepository(remote, currentUser, profiles)

    @Test
    fun `a username typed with an at sign and capitals is found`() = runTest {
        val result = repository().findUser(" @Anna_P ")

        assertThat(result).isEqualTo(Result.Success(anna))
        assertThat(remote.lastLookup).isEqualTo("anna_p")
    }

    @Test
    fun `a malformed username is rejected without asking the server`() = runTest {
        val result = repository().findUser("ab")

        assertThat(result).isEqualTo(Result.Error(NewChatError.INVALID_USERNAME))
        assertThat(remote.lastLookup).isNull()
    }

    @Test
    fun `an unknown username is reported as not found`() = runTest {
        assertThat(repository().findUser("nobody_here")).isEqualTo(Result.Error(NewChatError.USER_NOT_FOUND))
    }

    @Test
    fun `finding yourself is refused`() = runTest {
        assertThat(repository().findUser("ivan_i")).isEqualTo(Result.Error(NewChatError.CANNOT_CHAT_WITH_SELF))
    }

    @Test
    fun `no network is told apart from other failures`() = runTest {
        remote.findError = DataError.Network.NO_INTERNET
        assertThat(repository().findUser("anna_p")).isEqualTo(Result.Error(NewChatError.NO_INTERNET))

        remote.findError = DataError.Network.FORBIDDEN
        assertThat(repository().findUser("anna_p")).isEqualTo(Result.Error(NewChatError.UNKNOWN))
    }

    @Test
    fun `the chat id and the participants are ordered by user id`() = runTest {
        val result = repository().startChat(anna)

        assertThat(result).isEqualTo(Result.Success("uid-a_uid-m"))
        val (chatId, participants) = remote.created!!
        assertThat(chatId).isEqualTo("uid-a_uid-m")
        assertThat(participants.map { it.id }).isEqualTo(listOf("uid-a", "uid-m"))
        assertThat(participants.last().username).isEqualTo("ivan_i")
    }

    @Test
    fun `starting a chat works from a cold start by loading the own profile first`() = runTest {
        val profiles = FakeProfiles(initial = null, loadable = me)

        val result = repository(profiles).startChat(anna)

        assertThat(result).isEqualTo(Result.Success("uid-a_uid-m"))
        assertThat(profiles.refreshCalls).isEqualTo(1)
    }

    @Test
    fun `starting a chat fails when the own profile cannot be loaded`() = runTest {
        val result = repository(FakeProfiles(initial = null, loadable = null)).startChat(anna)

        assertThat(result).isEqualTo(Result.Error(NewChatError.UNKNOWN))
        assertThat(remote.created).isNull()
    }

    @Test
    fun `a failure to create the chat is reported`() = runTest {
        remote.createResult = Result.Error(DataError.Network.NO_INTERNET)

        assertThat(repository().startChat(anna)).isEqualTo(Result.Error(NewChatError.NO_INTERNET))
    }
}
