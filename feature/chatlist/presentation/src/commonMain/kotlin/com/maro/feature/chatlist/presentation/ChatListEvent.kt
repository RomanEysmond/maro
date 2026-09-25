package com.maro.feature.chatlist.presentation

sealed interface ChatListEvent {
    data class NavigateToChat(val chatId: String) : ChatListEvent
}
