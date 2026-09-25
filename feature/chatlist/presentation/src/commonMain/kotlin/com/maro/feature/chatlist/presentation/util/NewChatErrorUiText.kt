package com.maro.feature.chatlist.presentation.util

import com.maro.core.presentation.generated.resources.error_unknown
import com.maro.core.presentation.util.UiText
import com.maro.feature.chatlist.domain.NewChatError
import com.maro.feature.chatlist.presentation.generated.resources.Res
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_error_invalid_username
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_error_no_internet
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_error_not_found
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_error_self
import com.maro.core.presentation.generated.resources.Res as CoreRes

fun NewChatError.toUiText(): UiText {
    val resource = when (this) {
        NewChatError.INVALID_USERNAME -> Res.string.new_chat_error_invalid_username
        NewChatError.USER_NOT_FOUND -> Res.string.new_chat_error_not_found
        NewChatError.CANNOT_CHAT_WITH_SELF -> Res.string.new_chat_error_self
        NewChatError.NO_INTERNET -> Res.string.new_chat_error_no_internet
        NewChatError.UNKNOWN -> CoreRes.string.error_unknown
    }
    return UiText.Resource(resource)
}
