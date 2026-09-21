package com.maro.feature.profile.presentation.edit

import com.maro.core.domain.profile.BirthDate
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.presentation.util.UiText

enum class EditProfileMode {
    /** Right after the first sign-in: only the optional fields, and the screen can be skipped. */
    SETUP,

    /** Opened from the profile: every field, with the current values filled in. */
    EDIT,
}

data class EditProfileState(
    val mode: EditProfileMode,
    val firstName: String = "",
    val lastName: String = "",
    /** What the user typed: lowercase latin letters, digits and "_", without the "@". */
    val username: String = "",
    val bio: String = "",
    val birthDate: BirthDate? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val usernameError: UiText? = null,
) {
    val isUsernameValid: Boolean
        get() = username.isEmpty() || ProfileRules.isValidUsername(username)

    val canSave: Boolean
        get() = !isLoading && !isSaving && firstName.isNotBlank() && isUsernameValid
}
