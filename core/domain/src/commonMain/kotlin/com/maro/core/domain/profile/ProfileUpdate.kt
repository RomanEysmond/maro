package com.maro.core.domain.profile

import com.maro.core.domain.util.Error

/** New values for the editable part of a profile (the phone number is not editable). */
data class ProfileUpdate(
    val firstName: String,
    val lastName: String,
    val username: String?,
    val bio: String,
    val birthDate: BirthDate?,
)

enum class ProfileError : Error {
    USERNAME_TAKEN,
    NO_INTERNET,
    UNKNOWN,
}

/** Validation rules that both the UI and the backend rules (`firestore.rules`) agree on. */
object ProfileRules {
    const val MAX_NAME_LENGTH = 64
    const val MAX_BIO_LENGTH = 140
    const val MIN_USERNAME_LENGTH = 5
    const val MAX_USERNAME_LENGTH = 32

    private val usernameRegex = Regex("^[a-z][a-z0-9_]{${MIN_USERNAME_LENGTH - 1},${MAX_USERNAME_LENGTH - 1}}$")

    /** Expects an already normalized (trimmed, lowercase) username. */
    fun isValidUsername(username: String): Boolean = usernameRegex.matches(username)

    /** Trims, drops a leading "@" and lowercases what the user typed. */
    fun normalizeUsername(raw: String): String = raw.trim().removePrefix("@").lowercase()
}
