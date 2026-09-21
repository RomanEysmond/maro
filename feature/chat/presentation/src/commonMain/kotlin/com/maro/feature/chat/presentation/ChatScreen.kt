package com.maro.feature.chat.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ChatRoot(chatId: String) {
    // The conversation (messages, sending, statuses) arrives in stage 4; a ViewModel is added with it.
    ChatScreen(chatId = chatId)
}

@Composable
fun ChatScreen(chatId: String) {
    Text("Chat $chatId")
}
