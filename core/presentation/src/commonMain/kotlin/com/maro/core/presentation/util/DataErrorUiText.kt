package com.maro.core.presentation.util

import com.maro.core.domain.util.DataError
import com.maro.core.presentation.generated.resources.Res
import com.maro.core.presentation.generated.resources.error_disk_full
import com.maro.core.presentation.generated.resources.error_no_internet
import com.maro.core.presentation.generated.resources.error_server
import com.maro.core.presentation.generated.resources.error_unauthorized
import com.maro.core.presentation.generated.resources.error_unknown

fun DataError.toUiText(): UiText {
    val resource = when (this) {
        DataError.Network.NO_INTERNET -> Res.string.error_no_internet
        DataError.Network.SERVER_ERROR,
        DataError.Network.SERVICE_UNAVAILABLE,
        -> Res.string.error_server
        DataError.Network.UNAUTHORIZED -> Res.string.error_unauthorized
        DataError.Local.DISK_FULL -> Res.string.error_disk_full
        else -> Res.string.error_unknown
    }
    return UiText.Resource(resource)
}
