package com.maro.feature.chat.presentation.util

import com.maro.core.domain.user.UserSearchError
import com.maro.core.presentation.generated.resources.Res as CoreRes
import com.maro.core.presentation.generated.resources.error_no_internet
import com.maro.core.presentation.generated.resources.error_unknown
import com.maro.core.presentation.util.UiText
import com.maro.feature.chat.domain.GroupError
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.group_error_already_member
import com.maro.feature.chat.presentation.generated.resources.group_error_invalid_title
import com.maro.feature.chat.presentation.generated.resources.group_error_invalid_username
import com.maro.feature.chat.presentation.generated.resources.group_error_not_allowed
import com.maro.feature.chat.presentation.generated.resources.group_error_not_found
import com.maro.feature.chat.presentation.generated.resources.group_error_too_many

fun GroupError.toUiText(): UiText = UiText.Resource(
    when (this) {
        GroupError.INVALID_TITLE -> Res.string.group_error_invalid_title
        GroupError.NOT_ALLOWED -> Res.string.group_error_not_allowed
        GroupError.ALREADY_MEMBER -> Res.string.group_error_already_member
        GroupError.TOO_MANY_MEMBERS -> Res.string.group_error_too_many
        GroupError.NO_INTERNET -> CoreRes.string.error_no_internet
        GroupError.UNKNOWN -> CoreRes.string.error_unknown
    },
)

fun UserSearchError.toUiText(): UiText = UiText.Resource(
    when (this) {
        UserSearchError.INVALID_USERNAME -> Res.string.group_error_invalid_username
        UserSearchError.USER_NOT_FOUND -> Res.string.group_error_not_found
        UserSearchError.NO_INTERNET -> CoreRes.string.error_no_internet
        UserSearchError.UNKNOWN -> CoreRes.string.error_unknown
    },
)
