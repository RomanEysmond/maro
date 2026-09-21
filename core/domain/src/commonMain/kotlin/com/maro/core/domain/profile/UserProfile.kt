package com.maro.core.domain.profile

/** The profile of a user. Shared by the auth feature (creates it) and the profile feature (shows and edits it). */
data class UserProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    /** International format, e.g. "+79001234567". */
    val phone: String,
    /** Unique handle without the "@", lowercase. `null` until the user picks one. */
    val username: String? = null,
    val bio: String = "",
    val birthDate: BirthDate? = null,
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    /** One or two capital letters for the avatar placeholder. */
    val initials: String
        get() = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }

    fun withUpdate(update: ProfileUpdate): UserProfile = copy(
        firstName = update.firstName,
        lastName = update.lastName,
        username = update.username,
        bio = update.bio,
        birthDate = update.birthDate,
    )
}
