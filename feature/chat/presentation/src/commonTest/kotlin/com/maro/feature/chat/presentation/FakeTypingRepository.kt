package com.maro.feature.chat.presentation

import com.maro.feature.chat.domain.TypingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTypingRepository : TypingRepository {
    val typing = MutableStateFlow<Set<String>>(emptySet())

    /** Every call, in order: (chatId, isTyping). */
    val calls = mutableListOf<Pair<String, Boolean>>()

    override fun typingUsers(chatId: String): Flow<Set<String>> = typing

    override fun setTyping(chatId: String, isTyping: Boolean) {
        calls += chatId to isTyping
    }
}
