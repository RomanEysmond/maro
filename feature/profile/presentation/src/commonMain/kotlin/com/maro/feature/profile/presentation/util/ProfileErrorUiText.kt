package com.maro.feature.profile.presentation.util

import com.maro.core.domain.profile.ProfileError
import com.maro.core.presentation.generated.resources.error_unknown
import com.maro.core.presentation.util.UiText
import com.maro.feature.profile.presentation.generated.resources.Res
import com.maro.feature.profile.presentation.generated.resources.profile_error_no_internet
import com.maro.feature.profile.presentation.generated.resources.profile_error_username_taken
import com.maro.core.presentation.generated.resources.Res as CoreRes

fun ProfileError.toUiText(): UiText {
    val resource = when (this) {
        ProfileError.USERNAME_TAKEN -> Res.string.profile_error_username_taken
        ProfileError.NO_INTERNET -> Res.string.profile_error_no_internet
        ProfileError.UNKNOWN -> CoreRes.string.error_unknown
    }
    return UiText.Resource(resource)
}
