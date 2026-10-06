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
import com.maro.core.domain.user.UserCard
import com.maro.core.domain.user.UserDirectory
import com.maro.core.domain.user.UserSearchError
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
        var createResult: EmptyResult<DataError.Network> = Result.Success(Unit)
        var created: Pair<String, List<ParticipantCard>>? = null
        var group: List<Any>? = null

        override suspend fun createChatIfAbsent(
            chatId: String,
            participants: List<ParticipantCard>,
        ): EmptyResult<DataError.Network> {
            created = chatId to participants
            return createResult
        }

        override suspend fun createGroup(
            chatId: String,
            title: String,
            creatorId: String,
            participants: List<ParticipantCard>,
            systemMessageId: String,
        ): EmptyResult<DataError.Network> {
            group = listOf(chatId, title, creatorId, participants.map { it.id }, systemMessageId)
            return createResult
        }
    }

    private class FakeDirectory : UserDirectory {
        var users: Map<String, UserCard> = emptyMap()
        var error: UserSearchError? = null
        var lastLookup: String? = null

        override suspend fun findByUsername(username: String): Result<UserCard, UserSearchError> {
            lastLookup = username
            error?.let { return Result.Error(it) }
            return users[username]?.let { Result.Success(it) } ?: Result.Error(UserSearchError.USER_NOT_FOUND)
        }
    }

    private class FakeProfiles(initial: UserProfile?, private val loadable: UserProfile? = null) :
        UserProfileRepository {
        private val _profile = MutableStateFlow(initial)
        override val profile: StateFlow<UserProfile?> = _profile
        var refreshCalls = 0

        override suspend fun ensureProfile(
            firstName: String,
            lastName: String,
            phone: String,
        ): Result<EnsuredProfile, DataError.Network> = Result.Error(DataError.Network.UNKNOWN)

        override suspend fun refreshProfile(): EmptyResult<DataError.Network> {
            refreshCalls++
            loadable?.let { _profile.value = it }
            return Result.Success(Unit)
        }

        override suspend fun updateProfile(update: ProfileUpdate): EmptyResult<ProfileError> = Result.Success(Unit)
    }

    private val me = UserProfile("uid-m", "Иван", "Иванов", "+79001234567", username = "ivan_i")
    private val anna = FoundUser("uid-a", "Анна", "Петрова", "anna_p")
    private val remote = FakeRemote()
    private val directory = FakeDirectory().apply {
        users = mapOf("anna_p" to anna, "ivan_i" to FoundUser("uid-m", "Иван", "Иванов", "ivan_i"))
    }
    private var nextId = 0
    private val currentUser = object : CurrentUserProvider {
        override val userId: String = "uid-m"
        override val userIdFlow = MutableStateFlow<String?>("uid-m")
    }

    private fun repository(profiles: FakeProfiles = FakeProfiles(me)) =
        DefaultNewChatRepository(remote, directory, currentUser, profiles, newId = { "id-${nextId++}" })

    @Test
    fun `people are looked up in the directory`() = runTest {
        assertThat(repository().findUser("anna_p")).isEqualTo(Result.Success(anna))
        assertThat(directory.lastLookup).isEqualTo("anna_p")
    }

    @Test
    fun `directory failures become new chat errors`() = runTest {
        assertThat(repository().findUser("nobody_here")).isEqualTo(Result.Error(NewChatError.USER_NOT_FOUND))

        directory.error = UserSearchError.INVALID_USERNAME
        assertThat(repository().findUser("ab")).isEqualTo(Result.Error(NewChatError.INVALID_USERNAME))

        directory.error = UserSearchError.NO_INTERNET
        assertThat(repository().findUser("anna_p")).isEqualTo(Result.Error(NewChatError.NO_INTERNET))
    }

    @Test
    fun `finding yourself is refused`() = runTest {
        assertThat(repository().findUser("ivan_i")).isEqualTo(Result.Error(NewChatError.CANNOT_CHAT_WITH_SELF))
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

    private val petr = FoundUser("uid-p", "Пётр", "", "petr_s")

    @Test
    fun `a group gets a random id, its title trimmed and the creator first`() = runTest {
        val result = repository().createGroup("  Поход  ", listOf(anna, petr, anna))

        assertThat(result).isEqualTo(Result.Success("id-0"))
        assertThat(remote.group).isEqualTo(listOf("id-0", "Поход", "uid-m", listOf("uid-m", "uid-a", "uid-p"), "id-1"))
    }

    @Test
    fun `a group needs a title and someone besides the creator`() = runTest {
        assertThat(
            repository().createGroup("   ", listOf(anna)),
        ).isEqualTo(Result.Error(NewChatError.INVALID_GROUP_TITLE))
        assertThat(
            repository().createGroup("Поход", emptyList()),
        ).isEqualTo(Result.Error(NewChatError.NO_GROUP_MEMBERS))
        // The creator in the list does not count as a member.
        val self = FoundUser("uid-m", "Иван", "Иванов", "ivan_i")
        assertThat(
            repository().createGroup("Поход", listOf(self)),
        ).isEqualTo(Result.Error(NewChatError.NO_GROUP_MEMBERS))
        assertThat(remote.group).isNull()
    }

    @Test
    fun `a group is limited to fifty people`() = runTest {
        val many = (1..50).map { FoundUser("uid-$it", "Имя$it", "", "user_$it") }

        assertThat(repository().createGroup("Много", many)).isEqualTo(Result.Error(NewChatError.TOO_MANY_MEMBERS))
    }
}
