package com.maro.feature.chatlist.presentation

sealed interface ChatListAction {
    data class OnChatClick(val chatId: String) : ChatListAction
    data object OnRetryClick : ChatListAction
}
