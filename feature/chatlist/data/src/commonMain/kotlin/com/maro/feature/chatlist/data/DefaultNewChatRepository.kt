package com.maro.feature.chatlist.data

import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.domain.NewChatError
import com.maro.feature.chatlist.domain.NewChatRepository

class DefaultNewChatRepository(
    private val remote: NewChatRemoteDataSource,
    private val currentUser: CurrentUserProvider,
    private val profiles: UserProfileRepository,
) : NewChatRepository {

    override suspend fun findUser(username: String): Result<FoundUser, NewChatError> {
        val normalized = ProfileRules.normalizeUsername(username)
        if (!ProfileRules.isValidUsername(normalized)) return Result.Error(NewChatError.INVALID_USERNAME)

        return when (val result = remote.findUser(normalized)) {
            is Result.Error -> Result.Error(result.error.toNewChatError())
            is Result.Success -> {
                val user = result.data ?: return Result.Error(NewChatError.USER_NOT_FOUND)
                if (user.id == currentUser.userId) Result.Error(NewChatError.CANNOT_CHAT_WITH_SELF) else Result.Success(user)
            }
        }
    }

    override suspend fun startChat(user: FoundUser): Result<String, NewChatError> {
        val me = ownProfile() ?: return Result.Error(NewChatError.UNKNOWN)
        if (user.id == me.id) return Result.Error(NewChatError.CANNOT_CHAT_WITH_SELF)

        // Ordered by id, so both sides derive the very same document id and never create two chats.
        val participants = listOf(
            ParticipantCard(me.id, me.firstName, me.lastName, me.username),
            ParticipantCard(user.id, user.firstName, user.lastName, user.username),
        ).sortedBy { it.id }
        val chatId = participants.joinToString("_") { it.id }

        return when (val result = remote.createChatIfAbsent(chatId, participants)) {
            is Result.Error -> Result.Error(result.error.toNewChatError())
            is Result.Success -> Result.Success(chatId)
        }
    }

    private suspend fun ownProfile(): UserProfile? {
        profiles.profile.value?.let { return it }
        // Cold start: the profile is only loaded once somebody asks for it.
        profiles.refreshProfile()
        return profiles.profile.value
    }

    private fun DataError.Network.toNewChatError(): NewChatError = when (this) {
        DataError.Network.NO_INTERNET, DataError.Network.REQUEST_TIMEOUT -> NewChatError.NO_INTERNET
        else -> NewChatError.UNKNOWN
    }
}
