package com.maro.feature.chatlist.presentation.fakes

import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.domain.NewChatError
import com.maro.feature.chatlist.domain.NewChatRepository

class FakeNewChatRepository : NewChatRepository {
    var findResult: Result<FoundUser, NewChatError> = Result.Error(NewChatError.USER_NOT_FOUND)

    /** Overrides [findResult] per username when set. */
    var users: Map<String, FoundUser> = emptyMap()
    var startResult: Result<String, NewChatError> = Result.Success("chat-1")
    var createResult: Result<String, NewChatError> = Result.Success("group-1")
    val lookups = mutableListOf<String>()
    var started: FoundUser? = null
    var createdGroup: Pair<String, List<FoundUser>>? = null

    override suspend fun findUser(username: String): Result<FoundUser, NewChatError> {
        lookups += username
        return users[username]?.let { Result.Success(it) } ?: findResult
    }

    override suspend fun startChat(user: FoundUser): Result<String, NewChatError> {
        started = user
        return startResult
    }

    override suspend fun createGroup(title: String, members: List<FoundUser>): Result<String, NewChatError> {
        createdGroup = title to members
        return createResult
    }
}
