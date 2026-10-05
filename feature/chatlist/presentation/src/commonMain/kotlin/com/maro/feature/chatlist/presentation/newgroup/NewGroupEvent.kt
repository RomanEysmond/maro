package com.maro.feature.chatlist.presentation.newgroup

sealed interface NewGroupEvent {
    data object NavigateBack : NewGroupEvent
    data class NavigateToChat(val chatId: String) : NewGroupEvent
}
