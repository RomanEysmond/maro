package com.maro.feature.chatlist.domain

import com.maro.core.domain.user.UserCard
import com.maro.core.domain.util.Error
import com.maro.core.domain.util.Result

/** Someone found by @username: everything needed to open a conversation with them. */
typealias FoundUser = UserCard

enum class NewChatError : Error {
    INVALID_USERNAME,
    USER_NOT_FOUND,
    CANNOT_CHAT_WITH_SELF,
    INVALID_GROUP_TITLE,
    NO_GROUP_MEMBERS,
    TOO_MANY_MEMBERS,
    NO_INTERNET,
    UNKNOWN,
}

interface NewChatRepository {
    /** [username] may be typed with or without "@" and in any case. */
    suspend fun findUser(username: String): Result<FoundUser, NewChatError>

    /**
     * Makes sure the conversation with [user] exists and returns its id. Idempotent: the id is derived from the
     * two user ids, so asking twice (or both sides at once) never creates a second chat.
     */
    suspend fun startChat(user: FoundUser): Result<String, NewChatError>

    /** Creates a group of the user and [members] (the user is its creator) and returns its id. */
    suspend fun createGroup(title: String, members: List<FoundUser>): Result<String, NewChatError>
}
