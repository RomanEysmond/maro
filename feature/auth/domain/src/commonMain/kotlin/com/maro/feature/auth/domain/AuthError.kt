package com.maro.feature.auth.domain

import com.maro.core.domain.util.Error

/** Expected failures of phone sign-in. */
enum class AuthError : Error {
    INVALID_PHONE,
    INVALID_CODE,
    CODE_EXPIRED,
    TOO_MANY_REQUESTS,
    NETWORK,
    UNKNOWN,
}
