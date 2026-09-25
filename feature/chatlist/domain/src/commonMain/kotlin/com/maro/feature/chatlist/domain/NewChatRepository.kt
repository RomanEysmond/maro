package com.maro.feature.chatlist.domain

import com.maro.core.domain.util.Error
import com.maro.core.domain.util.Result

/** Someone found by @username: everything needed to open a conversation with them. */
data class FoundUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String,
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

enum class NewChatError : Error {
    INVALID_USERNAME,
    USER_NOT_FOUND,
    CANNOT_CHAT_WITH_SELF,
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
}
