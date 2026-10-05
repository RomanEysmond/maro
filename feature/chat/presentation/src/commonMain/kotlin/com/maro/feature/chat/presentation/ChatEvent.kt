package com.maro.feature.chat.presentation

sealed interface ChatEvent {
    data object NavigateBack : ChatEvent
    data class NavigateToGroupInfo(val chatId: String) : ChatEvent
}
