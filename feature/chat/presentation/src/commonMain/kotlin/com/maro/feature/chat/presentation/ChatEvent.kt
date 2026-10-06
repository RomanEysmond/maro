package com.maro.feature.chat.presentation

import com.maro.core.presentation.util.UiText
import com.maro.feature.chat.domain.MessageImage

sealed interface ChatEvent {
    data object NavigateBack : ChatEvent
    data class NavigateToGroupInfo(val chatId: String) : ChatEvent
    data class OpenImage(val image: MessageImage) : ChatEvent

    /** A one-off problem with what the user just did (a picture that could not be read). */
    data class ShowError(val message: UiText) : ChatEvent
}
