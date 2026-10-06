package com.maro.feature.chatlist.data

import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.chat.GroupRules
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.profile.UserProfileRepository
import com.maro.core.domain.user.UserDirectory
import com.maro.core.domain.user.UserSearchError
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.domain.NewChatError
import com.maro.feature.chatlist.domain.NewChatRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
private fun randomId(): String = Uuid.random().toString()

class DefaultNewChatRepository(
    private val remote: NewChatRemoteDataSource,
    private val users: UserDirectory,
    private val currentUser: CurrentUserProvider,
    private val profiles: UserProfileRepository,
    private val newId: () -> String = ::randomId,
) : NewChatRepository {

    override suspend fun findUser(username: String): Result<FoundUser, NewChatError> =
        when (val result = users.findByUsername(username)) {
            is Result.Error -> Result.Error(result.error.toNewChatError())

            is Result.Success -> {
                val user = result.data
                if (user.id ==
                    currentUser.userId
                ) {
                    Result.Error(NewChatError.CANNOT_CHAT_WITH_SELF)
                } else {
                    Result.Success(user)
                }
            }
        }

    override suspend fun startChat(user: FoundUser): Result<String, NewChatError> {
        val me = ownProfile() ?: return Result.Error(NewChatError.UNKNOWN)
        if (user.id == me.id) return Result.Error(NewChatError.CANNOT_CHAT_WITH_SELF)

        // Ordered by id, so both sides derive the very same document id and never create two chats.
        val participants = listOf(me.toCard(), user.toCard()).sortedBy { it.id }
        val chatId = participants.joinToString("_") { it.id }

        return when (val result = remote.createChatIfAbsent(chatId, participants)) {
            is Result.Error -> Result.Error(result.error.toNewChatError())
            is Result.Success -> Result.Success(chatId)
        }
    }

    override suspend fun createGroup(title: String, members: List<FoundUser>): Result<String, NewChatError> {
        if (!GroupRules.isValidTitle(title)) return Result.Error(NewChatError.INVALID_GROUP_TITLE)
        val me = ownProfile() ?: return Result.Error(NewChatError.UNKNOWN)
        val others = members.filter { it.id != me.id }.distinctBy { it.id }
        if (others.isEmpty()) return Result.Error(NewChatError.NO_GROUP_MEMBERS)
        if (others.size + 1 > GroupRules.MAX_MEMBERS) return Result.Error(NewChatError.TOO_MANY_MEMBERS)

        // A random id: unlike a direct chat, the same people may well have several groups.
        val chatId = newId()
        val result = remote.createGroup(
            chatId = chatId,
            title = title.trim(),
            creatorId = me.id,
            participants = listOf(me.toCard()) + others.map { it.toCard() },
            systemMessageId = newId(),
        )
        return when (result) {
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

    private fun UserProfile.toCard() = ParticipantCard(id, firstName, lastName, username)

    private fun FoundUser.toCard() = ParticipantCard(id, firstName, lastName, username)

    private fun UserSearchError.toNewChatError(): NewChatError = when (this) {
        UserSearchError.INVALID_USERNAME -> NewChatError.INVALID_USERNAME
        UserSearchError.USER_NOT_FOUND -> NewChatError.USER_NOT_FOUND
        UserSearchError.NO_INTERNET -> NewChatError.NO_INTERNET
        UserSearchError.UNKNOWN -> NewChatError.UNKNOWN
    }

    private fun DataError.Network.toNewChatError(): NewChatError = when (this) {
        DataError.Network.NO_INTERNET, DataError.Network.REQUEST_TIMEOUT -> NewChatError.NO_INTERNET
        else -> NewChatError.UNKNOWN
    }
}
