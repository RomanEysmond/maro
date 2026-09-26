package com.maro.feature.chat.presentation

import com.maro.core.presentation.util.UiText
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.Message

data class ChatState(
    /** `null` until the chat is in the local cache. */
    val header: ChatHeader? = null,
    /** Oldest first, straight from Room. */
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val error: UiText? = null,
) {
    val canSend: Boolean
        get() = input.isNotBlank()
}
