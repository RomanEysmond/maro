package com.maro.feature.chatlist.presentation.newchat

sealed interface NewChatEvent {
    data object NavigateBack : NewChatEvent
    data class NavigateToChat(val chatId: String) : NewChatEvent
}
