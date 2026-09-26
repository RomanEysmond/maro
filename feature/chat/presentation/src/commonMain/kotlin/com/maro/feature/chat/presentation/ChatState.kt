package com.maro.feature.chat.presentation

import com.maro.core.presentation.util.UiText
import com.maro.feature.chat.domain.ChatHeader

/** What the top bar says about the connection; nothing while the chat is live. */
enum class ChatConnection {
    UPDATING,
    WAITING_FOR_NETWORK,
}

/** The messages themselves are paged and live next to the state, in [ChatViewModel.messages]. */
data class ChatState(
    /** `null` until the chat is in the local cache. */
    val header: ChatHeader? = null,
    val input: String = "",
    val connection: ChatConnection? = ChatConnection.UPDATING,
    /** Room has been caught up with the server at least once since the screen opened. */
    val isCaughtUp: Boolean = false,
    /** Syncing gave up (for example no access to the chat). */
    val error: UiText? = null,
) {
    val canSend: Boolean
        get() = input.isNotBlank()
}
