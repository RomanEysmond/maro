package com.maro.feature.chatlist.presentation

import com.maro.core.presentation.util.UiText
import com.maro.feature.chatlist.domain.Chat

data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    /** Shown only while [chats] is empty — a stale list is still worth showing over an error banner. */
    val error: UiText? = null,
)
