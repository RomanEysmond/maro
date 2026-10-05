package com.maro.feature.chat.domain

import com.maro.core.domain.user.UserCard
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Error

enum class GroupError : Error {
    INVALID_TITLE,

    /** Only the group's creator may rename it or add people. */
    NOT_ALLOWED,
    ALREADY_MEMBER,
    TOO_MANY_MEMBERS,
    NO_INTERNET,
    UNKNOWN,
}

/**
 * Changes to a group. Each one is written together with its system message ("Anna added Peter"), so the
 * conversation always tells what happened; the chat itself then reaches Room through the chat list's listener.
 */
interface GroupRepository {
    suspend fun rename(chatId: String, title: String): EmptyResult<GroupError>

    suspend fun addMember(chatId: String, user: UserCard): EmptyResult<GroupError>

    /** If the user created the group, the creator's rights pass on to the earliest remaining member. */
    suspend fun leave(chatId: String): EmptyResult<GroupError>
}
