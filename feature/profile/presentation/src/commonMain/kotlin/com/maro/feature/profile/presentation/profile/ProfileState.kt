package com.maro.feature.profile.presentation.profile

import com.maro.core.domain.profile.UserProfile
import com.maro.core.presentation.util.UiText

data class ProfileState(
    /** `null` while the first load is running or has failed. */
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
)
