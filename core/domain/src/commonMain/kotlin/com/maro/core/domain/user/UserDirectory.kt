package com.maro.core.domain.user

import com.maro.core.domain.util.Error
import com.maro.core.domain.util.Result

/** What anyone may know about a user: their public card (`usernames/{name}`), nothing from the private profile. */
data class UserCard(val id: String, val firstName: String, val lastName: String, val username: String) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

enum class UserSearchError : Error {
    INVALID_USERNAME,
    USER_NOT_FOUND,
    NO_INTERNET,
    UNKNOWN,
}

/** Finding people by @username: shared by "new chat", "new group" and "add a member". */
interface UserDirectory {
    /** [username] may be typed with or without "@" and in any case. */
    suspend fun findByUsername(username: String): Result<UserCard, UserSearchError>
}
