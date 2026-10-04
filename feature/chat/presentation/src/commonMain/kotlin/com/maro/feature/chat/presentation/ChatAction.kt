package com.maro.feature.chat.presentation

sealed interface ChatAction {
    data class OnInputChange(val value: String) : ChatAction
    data object OnSendClick : ChatAction
    data class OnRetryClick(val messageId: String) : ChatAction
    data object OnBackClick : ChatAction

    /** The screen came to the front (resumed) or left it. */
    data class OnVisibilityChange(val isVisible: Boolean) : ChatAction
}
