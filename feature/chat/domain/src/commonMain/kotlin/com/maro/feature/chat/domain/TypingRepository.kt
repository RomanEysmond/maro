package com.maro.feature.chat.domain

import kotlinx.coroutines.flow.Flow

/**
 * "Typing…": ephemeral, never stored in Room. Fire-and-forget on the writing side; best effort on the reading side
 * (a stale "typing" disappears by itself after a few seconds).
 */
interface TypingRepository {
    /** Ids of the other participants typing in [chatId] right now; live while collected. */
    fun typingUsers(chatId: String): Flow<Set<String>>

    /** Called on every change of the input; throttled inside, so it is cheap to call often. */
    fun setTyping(chatId: String, isTyping: Boolean)
}
