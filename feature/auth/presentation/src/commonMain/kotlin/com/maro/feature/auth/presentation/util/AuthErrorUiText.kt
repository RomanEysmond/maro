package com.maro.feature.auth.presentation.util

import com.maro.core.presentation.generated.resources.error_unknown
import com.maro.core.presentation.util.UiText
import com.maro.feature.auth.domain.AuthError
import com.maro.feature.auth.presentation.generated.resources.Res
import com.maro.feature.auth.presentation.generated.resources.auth_error_code_expired
import com.maro.feature.auth.presentation.generated.resources.auth_error_invalid_code
import com.maro.feature.auth.presentation.generated.resources.auth_error_invalid_phone
import com.maro.feature.auth.presentation.generated.resources.auth_error_network
import com.maro.feature.auth.presentation.generated.resources.auth_error_too_many_requests
import com.maro.core.presentation.generated.resources.Res as CoreRes

fun AuthError.toUiText(): UiText {
    val resource = when (this) {
        AuthError.INVALID_PHONE -> Res.string.auth_error_invalid_phone
        AuthError.INVALID_CODE -> Res.string.auth_error_invalid_code
        AuthError.CODE_EXPIRED -> Res.string.auth_error_code_expired
        AuthError.TOO_MANY_REQUESTS -> Res.string.auth_error_too_many_requests
        AuthError.NETWORK -> Res.string.auth_error_network
        AuthError.UNKNOWN -> CoreRes.string.error_unknown
    }
    return UiText.Resource(resource)
}
