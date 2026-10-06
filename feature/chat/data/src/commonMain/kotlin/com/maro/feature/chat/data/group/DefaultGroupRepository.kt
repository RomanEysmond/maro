package com.maro.feature.chat.data.group

import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.chat.GroupRules
import com.maro.core.domain.user.UserCard
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.GroupError
import com.maro.feature.chat.domain.GroupRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
private fun randomId(): String = Uuid.random().toString()

class DefaultGroupRepository(
    private val remote: GroupRemoteDataSource,
    private val currentUser: CurrentUserProvider,
    private val newId: () -> String = ::randomId,
) : GroupRepository {

    override suspend fun rename(chatId: String, title: String): EmptyResult<GroupError> {
        if (!GroupRules.isValidTitle(title)) return Result.Error(GroupError.INVALID_TITLE)
        val userId = currentUser.userId ?: return Result.Error(GroupError.NOT_ALLOWED)
        return remote.rename(chatId, title.trim(), userId, newId()).toGroupResult()
    }

    override suspend fun addMember(chatId: String, user: UserCard): EmptyResult<GroupError> {
        val userId = currentUser.userId ?: return Result.Error(GroupError.NOT_ALLOWED)
        if (user.id == userId) return Result.Error(GroupError.ALREADY_MEMBER)
        val card = MemberCard(user.id, user.firstName, user.lastName, user.username)
        return remote.addMember(chatId, card, userId, newId()).toGroupResult()
    }

    override suspend fun leave(chatId: String): EmptyResult<GroupError> {
        val userId = currentUser.userId ?: return Result.Error(GroupError.NOT_ALLOWED)
        return remote.leave(chatId, userId, newId()).toGroupResult()
    }

    private fun EmptyResult<DataError.Network>.toGroupResult(): EmptyResult<GroupError> = when (this) {
        is Result.Success -> this

        is Result.Error -> Result.Error(
            when (error) {
                DataError.Network.FORBIDDEN -> GroupError.NOT_ALLOWED
                DataError.Network.CONFLICT -> GroupError.ALREADY_MEMBER
                DataError.Network.PAYLOAD_TOO_LARGE -> GroupError.TOO_MANY_MEMBERS
                DataError.Network.NO_INTERNET, DataError.Network.REQUEST_TIMEOUT -> GroupError.NO_INTERNET
                else -> GroupError.UNKNOWN
            },
        )
    }
}
