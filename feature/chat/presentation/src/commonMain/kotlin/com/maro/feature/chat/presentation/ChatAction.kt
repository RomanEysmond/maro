package com.maro.feature.chat.presentation

import com.maro.feature.chat.domain.MessageImage

sealed interface ChatAction {
    data class OnInputChange(val value: String) : ChatAction
    data object OnSendClick : ChatAction
    data class OnRetryClick(val messageId: String) : ChatAction

    /** The photo picker returned a picture ([source]: what the platform gave, e.g. a content URI). */
    data class OnImagePicked(val source: String) : ChatAction

    /** A photo in the conversation: open it on its own. */
    data class OnImageClick(val image: MessageImage) : ChatAction
    data object OnBackClick : ChatAction

    /** The top bar: opens the group's screen (nothing in a direct chat). */
    data object OnHeaderClick : ChatAction

    /** The screen came to the front (resumed) or left it. */
    data class OnVisibilityChange(val isVisible: Boolean) : ChatAction
}
