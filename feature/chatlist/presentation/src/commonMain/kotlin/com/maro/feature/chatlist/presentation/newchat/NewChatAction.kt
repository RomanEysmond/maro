package com.maro.feature.chatlist.presentation.newchat

sealed interface NewChatAction {
    data class OnQueryChange(val value: String) : NewChatAction
    data object OnSearchClick : NewChatAction
    data object OnUserClick : NewChatAction
    data object OnBackClick : NewChatAction
}
